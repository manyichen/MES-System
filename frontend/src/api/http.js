/**
 * 浏览器到 MES 后端的统一 HTTP 客户端。
 * 所有业务路径都加 /api 前缀；JSON 响应遵循 ApiResponse{success,message,data} 契约。
 * 登录令牌通过 Authorization: Bearer <token> 发送，401 会广播全局未授权事件清理会话。
 */
const API_BASE = '/api'

/** 保留 HTTP 状态码和原始响应，页面既能显示用户消息，也能按状态作特殊处理。 */
export class ApiError extends Error {
  constructor(message, status, payload) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.payload = payload
  }
}

/** 从 Pinia 持久化使用的同一缓存项中安全读取访问令牌。 */
function currentToken() {
  try {
    return JSON.parse(localStorage.getItem('mes.session') || 'null')?.token || ''
  } catch {
    return ''
  }
}

/** 统一处理身份令牌、响应包解析和请求失败的前端 HTTP 契约。 */
export async function request(path, options = {}) {
  // 合并调用方请求头，并为已登录请求注入 Bearer 令牌。
  const headers = new Headers(options.headers || {})
  const token = currentToken()
  if (token) headers.set('Authorization', 'Bearer ' + token)
  // 普通对象统一序列化为 JSON；FormData 保留浏览器自动生成的 multipart boundary。
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
  // 后端既有 JSON 接口，也有少量文本/文件接口，因此根据 Content-Type 选择解析方式。
  const contentType = response.headers.get('content-type') || ''
  const payload = contentType.includes('application/json')
    ? await response.json()
    : await response.text()
  // 同时识别 HTTP 失败和 HTTP 200 但 ApiResponse.success=false 的业务失败。
  if (!response.ok || payload?.success === false) {
    const message = payload?.message || payload || `请求失败 (${response.status})`
    if (response.status === 401) window.dispatchEvent(new CustomEvent('mes:unauthorized'))
    throw new ApiError(String(message), response.status, payload)
  }
  return payload?.data ?? payload
}

/** 请求二维码、标签或 PDF 等二进制内容；失败响应仍尽量解析为后端业务消息。 */
export async function requestBlob(path, options = {}) {
  const headers = new Headers(options.headers || {})
  const token = currentToken()
  if (token) headers.set('Authorization', 'Bearer ' + token)
  headers.set('Accept', options.accept || '*/*')
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  if (!response.ok) {
    const message = await response.text()
    if (response.status === 401) window.dispatchEvent(new CustomEvent('mes:unauthorized'))
    throw new ApiError(message || `请求失败 (${response.status})`, response.status, message)
  }
  return response.blob()
}

/** 页面使用的语义化便捷方法，避免各组件重复拼 method 和 body。 */
export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body }),
  put: (path, body) => request(path, { method: 'PUT', body }),
  delete: (path) => request(path, { method: 'DELETE' }),
  blob: (path, options) => requestBlob(path, options)
}
