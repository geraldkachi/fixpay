import Dexie, { type Table } from 'dexie'
import { encryptJson, decryptJson, type EncryptedBlob } from '@/lib/crypto'
import type { Transaction } from '@/types'

/** Raw row stored in IndexedDB — only id is plaintext (indexed), payload is encrypted */
interface TxRow {
  id: string
  iv: string
  ct: string
  updatedAt: number
}

class FixPayDB extends Dexie {
  transactions!: Table<TxRow, string>

  constructor() {
    super('fixpay-offline', { autoOpen: true })
    this.version(1).stores({
      // id is the primary key; updatedAt is indexed for ordering
      transactions: 'id, updatedAt',
    })
  }
}

const db = new FixPayDB()

// ── Public API ────────────────────────────────────────────────────────────────

/**
 * Persist a list of transactions to encrypted IndexedDB.
 * Each record is individually encrypted so a single failure doesn't
 * corrupt the whole table.
 */
export async function saveTransactions(txns: Transaction[]): Promise<void> {
  const rows: TxRow[] = await Promise.all(
    txns.map(async (tx) => {
      const blob: EncryptedBlob = await encryptJson(tx)
      return { id: tx.id, iv: blob.iv, ct: blob.ct, updatedAt: Date.now() }
    })
  )
  await db.transactions.bulkPut(rows)
}

/**
 * Read all cached transactions, sorted newest-first.
 * Returns an empty array if the table is empty or decryption fails.
 */
export async function loadTransactions(): Promise<Transaction[]> {
  const rows = await db.transactions.orderBy('updatedAt').reverse().toArray()
  const results: Transaction[] = []
  for (const row of rows) {
    try {
      const tx = await decryptJson<Transaction>({ iv: row.iv, ct: row.ct })
      results.push(tx)
    } catch {
      // skip corrupted rows
    }
  }
  return results
}

/**
 * Read a single cached transaction by id.
 * Returns null if not found or decryption fails.
 */
export async function loadTransaction(id: string): Promise<Transaction | null> {
  const row = await db.transactions.get(id)
  if (!row) return null
  try {
    return await decryptJson<Transaction>({ iv: row.iv, ct: row.ct })
  } catch {
    return null
  }
}

/**
 * Wipe all locally cached transactions (call on logout).
 */
export async function clearTransactions(): Promise<void> {
  await db.transactions.clear()
}
