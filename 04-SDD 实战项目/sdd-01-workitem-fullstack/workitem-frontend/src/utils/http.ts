// [SDD-TASK: Task001]
// [SDD-SPEC: 02-功能规范.md §1.5 / §1.6 / §7.3 + 04-验收标准.md TC-FE-08/09 + conventions/fe-conventions.md §5]
// FE-05 + FE-08:axios 实例 + 拦截器(traceparent 注入 / Authorization 注入 / Result 解包 / 401 跳 /login)
import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import type { Result } from '@/types/workitem'
import { newTraceparent } from './traceparent'
import { showToast } from './toast'

const TOKEN_KEY = 'workitem_jwt'

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

const http: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10_000,
})

// Request:traceparent + Authorization
http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  config.headers.set('traceparent', newTraceparent())
  const token = getToken()
  if (token) config.headers.set('Authorization', `Bearer ${token}`)
  return config
})

// Response:解包 Result + 错误统一 toast
http.interceptors.response.use(
  (resp) => {
    const body: Result<unknown> = resp.data
    if (body && typeof body.code === 'number') {
      if (body.code === 0) return body.data
      showToast(body.message || `Error ${body.code}`)
      const err = new Error(body.message) as Error & { code?: number }
      err.code = body.code
      return Promise.reject(err)
    }
    return resp.data
  },
  (error) => {
    const status: number | undefined = error?.response?.status
    const body = error?.response?.data as Result<unknown> | undefined
    if (status === 401 || body?.code === 3003) {
      // [SDD-SPEC: §4 BR-15] 未登录 / token 过期 → 清 token + 跳 login
      setToken(null)
      if (location.pathname !== '/login') {
        location.assign('/login')
      }
    }
    if (body && typeof body.code === 'number') {
      showToast(body.message || `Error ${body.code}`)
      const err = new Error(body.message) as Error & { code?: number }
      err.code = body.code
      return Promise.reject(err)
    }
    showToast('Network error, please retry later')
    return Promise.reject(error)
  }
)

export default http
