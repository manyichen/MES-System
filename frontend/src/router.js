import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from './views/DashboardView.vue'
import ExecutiveView from './views/ExecutiveView.vue'
import LoginView from './views/LoginView.vue'
import ProfileView from './views/ProfileView.vue'
import PublicTraceView from './views/PublicTraceView.vue'
import ModuleWorkspace from './components/ModuleWorkspace.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    { path: '/', name: 'dashboard', component: DashboardView },
    { path: '/executive', name: 'executive', component: ExecutiveView },
    { path: '/executive/live', name: 'executive-live', component: ExecutiveView },
    { path: '/profile', name: 'profile', component: ProfileView },
    { path: '/module/:moduleKey', name: 'module', component: ModuleWorkspace },
    { path: '/trace-public', name: 'public-trace', component: PublicTraceView, meta: { public: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  let cached = null
  try { cached = JSON.parse(localStorage.getItem('mes.session') || 'null') } catch { /* ignored */ }
  if (!cached?.token && to.name !== 'login') return { name: 'login', query: { redirect: to.fullPath } }
  if (cached?.token && to.name === 'login') return { name: 'dashboard' }
  return true
})

export default router
