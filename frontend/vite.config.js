/**
 * Vite 开发与生产构建配置。
 * 开发服务器默认使用 5173，并将同源 /api 请求代理到本地 Java 8080，避免浏览器 CORS 问题；
 * npm run build 输出 frontend/dist，随后由内嵌 Tomcat、WAR 或 Nginx 提供静态文件。
 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // Vue 插件负责解析 .vue 单文件组件中的 script/template/style。
  plugins: [vue()],
  server: {
    port: 5173,
    strictPort: false,
    // 前端代码始终请求相对 /api，开发和生产不需要切换 API Base URL。
    proxy: {
      '/api': 'http://127.0.0.1:8080'
    }
  },
  // 每次构建清空旧 dist，避免已经删除的带 hash 资源残留。
  build: {
    outDir: 'dist',
    emptyOutDir: true
  }
})
