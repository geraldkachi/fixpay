import axios from 'axios'
import keycloak from './keycloak'
import { usePortalAuthStore } from '@/store/auth.store'

export const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use(async config => {
  const token = usePortalAuthStore.getState().accessToken
  if (keycloak.authenticated && token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  res => res,
  err => Promise.reject(err),
)
