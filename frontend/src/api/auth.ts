import type { LoginResult, User } from '@/types'
import { http } from './client'

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload extends LoginPayload {
  nickname: string
  contact?: string
}

export const authApi = {
  login(payload: LoginPayload) {
    return http.post<LoginResult>('/auth/login', payload)
  },
  register(payload: RegisterPayload) {
    return http.post<LoginResult>('/auth/register', payload)
  },
  me() {
    return http.get<User>('/auth/me')
  },
  logout() {
    return http.post<null>('/auth/logout')
  }
}
