import axios from 'axios'
import { mockAdapter } from '@/mock/adapter'

export const isMockMode = import.meta.env.VITE_USE_MOCK !== 'false'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 15_000,
  adapter: isMockMode ? mockAdapter : undefined
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('sgj_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 0) {
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      return body.data
    }
    return body
  },
  (error) => Promise.reject(new Error(error.response?.data?.message || error.message || '网络异常'))
)

export const http = {
  get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
    return client.get(url, { params }) as unknown as Promise<T>
  },
  post<T>(url: string, data?: unknown): Promise<T> {
    return client.post(url, data) as unknown as Promise<T>
  },
  put<T>(url: string, data?: unknown): Promise<T> {
    return client.put(url, data) as unknown as Promise<T>
  },
  delete<T>(url: string): Promise<T> {
    return client.delete(url) as unknown as Promise<T>
  }
}
