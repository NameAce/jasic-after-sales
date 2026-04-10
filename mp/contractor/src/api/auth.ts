import { http, unwrap } from '@/utils/http'
import type { LoginResult } from '@/utils/permissions'

export type LoginPayload = {
  username: string
  password: string
}

export async function login(payload: LoginPayload) {
  const res = await http<LoginResult>({
    url: '/api/auth/login',
    method: 'POST',
    data: payload,
  })
  const code = String(res.code ?? '')
  if (code !== '00000' && code !== '0' && code !== '200') {
    throw new Error(String(res.msg ?? res.message ?? '登录失败'))
  }
  return {
    ...res,
    data: unwrap(res),
  }
}

export async function logout() {
  await http<void>({
    url: '/api/auth/logout',
    method: 'POST',
  })
}

