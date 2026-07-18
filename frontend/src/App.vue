<script setup>
/**
 * 根组件：决定当前路由应直接展示，还是放进登录后的 AppShell 工作台框架。
 * 公开追溯页不加载后台导航；普通页面只有恢复出有效会话后才显示应用壳。
 */
import { onMounted } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppShell from './components/AppShell.vue'
import { useSessionStore } from './stores/session'

const route = useRoute()
const session = useSessionStore()

// 页面刷新后从 localStorage 恢复令牌，并调用 GET /api/auth/me 向服务端复核会话。
onMounted(() => session.restore())
</script>

<template>
  <!-- 公共追溯页由二维码访问，不要求后台账号登录。 -->
  <RouterView v-if="route.meta.public" />
  <!-- 已认证页面统一放在侧边导航、账号区和后端在线状态组成的应用壳中。 -->
  <AppShell v-else-if="session.authenticated">
    <RouterView />
  </AppShell>
  <!-- 会话尚未恢复或已失效时仍允许 RouterView 渲染登录页。 -->
  <RouterView v-else />
</template>
