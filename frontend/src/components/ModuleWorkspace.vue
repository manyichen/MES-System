<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Plus, RefreshCw, Search } from 'lucide-vue-next'
import { api } from '../api/http'
import { modules } from '../config/modules'
import { useSessionStore } from '../stores/session'
import ActionDialog from './ActionDialog.vue'
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
const sections = computed(() => module.value?.sections.filter(section => !section.permission || session.hasPermission(section.permission)) || [])
const section = computed(() => sections.value.find(item => item.key === activeSection.value) || sections.value[0])
const allowedActions = computed(() => (section.value?.actions || []).filter(item => session.hasPermission(item.permission)))
const allowedRowActions = computed(() => (section.value?.rowActions || []).filter(item => session.hasPermission(item.permission)))
const filteredRows = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  if (!keyword) return rows.value
  return rows.value.filter(row => JSON.stringify(row).toLowerCase().includes(keyword))
})

function normalize(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.items)) return data.items
  if (Array.isArray(data?.records)) return data.records
  return data ? [data] : []
}

async function load() {
  if (!section.value) return
  loading.value = true
  error.value = ''
  try {
    rows.value = normalize(await api.get(section.value.endpoint))
  } catch (cause) {
    error.value = cause.message
    rows.value = []
  } finally {
    loading.value = false
  }
}

function openAction(action, row = null) {
  if (!(action.fields || []).length) return execute(action, row, {})
  dialog.value = { action, row }
}

async function execute(action, row, values) {
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    const path = typeof action.path === 'function' ? action.path(row, values) : action.path
    const body = typeof action.body === 'function' ? action.body(values, row) : values
    await api[action.method || 'post'](path, body)
    notice.value = `${action.label}成功`
    dialog.value = null
    await load()
  } catch (cause) {
    error.value = cause.message
  } finally {
    busy.value = false
  }
}

watch(() => route.params.moduleKey, () => {
  activeSection.value = sections.value[0]?.key || ''
  load()
})
watch(activeSection, load)
onMounted(() => {
  activeSection.value = sections.value[0]?.key || ''
  load()
})
</script>

<template>
  <main v-if="module" class="workspace-page">
    <header class="page-header">
      <div><span>{{ module.eyebrow }}</span><h1>{{ module.title }}</h1></div>
      <button type="button" class="icon-button" title="刷新" @click="load"><RefreshCw :size="19" /></button>
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
      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice error">{{ error }}</p>
      <div v-if="loading" class="loading-line">正在加载数据...</div>
      <DataTable v-else :rows="filteredRows" :columns="section?.columns || []" :actions="allowedRowActions" @action="openAction" />
    </section>

    <ActionDialog v-if="dialog" :action="dialog.action" :row="dialog.row" :busy="busy" @close="dialog = null" @submit="values => execute(dialog.action, dialog.row, values)" />
  </main>
  <main v-else class="workspace-page"><p class="notice error">模块不存在</p></main>
</template>
