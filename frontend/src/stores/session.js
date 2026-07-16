import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '../api/http'

const STORAGE_KEY = 'mes.session'

export const useSessionStore = defineStore('session', () => {
  const session = ref(null)
  const restored = ref(false)
  const authenticated = computed(() => Boolean(session.value?.token))
  const user = computed(() => session.value?.user || {})
  const roles = computed(() => new Set(session.value?.roles || []))
  const permissions = computed(() => new Set(session.value?.permissions || []))
  const isSuperAdmin = computed(() => roles.value.has('SUPER_ADMIN'))

  function persist(value) {
    session.value = value
    if (value) localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
    else localStorage.removeItem(STORAGE_KEY)
  }

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

  async function login(username, password) {
    const value = await api.post('/auth/login', { username, password })
    persist(value)
    return value
  }

  async function logout() {
    try {
      if (authenticated.value) await api.post('/auth/logout')
    } finally {
      persist(null)
    }
  }

  const hasRole = (role) => roles.value.has(role)
  const hasPermission = (permission) => isSuperAdmin.value || !permission || permissions.value.has(permission)
  const hasAnyPermission = (items = []) => !items.length || items.some(hasPermission)

  window.addEventListener('mes:unauthorized', () => persist(null))

  return {
    session, restored, authenticated, user, roles, permissions, isSuperAdmin,
    restore, login, logout, hasRole, hasPermission, hasAnyPermission
  }
})
