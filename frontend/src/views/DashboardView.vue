<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, RefreshCw } from 'lucide-vue-next'
import { api } from '../api/http'

const router = useRouter()
const dashboard = ref({ metrics: [], todos: [], prohibitedActions: [] })
const loading = ref(false)
const error = ref('')
const generatedAt = ref(new Date())
const priorityTodos = computed(() => dashboard.value.todos || [])

const targetRoutes = {
  planning: '/module/planning', production: '/module/production', warehouse: '/module/warehouse',
  quality: '/module/quality', equipment: '/module/equipment', process: '/module/process',
  trace: '/module/trace', feedback: '/module/feedback', system: '/module/access',
  systemOps: '/module/access', dashboard: '/'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    dashboard.value = await api.get('/dashboard/my-summary')
    generatedAt.value = new Date()
  } catch (cause) { error.value = cause.message } finally { loading.value = false }
}

function go(target) { router.push(targetRoutes[target] || '/') }
onMounted(load)
</script>

<template>
  <main class="workspace-page dashboard-page">
    <header class="page-header"><div><span>角色工作台</span><h1>{{ dashboard.roleName || '生产运行总览' }}</h1></div><button type="button" class="icon-button" title="刷新" @click="load"><RefreshCw :size="19" /></button></header>
    <p v-if="error" class="notice error">{{ error }}</p>
    <section class="dashboard-band">
      <div><span>数据范围</span><strong>{{ dashboard.dataScope || '-' }}</strong></div>
      <div><span>更新时间</span><strong>{{ generatedAt.toLocaleTimeString('zh-CN', { hour12: false }) }}</strong></div>
      <div><span>当前待办</span><strong>{{ priorityTodos.length }} 项</strong></div>
    </section>
    <section class="metrics-grid" aria-busy="loading">
      <button v-for="metric in dashboard.metrics || []" :key="metric.code" type="button" :class="['metric-block', metric.level]" @click="go(metric.targetTab)">
        <span>{{ metric.label }}</span><strong>{{ metric.value }}<small>{{ metric.unit }}</small></strong><ArrowRight :size="18" />
      </button>
    </section>
    <section class="dashboard-columns">
      <div class="todo-list"><header><div><span>需要处理</span><h2>角色待办</h2></div><strong>{{ priorityTodos.length }}</strong></header><button v-for="todo in priorityTodos" :key="todo.code" type="button" @click="go(todo.targetTab)"><b>{{ todo.count }}</b><span><strong>{{ todo.title }}</strong><small>{{ todo.description }}</small></span><ArrowRight :size="18" /></button><p v-if="!priorityTodos.length" class="empty-message">当前没有待处理事项</p></div>
      <div class="boundary-panel"><header><span>权限边界</span><h2>当前角色不可执行</h2></header><ul><li v-for="item in dashboard.prohibitedActions || []" :key="item">{{ item }}</li></ul></div>
    </section>
  </main>
</template>
