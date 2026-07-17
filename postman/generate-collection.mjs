import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve(import.meta.dirname, '..')
const sourceRoot = path.join(root, 'backend', 'src', 'main', 'java', 'com', 'example', 'messystem')
const output = path.join(root, 'postman', 'MES-Full-API.postman_collection.json')
const traceStorage = path.join(root, 'storage', 'trace')

function walk(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const target = path.join(directory, entry.name)
    if (entry.isDirectory()) return walk(target)
    return entry.name.endsWith('Resource.java') ? [target] : []
  })
}

function normalizePath(value) {
  const normalized = `/${value}`.replace(/\/{2,}/g, '/').replace(/\/$/, '')
  return normalized || '/'
}

function parseResources() {
  const endpoints = []
  for (const file of walk(sourceRoot)) {
    const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/)
    let classPath = ''
    let methodPath = ''
    let pendingMethod = ''
    let insideClass = false
    for (const rawLine of lines) {
      const line = rawLine.trim()
      const pathMatch = line.match(/^@Path\("([^"]*)"\)/)
      if (!insideClass && pathMatch) classPath = pathMatch[1]
      if (/\bclass\s+\w+/.test(line)) {
        insideClass = true
        continue
      }
      if (!insideClass) continue
      const methodMatch = line.match(/^@(GET|POST|PUT|DELETE|PATCH)$/)
      if (methodMatch) {
        pendingMethod = methodMatch[1]
        methodPath = ''
        continue
      }
      if (pendingMethod && pathMatch) {
        methodPath = pathMatch[1]
        continue
      }
      if (pendingMethod && /^(public|protected)\s+/.test(line)) {
        endpoints.push({
          method: pendingMethod,
          route: normalizePath(`${classPath}/${methodPath}`),
          resource: path.basename(file, '.java')
        })
        pendingMethod = ''
        methodPath = ''
      }
    }
  }
  return endpoints.sort((a, b) => a.resource.localeCompare(b.resource) || a.route.localeCompare(b.route) || a.method.localeCompare(b.method))
}

function concreteRoute(route) {
  const resourceId = route.startsWith('/tire-labels/') ? '{{resourceId}}' : '1'
  return route
    .replace(/\{(?:token)\}/g, 'POSTMAN-NOT-FOUND')
    .replace(/\{(?:id)\}/g, resourceId)
    .replace(/(?<!\{)\{[^{}]+\}(?!\})/g, '1')
}

function jsonRequest(endpoint, { auth = false, namePrefix = '' } = {}) {
  const route = concreteRoute(endpoint.route)
  const binaryMediaType = endpoint.route.endsWith('/qrcode') || endpoint.route.endsWith('/label')
    ? 'image/png'
    : endpoint.route.endsWith('/document') ? 'application/pdf' : null
  const headers = [{ key: 'Accept', value: binaryMediaType ?? 'application/json' }]
  if (auth) headers.push({ key: 'Authorization', value: 'Bearer {{authToken}}', type: 'text' })
  if (['POST', 'PUT', 'PATCH'].includes(endpoint.method)) headers.push({ key: 'Content-Type', value: 'application/json' })
  return {
    name: `${namePrefix}${endpoint.method} ${endpoint.route}`,
    request: {
      method: endpoint.method,
      header: headers,
      ...(endpoint.method === 'GET' || endpoint.method === 'DELETE' ? {} : {
        body: { mode: 'raw', raw: '{}', options: { raw: { language: 'json' } } }
      }),
      url: { raw: `{{baseUrl}}/api${route}`, host: ['{{baseUrl}}'], path: ['api', ...route.slice(1).split('/')] },
      description: `${endpoint.resource} 中声明的正式接口。`
    }
  }
}

function event(lines, listen = 'test') {
  return [{ listen, script: { type: 'text/javascript', exec: lines } }]
}

const endpoints = parseResources()
const publicRoutes = new Set([
  'POST /auth/login',
  'GET /public/tire-traces/{token}',
  'GET /public/tire-traces/{token}/document'
])
const protectedEndpoints = endpoints.filter(endpoint => !publicRoutes.has(`${endpoint.method} ${endpoint.route}`))
const authenticatedReads = protectedEndpoints.filter(endpoint => endpoint.method === 'GET')

const unauthorizedItems = protectedEndpoints.map(endpoint => ({
  ...jsonRequest(endpoint),
  event: event([
    "pm.test('未登录请求返回 401', () => pm.response.to.have.status(401));",
    "pm.test('错误响应为 JSON', () => pm.expect(pm.response.headers.get('Content-Type') || '').to.include('application/json'));",
    "pm.test('响应时间小于 2 秒', () => pm.expect(pm.response.responseTime).to.be.below(2000));"
  ])
}))

const readItems = authenticatedReads.map(endpoint => ({
  ...jsonRequest(endpoint, { auth: true }),
  event: event([
    "pm.test('登录态未被拒绝', () => pm.expect(pm.response.code).not.to.eql(401));",
    "pm.test('权限策略已配置', () => pm.expect(pm.response.code).not.to.eql(403));",
    "pm.test('服务端无 5xx 异常', () => pm.expect(pm.response.code).to.be.below(500));",
    "pm.test('响应时间小于 5 秒', () => pm.expect(pm.response.responseTime).to.be.below(5000));",
    "if (pm.response.code === 200 && (pm.response.headers.get('Content-Type') || '').includes('application/json')) { pm.test('成功响应 JSON 可解析', () => pm.response.json()); }"
    ,...(endpoint.route.endsWith('/qrcode') || endpoint.route.endsWith('/label')
      ? ["pm.test('图片下载类型正确', () => pm.expect(pm.response.headers.get('Content-Type') || '').to.include('image/png'));"]
      : endpoint.route.endsWith('/document')
        ? ["pm.test('文档下载类型正确', () => pm.expect(pm.response.headers.get('Content-Type') || '').to.include('application/pdf'));"]
        : [])
  ])
}))

const tireList = readItems.find(item => item.name === 'GET /tire-labels')
if (tireList) {
  const localSerials = fs.existsSync(traceStorage)
    ? fs.readdirSync(traceStorage, { withFileTypes: true })
      .filter(entry => entry.isDirectory())
      .map(entry => entry.name)
      .filter(serial => ['qrcode.png', 'label.png', 'product-info.pdf'].every(name => fs.existsSync(path.join(traceStorage, serial, name))))
    : []
  tireList.event[0].script.exec.push(
    `if (pm.response.code === 200) { const rows = pm.response.json()?.data || []; const localSerials = ${JSON.stringify(localSerials)}; const row = rows.find(item => localSerials.includes(item.serialNo)); if (row?.tireId) { pm.environment.set('resourceId', String(row.tireId)); pm.collectionVariables.set('resourceId', String(row.tireId)); } }`
  )
}

const collection = {
  info: {
    _postman_id: '5ebca0f0-7642-46d3-9f55-32815e7c7c65',
    name: '双星轮胎 MES - 全量接口测试',
    description: `自动扫描 ${endpoints.length} 个 JAX-RS 接口。对全部 ${protectedEndpoints.length} 个受保护接口验证 401 安全边界；登录后对 ${authenticatedReads.length} 个只读接口验证权限、5xx、响应时间和 JSON。为保护现有远程数据库，POST/PUT/DELETE 业务写入只做未登录拦截测试。`,
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json'
  },
  variable: [
    { key: 'baseUrl', value: 'http://127.0.0.1:18084', type: 'string' },
    { key: 'username', value: 'superadmin', type: 'string' },
    { key: 'password', value: '123456', type: 'string' },
    { key: 'authToken', value: '', type: 'string' },
    { key: 'resourceId', value: '1', type: 'string' }
  ],
  item: [
    {
      name: '00 - 基础与认证',
      item: [
        {
          name: '数据库健康检查 - 未登录应拦截',
          request: { method: 'GET', header: [{ key: 'Accept', value: 'application/json' }], url: '{{baseUrl}}/api/db/ping' },
          event: event(["pm.test('健康接口受认证保护', () => pm.response.to.have.status(401));"])
        },
        {
          name: '系统管理员登录（最小权限校验）',
          request: {
            method: 'POST',
            header: [{ key: 'Content-Type', value: 'application/json' }, { key: 'Accept', value: 'application/json' }],
            body: { mode: 'raw', raw: '{\n  "username": "admin",\n  "password": "{{password}}"\n}', options: { raw: { language: 'json' } } },
            url: '{{baseUrl}}/api/auth/login'
          },
          event: event([
            "pm.test('系统管理员登录成功', () => pm.response.to.have.status(200));",
            "const payload = pm.response.json(); pm.environment.set('adminToken', payload.data.token);"
          ])
        },
        {
          name: '系统管理员不能读取生产工单',
          request: { method: 'GET', header: [{ key: 'Authorization', value: 'Bearer {{adminToken}}' }, { key: 'Accept', value: 'application/json' }], url: '{{baseUrl}}/api/work-orders' },
          event: event([
            "pm.test('最小权限边界生效', () => pm.response.to.have.status(403));",
            "pm.test('返回明确权限提示', () => pm.expect(pm.response.json().message).to.include('权限不足'));"
          ])
        },
        {
          name: '超级管理员登录',
          request: {
            method: 'POST',
            header: [{ key: 'Content-Type', value: 'application/json' }, { key: 'Accept', value: 'application/json' }],
            body: { mode: 'raw', raw: '{\n  "username": "{{username}}",\n  "password": "{{password}}"\n}', options: { raw: { language: 'json' } } },
            url: '{{baseUrl}}/api/auth/login'
          },
          event: event([
            "pm.test('登录成功', () => pm.response.to.have.status(200));",
            "pm.test('登录响应为成功 JSON', () => { const json = pm.response.json(); pm.expect(json.success).to.eql(true); pm.expect(json.data).to.be.an('object'); pm.expect(json.data.roles).to.include('SUPER_ADMIN'); });",
            "const payload = pm.response.json(); const token = payload?.data?.token; pm.expect(token, '登录令牌').to.be.a('string').and.not.empty; pm.environment.set('authToken', token); pm.collectionVariables.set('authToken', token);"
          ])
        },
        {
          name: '当前用户',
          request: { method: 'GET', header: [{ key: 'Authorization', value: 'Bearer {{authToken}}' }, { key: 'Accept', value: 'application/json' }], url: '{{baseUrl}}/api/auth/me' },
          event: event([
            "pm.test('当前用户查询成功', () => pm.response.to.have.status(200));",
            "pm.test('返回超级管理员身份', () => { const data = pm.response.json().data; pm.expect(data.user.username).to.eql(pm.environment.get('username')); pm.expect(data.roles).to.include('SUPER_ADMIN'); });"
          ])
        }
      ]
    },
    { name: `01 - 全接口未登录安全边界 (${protectedEndpoints.length})`, item: unauthorizedItems },
    { name: `02 - 登录后全量只读接口 (${authenticatedReads.length})`, item: readItems },
    {
      name: '03 - 公开接口与异常场景',
      item: [
        {
          name: '错误密码登录',
          request: { method: 'POST', header: [{ key: 'Content-Type', value: 'application/json' }], body: { mode: 'raw', raw: '{"username":"{{username}}","password":"POSTMAN-WRONG-PASSWORD"}', options: { raw: { language: 'json' } } }, url: '{{baseUrl}}/api/auth/login' },
          event: event([
            "pm.test('错误密码被拒绝', () => pm.expect(pm.response.code).to.be.oneOf([400, 401]));",
            "pm.test('错误信息不泄露密码', () => pm.expect(pm.response.text()).not.to.include('POSTMAN-WRONG-PASSWORD'));"
          ])
        },
        {
          name: '不存在的公开追溯码',
          request: { method: 'GET', header: [{ key: 'Accept', value: 'application/json' }], url: '{{baseUrl}}/api/public/tire-traces/POSTMAN-NOT-FOUND' },
          event: event([
            "pm.test('公开接口无需登录', () => pm.expect(pm.response.code).not.to.eql(401));",
            "pm.test('不存在追溯码返回 404', () => pm.response.to.have.status(404));"
          ])
        }
      ]
    },
    {
      name: '99 - 清理会话',
      item: [{
        name: '退出登录',
        request: { method: 'POST', header: [{ key: 'Authorization', value: 'Bearer {{authToken}}' }], url: '{{baseUrl}}/api/auth/logout' },
        event: event([
          "pm.test('退出登录成功', () => pm.response.to.have.status(200));",
          "pm.environment.unset('authToken'); pm.collectionVariables.unset('authToken');"
        ])
      }]
    }
  ]
}

fs.writeFileSync(output, `${JSON.stringify(collection, null, 2)}\n`)
console.log(`Generated ${output}`)
console.log(`Endpoints: ${endpoints.length}; protected: ${protectedEndpoints.length}; authenticated GET: ${authenticatedReads.length}`)
