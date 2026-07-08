// [SDD-SPEC: 02-功能规范.md §3.6 + §3.7 + conventions/fe-conventions.md §3]
import http from '@/utils/http'
import type { LoginReq, LoginVO, MeVO } from '@/types/auth'

export function loginApi(req: LoginReq): Promise<LoginVO> {
  return http.post('/login', req) as unknown as Promise<LoginVO>
}

export function meApi(): Promise<MeVO> {
  return http.get('/me') as unknown as Promise<MeVO>
}
