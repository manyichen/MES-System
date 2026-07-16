const API_BASE = '/api'

export class ApiError extends Error {
  constructor(message, status, payload) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.payload = payload
  }
}

function currentToken() {
  try {
    return JSON.parse(localStorage.getItem('mes.session') || 'null')?.token || ''
  } catch {
    return ''
  }
}

/** 统一处理身份令牌、响应包解析和请求失败的前端 HTTP 契约。 */
export async function request(path, options = {}) {
  const headers = new Headers(options.headers || {})
  const token = currentToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (options.body !== undefined && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
    body: options.body === undefined || options.body instanceof FormData
      ? options.body
      : JSON.stringify(options.body)
  })
  const contentType = response.headers.get('content-type') || ''
  const payload = contentType.includes('application/json')
    ? await response.json()
    : await response.text()
  if (!response.ok || payload?.success === false) {
    const message = payload?.message || payload || `请求失败 (${response.status})`
    if (response.status === 401) window.dispatchEvent(new CustomEvent('mes:unauthorized'))
    throw new ApiError(String(message), response.status, payload)
  }
  return payload?.data ?? payload
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body }),
  put: (path, body) => request(path, { method: 'PUT', body }),
  delete: (path) => request(path, { method: 'DELETE' }),
  blob: (path) => request(path, { headers: { Accept: '*/*' } })
}
