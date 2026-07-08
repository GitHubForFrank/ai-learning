// [SDD-SPEC: 02-功能规范.md §3.6 + §3.7]

export interface LoginReq {
  username: string
  password: string
}

export interface LoginVO {
  token: string
  expiresIn: number
  username: string
}

export interface MeVO {
  id: number
  username: string
  status: 'ACTIVE' | 'DISABLED'
}
