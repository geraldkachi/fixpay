/**
 * AES-256-GCM encryption using the built-in Web Crypto API.
 *
 * Key lifecycle:
 *  - First run: generate a random 256-bit AES-GCM key, export as JWK,
 *    and persist it to localStorage under "fixpay-db-key".
 *  - Subsequent runs: import the stored JWK.
 *
 * Security trade-off: the raw symmetric key lives in localStorage.
 * This protects against direct file-system inspection of the IndexedDB
 * files and cross-origin JS access. It does NOT protect against a full
 * XSS attack that can read localStorage. For stronger guarantees,
 * replace this with a PIN-derived PBKDF2 key (see comments below).
 */

const KEY_STORAGE = 'fixpay-db-key'
const ALGO = { name: 'AES-GCM', length: 256 } as const

let _keyCache: CryptoKey | null = null

async function loadOrCreateKey(): Promise<CryptoKey> {
  if (_keyCache) return _keyCache

  const stored = localStorage.getItem(KEY_STORAGE)
  if (stored) {
    try {
      const jwk = JSON.parse(stored) as JsonWebKey
      _keyCache = await crypto.subtle.importKey('jwk', jwk, ALGO, true, ['encrypt', 'decrypt'])
      return _keyCache
    } catch {
      // stored key is corrupt — generate a new one (existing data will be unreadable)
      localStorage.removeItem(KEY_STORAGE)
    }
  }

  // Generate a fresh key
  _keyCache = await crypto.subtle.generateKey(ALGO, true, ['encrypt', 'decrypt'])
  const jwk = await crypto.subtle.exportKey('jwk', _keyCache)
  localStorage.setItem(KEY_STORAGE, JSON.stringify(jwk))
  return _keyCache
}

export interface EncryptedBlob {
  iv: string  // base64
  ct: string  // base64 ciphertext
}

function bufToBase64(buf: ArrayBuffer | Uint8Array): string {
  const arr = buf instanceof Uint8Array ? buf : new Uint8Array(buf)
  return btoa(String.fromCharCode(...arr))
}

function base64ToBuf(b64: string): Uint8Array<ArrayBuffer> {
  const binary = atob(b64)
  const buf = new ArrayBuffer(binary.length)
  const arr = new Uint8Array(buf)
  for (let i = 0; i < binary.length; i++) arr[i] = binary.charCodeAt(i)
  return arr
}

export async function encryptJson(data: unknown): Promise<EncryptedBlob> {
  const key = await loadOrCreateKey()
  const iv = crypto.getRandomValues(new Uint8Array(12))
  const plaintext = new TextEncoder().encode(JSON.stringify(data))
  const ciphertext = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, plaintext)
  return { iv: bufToBase64(iv), ct: bufToBase64(ciphertext) }
}

export async function decryptJson<T>(blob: EncryptedBlob): Promise<T> {
  const key = await loadOrCreateKey()
  const iv = base64ToBuf(blob.iv)
  const ct = base64ToBuf(blob.ct)
  const plaintext = await crypto.subtle.decrypt({ name: 'AES-GCM', iv }, key, ct)
  return JSON.parse(new TextDecoder().decode(plaintext)) as T
}

/** Call on logout to wipe the encryption key (makes stored data unreadable). */
export function purgeDbKey(): void {
  localStorage.removeItem(KEY_STORAGE)
  _keyCache = null
}
