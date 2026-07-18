/**
 * Pinia 会话仓库：集中管理登录用户、角色、权限和浏览器持久化。
 * localStorage 只用于刷新恢复，不视为可信授权来源；restore() 必须再调用后端 /auth/me 校验。
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '../api/http'

const STORAGE_KEY = 'mes.session'

export const useSessionStore = defineStore('session', () => {
  // 原始会话由登录接口返回，computed 将常用字段派生为适合模板判断的结构。
  const session = ref(null)
  const restored = ref(false)
  const authenticated = computed(() => Boolean(session.value?.token))
  const user = computed(() => session.value?.user || {})
  const roles = computed(() => new Set(session.value?.roles || []))
  const permissions = computed(() => new Set(session.value?.permissions || []))
  const isSuperAdmin = computed(() => roles.value.has('SUPER_ADMIN'))

  /** 同步内存和 localStorage；传 null 表示彻底退出或会话已失效。 */
  function persist(value) {
    session.value = value
    if (value) localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
    else localStorage.removeItem(STORAGE_KEY)
  }

  /** 页面刷新时恢复缓存并用 GET /api/auth/me 复核；复核失败立即清空伪造或过期缓存。 */
  async function restore() {
    if (restored.value) return
    restored.value = true
    try {
      const cached = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
      if (!cached?.token) return
      session.value = cached
      const current = await api.get('/auth/me')
      persist({ ...cached, ...current, token: cached.token })
    } catch {
      persist(null)
    }
  }

  /** 调用 POST /api/auth/login，保存令牌以及后端展开的角色、权限、数据范围。 */
  async function login(username, password) {
    const value = await api.post('/auth/login', { username, password })
    persist(value)
    return value
  }

  /** 尽力通知后端撤销会话；即使网络失败也必须清理本地令牌。 */
  async function logout() {
    try {
      if (authenticated.value) await api.post('/auth/logout')
    } finally {
      persist(null)
    }
  }

  // 这些判断决定菜单和按钮是否展示；后端仍会对每次请求独立校验，不能只信任前端。
  const hasRole = (role) => roles.value.has(role)
  const hasPermission = (permission) => isSuperAdmin.value || !permission || permissions.value.has(permission)
  const hasAnyPermission = (items = []) => !items.length || items.some(hasPermission)

  // HTTP 客户端收到任意 401 时广播该事件，使所有组件共享同一退出行为。
  window.addEventListener('mes:unauthorized', () => persist(null))

  return {
    session, restored, authenticated, user, roles, permissions, isSuperAdmin,
    restore, login, logout, hasRole, hasPermission, hasAnyPermission
  }
})
