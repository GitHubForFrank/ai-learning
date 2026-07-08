// [SDD-SPEC: 02-功能规范.md §2.1 ~ §2.3 + §1.1 Result + §3.2 PageVO]

export type WorkitemStatus = 'TODO' | 'DOING' | 'DONE'
export type WorkitemPriority = 'LOW' | 'MEDIUM' | 'HIGH'

export interface WorkitemVO {
  id: number
  title: string
  description: string | null
  status: WorkitemStatus
  priority: WorkitemPriority
  dueDate: string | null
  createdAt: string
  updatedAt: string
}

export interface WorkitemCreateReq {
  title: string
  description?: string
  priority?: WorkitemPriority
  dueDate?: string
}

export interface WorkitemUpdateReq {
  title?: string
  description?: string
  status?: WorkitemStatus
  priority?: WorkitemPriority
  dueDate?: string
}

export interface PageVO<T> {
  page: number
  size: number
  total: number
  totalPages: number
  list: T[]
}

export interface Result<T> {
  code: number
  message: string
  data: T | null
  timestamp: number
}
