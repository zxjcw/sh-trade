import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { handleMockRequest } from './handlers'

const delay = (ms = 140) => new Promise((resolve) => setTimeout(resolve, ms))

export const mockAdapter: AxiosAdapter = async (config) => {
  await delay()

  const method = (config.method ?? 'get').toUpperCase()
  const url = `${config.baseURL ?? ''}${config.url ?? ''}`
  const headers = config.headers as unknown as
    | {
        get?: (name: string) => unknown
        Authorization?: unknown
        authorization?: unknown
      }
    | undefined
  const authorization =
    headers?.get?.('Authorization') ?? headers?.Authorization ?? headers?.authorization
  const token =
    typeof authorization === 'string' ? authorization.replace(/^Bearer\s+/i, '') : undefined

  let payload: unknown = config.data
  if (typeof config.data === 'string') {
    try {
      payload = config.data ? JSON.parse(config.data) : undefined
    } catch {
      payload = undefined
    }
  }

  const result = handleMockRequest({
    method,
    url,
    params: (config.params as Record<string, unknown>) ?? {},
    data: payload,
    token
  })
  // 模拟网络序列化，避免页面状态与 mock 数据库共享对象引用
  const body = JSON.parse(JSON.stringify(result)) as typeof result

  const response: AxiosResponse = {
    data: body,
    status: 200,
    statusText: 'OK',
    headers: {},
    config: config as InternalAxiosRequestConfig
  }
  return response
}
