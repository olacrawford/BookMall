import { clearSession } from '../utils/session'

export function unwrapResult(response) {
  const payload = response?.data
  if (payload && typeof payload === 'object' && 'code' in payload) {
    if (payload.code === 200) {
      return payload.data
    }
    const error = new Error(payload.message || '请求失败')
    error.code = payload.code
    if (payload.code === 401) {
      clearSession()
    }
    throw error
  }
  return payload
}
