// [SDD-SPEC: 02-功能规范.md §3.1 ~ §3.5 + conventions/fe-conventions.md §3]
import http from '@/utils/http'
import type { WorkitemVO, WorkitemCreateReq, WorkitemUpdateReq, PageVO, WorkitemStatus } from '@/types/workitem'

export function createWorkitem(req: WorkitemCreateReq): Promise<WorkitemVO> {
  return http.post('/workitems', req) as unknown as Promise<WorkitemVO>
}

export function listWorkitemPage(params: {
  page: number
  size: number
  status?: WorkitemStatus
}): Promise<PageVO<WorkitemVO>> {
  return http.get('/workitems', { params }) as unknown as Promise<PageVO<WorkitemVO>>
}

export function getWorkitemById(id: number): Promise<WorkitemVO> {
  return http.get(`/workitems/${id}`) as unknown as Promise<WorkitemVO>
}

export function updateWorkitem(id: number, req: WorkitemUpdateReq): Promise<WorkitemVO> {
  return http.put(`/workitems/${id}`, req) as unknown as Promise<WorkitemVO>
}

export function softDeleteWorkitem(id: number): Promise<void> {
  return http.delete(`/workitems/${id}`) as unknown as Promise<void>
}
