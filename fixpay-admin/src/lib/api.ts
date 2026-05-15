import axios from 'axios'
import keycloak from './keycloak'

export const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

// Attach bearer token from Keycloak on every request
api.interceptors.request.use(async config => {
  if (keycloak.authenticated && keycloak.token) {
    // Refresh token only when a refresh token exists; avoid churn during callback races.
    if (keycloak.refreshToken) {
      try { await keycloak.updateToken(30) } catch { /* silent */ }
    }

    if (keycloak.token) {
      config.headers.Authorization = `Bearer ${keycloak.token}`
    }
  }
  return config
})

// Do not force-login on every 401; callers/screens can decide the UX.
api.interceptors.response.use(
  res => res,
  err => Promise.reject(err),
)
