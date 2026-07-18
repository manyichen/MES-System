/**
 * 生成 docs/代码答辩导读与接口索引.md。
 *
 * 权威输入是 backend/src/main/java 中的 JAX-RS Resource 源码：脚本读取类/方法 @Path、HTTP 注解、
 * 方法名和“用例”Javadoc，按模块输出全量接口目录；固定章节补充技术栈、分层、外部依赖与脚本入口。
 * 该索引不扫描 SQL 内容，也不读取 .env 的真实密钥。运行方式：node scripts/generate-defense-code-index.mjs。
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(scriptDirectory, '..')
const javaRoot = path.join(root, 'backend', 'src', 'main', 'java', 'com', 'example', 'messystem')
const output = path.join(root, 'docs', '代码答辩导读与接口索引.md')

/** 业务模块中文名，与后端一级包和前端 modules.js 保持一致。 */
const moduleLabels = Object.freeze({
  access: '用户、权限与系统维护',
  auth: '认证、会话与个人资料',
  common: '健康检查与公共响应',
  dashboard: '角色首页、经营驾驶舱与管理反馈',
  equipment: '设备、报修、维修与保养',
  master: '用户与制造主数据',
  planning: '订单、任务、齐套、缺料、返工计划与工单',
  production: '生产报工与计件工资',
  quality: '质量检验、质量追溯与返工',
  trace: '轮胎标签、文件与公开追溯',
  warehouse: '物料、仓库、库存、领料、拣货与机器人物流'
})

/** 递归查找自有 Resource 源码，不进入 target 或测试目录。 */
function walk(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const target = path.join(directory, entry.name)
    if (entry.isDirectory()) return walk(target)
    return entry.name.endsWith('Resource.java') ? [target] : []
  })
}

/** 合并类级和方法级路径，保证目录中的接口统一以 /api 开头。 */
function apiPath(classPath, methodPath) {
  const route = `/api/${classPath}/${methodPath}`.replace(/\/{2,}/g, '/').replace(/\/$/, '')
  return route || '/api'
}

/**
 * 解析一个 Resource 文件。JAX-RS 注解位于方法签名前，生成器在遇到 public 方法时提交待处理注解；
 * Javadoc 中的“用例：”来自源码内答辩注释，缺失时回退为真实 Java 方法名，避免猜测。
 */
function parseResource(file) {
  const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/)
  const relative = path.relative(javaRoot, file).replaceAll('\\', '/')
  const firstPart = relative.split('/')[0]
  const module = moduleLabels[firstPart] ? firstPart : 'common'
  let classPath = ''
  let methodPath = ''
  let httpMethod = ''
  let useCase = ''
  let insideClass = false
  const endpoints = []

  for (const raw of lines) {
    const line = raw.trim()
    const pathMatch = line.match(/^@Path\("([^"]*)"\)/)
    const useCaseMatch = line.match(/^\*\s*用例：(.+?)(?:；|。|$)/)
    if (useCaseMatch) useCase = useCaseMatch[1]
    if (!insideClass && pathMatch) classPath = pathMatch[1]
    if (/\bclass\s+\w+/.test(line)) {
      insideClass = true
      continue
    }
    if (!insideClass) continue
    const methodMatch = line.match(/^@(GET|POST|PUT|DELETE|PATCH)$/)
    if (methodMatch) {
      httpMethod = methodMatch[1]
      methodPath = ''
      continue
    }
    if (httpMethod && pathMatch) {
      methodPath = pathMatch[1]
      continue
    }
    if (httpMethod) {
      const signature = line.match(/^public\s+[\w<>,.?\[\] ]+\s+(\w+)\s*\(/)
      if (signature) {
        endpoints.push({
          module,
          resource: path.basename(file, '.java'),
          methodName: signature[1],
          httpMethod,
          route: apiPath(classPath, methodPath),
          useCase: useCase || `执行 ${signature[1]} 用例`,
          source: `backend/src/main/java/com/example/messystem/${relative}`
        })
        methodPath = ''
        httpMethod = ''
        useCase = ''
      }
    }
  }
  return endpoints
}

/** Markdown 单元格转义，防止说明中的竖线破坏表格列。 */
function cell(value) {
  return String(value).replaceAll('|', '\\|').replaceAll('\n', ' ')
}

/** 将全量接口和固定架构说明组装为一份可直接答辩检索的 Markdown。 */
function render(endpoints) {
  const modules = [...new Set(endpoints.map(endpoint => endpoint.module))]
    .sort((left, right) => (moduleLabels[left] || left).localeCompare(moduleLabels[right] || right, 'zh-CN'))
  const lines = [
    '# 代码答辩导读与接口索引',
    '',
    '> 本文件由 `node scripts/generate-defense-code-index.mjs` 从 JAX-RS 源码生成。接口以源码注解为准；SQL 文件按任务要求不纳入注释与索引。',
    '',
    '## 一、系统从浏览器到数据库的调用链',
    '',
    '```text',
    'Vue 页面/ModuleWorkspace',
    '  -> frontend/src/api/http.js（/api + Bearer Token + ApiResponse 解包）',
    '  -> Nginx 或内嵌 Tomcat',
    '  -> AuthFilter（身份） -> AuthorizationPolicy（接口权限） -> DataScopeService（数据范围）',
    '  -> *Resource（HTTP 参数与响应）',
    '  -> *Service（校验、状态机、用例编排）',
    '  -> *Dao（PreparedStatement、事务、行映射）',
    '  -> Db/DbConfig -> PostgreSQL 或云 RDS',
    '```',
    '',
    '答辩要点：前端隐藏菜单/按钮只改善体验；真正的安全边界在后端。Controller 不写 SQL，Service 不依赖 HTTP/JDBC，DAO 不依赖 JAX-RS，这些约束由 `ArchitectureLayerTest` 固化。',
    '',
    '## 二、技术栈与外部依赖',
    '',
    '| 层次 | 技术/依赖 | 在代码中的入口 | 作用 |',
    '|---|---|---|---|',
    '| 前端 | Vue 3、Composition API | `frontend/src/main.js`、`.vue` | 组件、响应式状态与模板 |',
    '| 前端状态/路由 | Pinia、Vue Router | `stores/session.js`、`router.js` | 会话持久化、登录守卫、History 路由 |',
    '| 前端构建 | Vite | `frontend/vite.config.js` | 5173 开发服务器、`/api` 代理、生产 `dist` |',
    '| HTTP | Jakarta REST 4、Jersey 3 | `MesApplication`、`*Resource` | JAX-RS 路由、JSON 接口、过滤器 |',
    '| Web 容器 | Tomcat 10.1 / Servlet 6 | `MesBackendApplication`、`web.xml` | 内嵌启动、WAR 兼容、静态文件 |',
    '| JSON | Jackson 2.17 | `JacksonConfig` | entity/record 与 JSON、Java 时间转换 |',
    '| 数据库 | JDBC、PostgreSQL 42.7 | `DbConfig`、`Db`、`*Dao` | 云数据库连接、预编译 SQL、事务 |',
    '| 安全 | PBKDF2-HMAC-SHA256、随机 Token 摘要 | `PasswordHasher`、`TokenHasher`、`AuthFilter` | 密码加盐哈希、会话、RBAC 默认拒绝 |',
    '| 文件 | ZXing、Java2D、PDFBox | `TraceFileService` | 二维码、标签 PNG、产品 PDF、SHA-256 |',
    '| AI | 阿里云百炼 OpenAI Compatible API | `AiPlanningClient`、`AiPlanningConfig` | PMC 排产建议；只建议、不自动落库 |',
    '| 测试 | JUnit 5、Node assert、Playwright、Newman | `backend/src/test`、`frontend/scripts`、`postman` | 单元、架构、UI、接口和报告 |',
    '| 部署 | Nginx、Supervisor、Bash | `deploy/`、`scripts/run-backend.sh` | 静态资源、反代、守护、回滚与探针 |',
    '',
    '## 三、模块与代码定位',
    '',
    '| 模块 | 前端入口 | 后端目录 | 核心说明 |',
    '|---|---|---|---|',
    '| 认证与个人资料 | `/login`、`/profile`、`stores/session.js` | `auth/` | 登录、令牌、会话、当前用户、资料更新 |',
    '| 首页与驾驶舱 | `/`、`/executive` | `dashboard/` | 岗位首页、经营指标、趋势、告警、反馈、产品追溯 |',
    '| 计划与工单 | `/module/planning` | `planning/` | 客户订单、任务、齐套、缺料、返工计划、工单派发/接收、AI 建议 |',
    '| 生产执行 | `/module/production` | `production/` | 报工新增/修改/审核/驳回与计件工资 |',
    '| 仓储物流 | `/module/warehouse` | `warehouse/` | 主数据、库存、外采、领料审批/接收、拣货、机器人配送 |',
    '| 质量管理 | `/module/quality` | `quality/` | 质检创建/分配/录项/提交/判定、返工、质量追溯 |',
    '| 设备维护 | `/module/equipment` | `equipment/` | 设备台账、报修审核、维修派工/完工/验收、保养计划 |',
    '| 工艺与主数据 | `/module/process` | `master/` | 产品、BOM、工艺路线、生产线、同步日志、用户 |',
    '| 产品追溯 | `/module/trace`、`/trace-public` | `trace/` | 一胎一码、文件预览/打印、随机 token 公开追溯 |',
    '| 用户与权限 | `/module/access` | `access/`、`security/` | 角色、权限、申请审核、数据范围、会话/账号维护、审计 |',
    '',
    '## 四、启动、配置与部署代码',
    '',
    '| 场景 | 文件/命令 | 说明 |',
    '|---|---|---|',
    '| 本地前端 | `cd frontend && npm run dev` | Vite 5173，将 `/api` 代理到 8080 |',
    '| 本地后端 | `mvn -pl backend compile exec:java` | 启动 `MesBackendApplication`，默认 127.0.0.1:8080 |',
    '| 配置 | `.env.example`、`DbConfig` | 系统属性 -> 环境变量 -> `.env` -> 默认值；真实 `.env` 不提交 |',
    '| 生产前端/API | `deploy/nginx/mes.conf` | `dist` 静态文件、History 回退、`/api` 反代 |',
    '| 进程守护 | `deploy/supervisor/mes-backend.conf` | 自动启动/重启、进程组停止、日志与 JDK 21 |',
    '| 发布 | `deploy/install-release.sh` | 暂存编译、配置/存储继承、原子切换、探针、失败回滚 |',
    '| Postman | `postman/generate-collection.mjs`、`run-collection.mjs` | 扫描 Resource，Newman 执行并生成脱敏答辩报告 |',
    '| 源码注释 | `scripts/add-defense-comments.ps1` | 幂等补充 Java 分层、接口、方法和字段注释 |',
    '',
    '## 五、严格 JSON 与生成/第三方文件说明',
    '',
    '`package.json`、`package-lock.json`、Postman collection/environment、微信项目配置和测试 JSON 报告采用严格 JSON，语法不允许 `//` 或 `/* */`。为保证 npm、Postman、微信工具可正常解析，这些文件不写非法行内注释；其用途在本索引、相邻生成脚本及 README 中说明。`node_modules`、`target`、`dist`、运行日志、报告 HTML/XML、图片、Office 文档和 Maven Wrapper 属于第三方或生成产物，不按自有业务源码改写。',
    '',
    `## 六、全量 HTTP 接口（${endpoints.length} 个）`,
    ''
  ]

  for (const module of modules) {
    lines.push(`### ${moduleLabels[module] || module}`, '')
    const resources = [...new Set(endpoints.filter(endpoint => endpoint.module === module).map(endpoint => endpoint.resource))].sort()
    for (const resource of resources) {
      const rows = endpoints.filter(endpoint => endpoint.module === module && endpoint.resource === resource)
      lines.push(`#### ${resource}`, '', '| 方法 | 路径 | Java 方法 | 用例 |', '|---|---|---|---|')
      for (const endpoint of rows) {
        lines.push(`| ${endpoint.httpMethod} | \`${cell(endpoint.route)}\` | \`${endpoint.methodName}()\` | ${cell(endpoint.useCase)} |`)
      }
      lines.push('', `源码：\`${rows[0].source}\``, '')
    }
  }

  lines.push(
    '## 七、常见追问定位',
    '',
    '- “接口为什么安全？”：看 `AuthFilter`、`AuthorizationPolicy`、`AuthenticatedUser`、`DataScopeService`。',
    '- “密码和 Token 怎么保存？”：看 `PasswordHasher`、`TokenHasher`、`AuthDao`；数据库只存哈希。',
    '- “库存为什么不会扣成负数？”：看 `WarehouseService` 的业务前置校验和 `WarehouseDao` 的同连接事务/行锁。',
    '- “工单状态怎么流转？”：看 `WorkOrderResource` -> `WorkOrderService` -> `WorkOrderDao` 及操作日志。',
    '- “AI 会不会乱改生产数据？”：看 `AiPlanningClient`、`AiPlanningAdviceService` 和 `AiPlanningPanel`；只返回建议并回填人工表单。',
    '- “二维码如何追溯？”：看 `TireLabelResource`、`TireTraceService`、`TraceFileService`、`PublicTireTraceResource`。',
    '- “数据库/服务器在哪里配置？”：看 `.env.example`、`DbConfig`、`run-backend.sh`、Nginx 与 Supervisor 配置。',
    '- “分层如何保证不被破坏？”：看 `ArchitectureLayerTest` 和每个 Java 文件头的答辩定位。',
    ''
  )
  return `${lines.join('\n')}\n`
}

const endpoints = walk(javaRoot).flatMap(parseResource)
  .sort((left, right) => left.module.localeCompare(right.module) || left.resource.localeCompare(right.resource) || left.route.localeCompare(right.route) || left.httpMethod.localeCompare(right.httpMethod))

fs.mkdirSync(path.dirname(output), { recursive: true })
fs.writeFileSync(output, render(endpoints), 'utf8')
console.log(`Generated ${path.relative(root, output)} with ${endpoints.length} endpoints.`)
