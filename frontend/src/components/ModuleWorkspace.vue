<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Plus, RefreshCw, Search } from 'lucide-vue-next'
import { api } from '../api/http'
import { modules } from '../config/modules'
import { businessValue, completedMessage, incompleteMessage, localizeMessage } from '../utils/display.js'
import { useSessionStore } from '../stores/session'
import ActionDialog from './ActionDialog.vue'
import AiPlanningPanel from './AiPlanningPanel.vue'
import DataTable from './DataTable.vue'

const route = useRoute()
const session = useSessionStore()
const activeSection = ref('')
const rows = ref([])
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const notice = ref('')
const query = ref('')
const dialog = ref(null)

const module = computed(() => modules[route.params.moduleKey])
const allowed = item => {
  if (session.isSuperAdmin) return true
  if (item.roles?.length && !item.roles.some(session.hasRole)) return false
  if (item.denyRoles?.some(session.hasRole)) return false
  if (item.permissions?.length && !session.hasAnyPermission(item.permissions)) return false
  return !item.permission || session.hasPermission(item.permission)
}
const sections = computed(() => module.value?.sections.filter(allowed) || [])
const section = computed(() => sections.value.find(item => item.key === activeSection.value) || sections.value[0])
const allowedActions = computed(() => (section.value?.actions || []).filter(allowed))
const allowedRowActions = computed(() => (section.value?.rowActions || []).filter(allowed))
const showAiPlanning = computed(() => (
  route.params.moduleKey === 'planning'
  && section.value?.key === 'workOrders'
  && session.hasPermission('planning.work_order.create')
))
const filteredRows = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  if (!keyword) return rows.value
  return rows.value.filter(row => {
    const translated = Object.entries(row).map(([key, value]) => businessValue(key, value)).join(' ')
    return `${JSON.stringify(row)} ${translated}`.toLowerCase().includes(keyword)
  })
})

function normalize(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.items)) return data.items
  if (Array.isArray(data?.records)) return data.records
  return data ? [data] : []
}

async function load({ preserveNotice = false } = {}) {
  if (!section.value) return
  loading.value = true
  error.value = ''
  if (!preserveNotice) notice.value = ''
  try {
    const payload = await api.get(section.value.endpoint)
    const selected = section.value.dataPath
      ? section.value.dataPath.split('.').reduce((value, key) => value?.[key], payload)
      : payload
    rows.value = normalize(selected)
  } catch (cause) {
    error.value = `页面数据加载未完成：${localizeMessage(cause.message)}`
    rows.value = []
  } finally {
    loading.value = false
  }
}

function openAction(action, row = null) {
  if (!(action.fields || []).length) return execute(action, row, {})
  dialog.value = { action, row }
}

function applyAiAdvice(defaults) {
  const action = allowedActions.value.find(item => (
    item.path === '/work-orders' && (item.method || 'post') === 'post'
  ))
  if (!action) return
  dialog.value = {
    action: { ...action, defaults: { ...(action.defaults || {}), ...defaults } },
    row: null
  }
}

async function execute(action, row, values) {
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    const path = typeof action.path === 'function' ? action.path(row, values) : action.path
    const body = typeof action.body === 'function' ? action.body(values, row) : values
    await api[action.method || 'post'](path, body)
    notice.value = completedMessage(action.label)
    dialog.value = null
    await load({ preserveNotice: true })
  } catch (cause) {
    error.value = incompleteMessage(action.label, cause)
  } finally {
    busy.value = false
  }
}

watch(() => route.params.moduleKey, () => {
  activeSection.value = sections.value[0]?.key || ''
  load()
})
watch(activeSection, () => load())
onMounted(() => {
  activeSection.value = sections.value[0]?.key || ''
  load()
})
</script>

<template>
  <main v-if="module" class="workspace-page">
    <header class="page-header">
      <div><span>{{ module.eyebrow }}</span><h1>{{ module.title }}</h1></div>
      <button type="button" class="icon-button" title="刷新" @click="load()"><RefreshCw :size="19" /></button>
    </header>

    <nav class="section-tabs" aria-label="模块视图">
      <button v-for="item in sections" :key="item.key" type="button" :class="{ active: item.key === section?.key }" @click="activeSection = item.key">
        {{ item.title }}
      </button>
    </nav>

    <section class="data-section">
      <header class="data-toolbar">
        <div><h2>{{ section?.title }}</h2><span>{{ filteredRows.length }} 条记录</span></div>
        <div class="toolbar-actions">
          <label class="search-control"><Search :size="17" /><input v-model="query" type="search" placeholder="搜索当前视图" /></label>
          <button v-for="action in allowedActions" :key="action.label" type="button" @click="openAction(action)"><Plus :size="17" />{{ action.label }}</button>
        </div>
      </header>
      <AiPlanningPanel v-if="showAiPlanning" @apply="applyAiAdvice" />
      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice error">{{ error }}</p>
      <div v-if="loading" class="loading-line">正在加载数据...</div>
      <DataTable v-else :rows="filteredRows" :columns="section?.columns || []" :actions="allowedRowActions" @action="openAction" />
    </section>

    <ActionDialog v-if="dialog" :action="dialog.action" :row="dialog.row" :busy="busy" @close="dialog = null" @submit="values => execute(dialog.action, dialog.row, values)" />
  </main>
  <main v-else class="workspace-page"><p class="notice error">模块不存在</p></main>
</template>
