import { useAuthStore } from '@/store/auth.store'

/**
 * Decodes the in-memory JWT (without verifying signature — verification happens on the server)
 * and returns {@code true} when the token contains "PLATFORM_ADMIN" in realm_access.roles.
 *
 * Falls back to {@code false} when:
 * - No token is present (not logged in or httpOnly-cookie mode)
 * - Token is malformed
 * - The role is not present
 */
export function useIsPlatformAdmin(): boolean {
  const token = useAuthStore(s => s.token)
  if (!token) return false
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const roles: string[] = payload?.realm_access?.roles ?? []
    return roles.includes('PLATFORM_ADMIN')
  } catch {
    return false
  }
}
