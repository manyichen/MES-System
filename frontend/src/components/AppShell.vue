<script setup>
/**
 * 登录后的应用框架：提供响应式侧栏、按权限过滤的导航、账号信息、退出和后端在线状态。
 * navigation 来自 modules.js；这里只负责通用导航交互，不包含具体业务接口。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BadgeCheck, ChartNoAxesCombined, ChevronLeft, ChevronRight, ClipboardList,
  Factory, LayoutDashboard, LogOut, Menu, MessageSquareText, Route, ScanLine,
  ShieldCheck, UserRound, Warehouse, Wrench, X
} from 'lucide-vue-next'
import { navigation } from '../config/modules'
import { api } from '../api/http'
import { useSessionStore } from '../stores/session'
import { codeLabel } from '../utils/display.js'

const iconMap = { BadgeCheck, ChartNoAxesCombined, ClipboardList, Factory, LayoutDashboard, MessageSquareText, Route, ScanLine, ShieldCheck, UserRound, Warehouse, Wrench }
const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const collapsed = ref(false)
const mobileOpen = ref(false)
const backendOnline = ref(null)
const isExecutiveContext = computed(() => route.path.startsWith('/executive'))
const currentItem = computed(() => items.value.find(item => isActive(item)) || items.value[0])
const navigationGroups = computed(() => {
  const groups = [
    { key: 'overview', label: '总览', keys: ['dashboard', 'executive'] },
    { key: 'manufacturing', label: '制造运营', keys: ['planning', 'production', 'warehouse', 'quality', 'equipment'] },
    { key: 'support', label: '协同与管理', keys: ['process', 'trace', 'feedback', 'access', 'profile'] }
  ]
  return groups
    .map(group => ({ ...group, items: items.value.filter(item => group.keys.includes(item.key)) }))
    .filter(group => group.items.length)
})

// 根据后端登录响应中的角色/权限隐藏无权入口；后端仍是最终安全边界。
const items = computed(() => navigation.filter(item => {
  if (session.isSuperAdmin) return true
  if (item.roles?.length && !item.roles.some(session.hasRole)) return false
  if (item.denyRoles?.some(session.hasRole)) return false
  return !item.permissions?.length || session.hasAnyPermission(item.permissions)
}))

/** 撤销后端会话、清理本地缓存并回到登录页。 */
async function logout() {
  await session.logout()
  router.replace('/login')
}

/** 移动端导航后关闭抽屉，再交给 Vue Router 切换页面。 */
function navigate(to) {
  mobileOpen.value = false
  router.push(to)
}

// 用 GET /api/auth/me 做轻量在线检查，同时也验证当前令牌仍可被后端识别。
onMounted(async () => {
  try {
    await api.get('/auth/me')
    backendOnline.value = true
  } catch {
    backendOnline.value = false
  }
})

/** 模块导航按完整路径判断激活态，首页使用精确匹配。 */
function isActive(item) {
  return route.path === item.to || (item.key === 'executive' && route.path.startsWith('/executive/'))
}
</script>

<template>
  <div :class="['app-shell', { collapsed, 'executive-context': isExecutiveContext }]">
    <aside :class="['sidebar', { open: mobileOpen }]">
      <header class="brand">
        <img src="/assets/mes-icon.svg" alt="" />
        <div><strong>双星轮胎 MES</strong><span>制造执行系统</span></div>
        <button type="button" class="mobile-close" title="关闭导航" @click="mobileOpen = false"><X :size="20" /></button>
      </header>
      <nav v-if="isExecutiveContext">
        <button v-for="item in items" :key="item.key" type="button" :class="{ active: isActive(item) }" :title="collapsed ? item.label : undefined" @click="navigate(item.to)">
          <component :is="iconMap[item.icon]" :size="19" /><span>{{ item.label }}</span>
        </button>
      </nav>
      <nav v-else class="grouped-nav">
        <section v-for="group in navigationGroups" :key="group.key" class="nav-group">
          <span class="nav-group-label">{{ group.label }}</span>
          <button v-for="item in group.items" :key="item.key" type="button" :class="{ active: isActive(item) }" :title="collapsed ? item.label : undefined" @click="navigate(item.to)">
            <component :is="iconMap[item.icon]" :size="19" /><span>{{ item.label }}</span>
          </button>
        </section>
      </nav>
      <footer>
        <div class="account"><span>{{ session.user.realName?.slice(0, 1) || '用' }}</span><div><strong>{{ session.user.realName || session.user.username }}</strong><small>{{ codeLabel(session.user.roleCode, 'roleCode') }}</small></div></div>
        <button type="button" class="logout" title="退出登录" @click="logout"><LogOut :size="18" /><span>退出</span></button>
      </footer>
      <button type="button" class="collapse-control" :title="collapsed ? '展开导航' : '收起导航'" @click="collapsed = !collapsed">
        <ChevronRight v-if="collapsed" :size="17" /><ChevronLeft v-else :size="17" />
      </button>
    </aside>
    <div v-if="mobileOpen" class="mobile-backdrop" @click="mobileOpen = false" />
    <section class="app-main">
      <div v-if="!isExecutiveContext" class="app-ambient" aria-hidden="true">
        <i class="ambient-orb ambient-orb-one" />
        <i class="ambient-orb ambient-orb-two" />
        <i class="ambient-grid" />
      </div>
      <header class="topbar">
        <template v-if="isExecutiveContext">
          <button type="button" class="mobile-menu" title="打开导航" @click="mobileOpen = true"><Menu :size="20" /></button>
          <div :class="['connection', { offline: backendOnline === false }]"><i />{{ backendOnline === null ? '正在检测后端服务' : backendOnline ? '后端服务已连接' : '后端服务连接异常' }}</div>
        </template>
        <div v-else class="topbar-leading">
          <button type="button" class="mobile-menu" title="打开导航" @click="mobileOpen = true"><Menu :size="20" /></button>
          <div class="page-location"><span>MES</span><ChevronRight :size="14" /><strong>{{ currentItem?.label || '工作台' }}</strong></div>
        </div>
        <div class="topbar-meta"><span>{{ new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }) }}</span><strong>双星轮胎工厂</strong></div>
        <div v-if="!isExecutiveContext" :class="['connection', { offline: backendOnline === false }]"><i />{{ backendOnline === null ? '服务检测中' : backendOnline ? '系统在线' : '连接异常' }}</div>
      </header>
      <slot />
    </section>
  </div>
</template>
