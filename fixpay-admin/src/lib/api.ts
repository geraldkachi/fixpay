import axios from 'axios'

export const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
  withCredentials: true,
})

// Add CSRF cookie fetching logic where necessary
// Intercept responses for global error handling
api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      // Could trigger global logout event here if needed
    }
    return Promise.reject(err)
  }
)
