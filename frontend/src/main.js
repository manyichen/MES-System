/**
 * 前端启动入口。
 * 技术栈：Vue 3 负责组件渲染，Pinia 保存登录会话，Vue Router 管理单页路由。
 * Vite 开发时从本文件构建依赖图；生产构建后由 Java 内嵌 Tomcat 或 Nginx 提供静态文件。
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles.css'

// 创建唯一根应用，依次注册全局状态和路由，最后挂载到 index.html 的 #app 容器。
createApp(App).use(createPinia()).use(router).mount('#app')
