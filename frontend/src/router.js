/**
 * Vue Router 路由表与前端登录守卫。
 * 使用 HTML5 History 模式，因此生产服务器必须把无扩展名的未知路径回退到 index.html。
 * 注意：本守卫只负责导航体验，不能替代后端接口的令牌与权限校验。
 */
import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from './views/DashboardView.vue'
import ExecutiveView from './views/ExecutiveView.vue'
import LoginView from './views/LoginView.vue'
import ProfileView from './views/ProfileView.vue'
import PublicTraceView from './views/PublicTraceView.vue'
import ModuleWorkspace from './components/ModuleWorkspace.vue'

const router = createRouter({
  // URL 无 # 号；服务器回退分别在 MesBackendApplication 和 deploy/nginx/mes.conf 中配置。
  history: createWebHistory(),
  routes: [
    // 登录、首页、管理驾驶舱、个人资料和配置驱动的通用业务工作台。
    { path: '/login', name: 'login', component: LoginView },
    { path: '/', name: 'dashboard', component: DashboardView },
    { path: '/executive', name: 'executive', component: ExecutiveView },
    { path: '/executive/live', name: 'executive-live', component: ExecutiveView },
    { path: '/profile', name: 'profile', component: ProfileView },
    { path: '/module/:moduleKey', name: 'module', component: ModuleWorkspace },
    // 轮胎二维码落到公开追溯页，meta.public 使其跳过后台登录跳转。
    { path: '/trace-public', name: 'public-trace', component: PublicTraceView, meta: { public: true } },
    // 未知前端地址回到工作台，避免产生空白页面。
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

/** 每次导航前读取缓存令牌：未登录转登录页，已登录访问登录页则回首页。 */
router.beforeEach((to) => {
  if (to.meta.public) return true
  let cached = null
  // 缓存可能被用户手工改坏；解析失败按未登录处理，不让路由守卫抛异常。
  try { cached = JSON.parse(localStorage.getItem('mes.session') || 'null') } catch { /* 按未登录处理 */ }
  if (!cached?.token && to.name !== 'login') return { name: 'login', query: { redirect: to.fullPath } }
  if (cached?.token && to.name === 'login') return { name: 'dashboard' }
  return true
})

export default router
