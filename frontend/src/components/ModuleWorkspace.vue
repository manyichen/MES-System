<script setup>
/**
 * 配置驱动的通用业务工作台。
 * 调用链：路由 /module/:moduleKey -> modules.js section -> GET 列表接口 -> DataTable；
 * 用户点击动作 -> ActionDialog -> api.post/put/delete -> 后端 Resource/Service/DAO -> 重新加载列表。
 * 本组件复用计划、生产、仓储、质量、设备、主数据和系统管理页面的共同交互。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ExternalLink, Plus, RefreshCw, Search, X } from 'lucide-vue-next'
import { api } from '../api/http'
import { modules } from '../config/modules'
import { businessValue, completedMessage, incompleteMessage, localizeMessage } from '../utils/display.js'
import { useSessionStore } from '../stores/session'
import ActionDialog from './ActionDialog.vue'
import AiPlanningPanel from './AiPlanningPanel.vue'
import DataTable from './DataTable.vue'

// 页面状态分为列表加载 loading、动作提交 busy、用户提示以及对话框/文件预览。
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
const preview = ref(null)

// 根据路由选择模块，再按会话中的角色/权限过滤页签和按钮。
const module = computed(() => modules[route.params.moduleKey])
/**
 * 判断一项配置是否可见。超级管理员跳过前端可见性限制；普通用户依次检查角色和权限点。
 * 这只是 UI 防误操作，不能当作安全边界，后端仍会独立拒绝越权请求。
 */
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
// 搜索同时匹配原始 JSON 值和翻译后的业务值，中文状态也能被检索。
const filteredRows = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  if (!keyword) return rows.value
  return rows.value.filter(row => {
    const translated = Object.entries(row).map(([key, value]) => businessValue(key, value)).join(' ')
    return `${JSON.stringify(row)} ${translated}`.toLowerCase().includes(keyword)
  })
})

/** 兼容后端返回数组、PageResult.items、records 或单对象等列表形态。 */
function normalize(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.items)) return data.items
  if (Array.isArray(data?.records)) return data.records
  return data ? [data] : []
}

/**
 * 加载当前 section 的 endpoint；dataPath 取嵌套集合，transform/enrich 完成页面专用转换。
 * preserveNotice 用于动作成功后刷新时保留“操作完成”消息。
 */
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
    const normalized = normalize(selected)
    const transformed = section.value.transformRows
      ? section.value.transformRows(normalized)
      : normalized
    rows.value = section.value.enrichRows
      ? await section.value.enrichRows(transformed, endpoint => api.get(endpoint))
      : transformed
  } catch (cause) {
    error.value = `页面数据加载未完成：${localizeMessage(cause.message)}`
    rows.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 动作入口：依次处理禁用原因、二次确认、文件预览、无表单直接执行和有表单弹窗。
 * row=null 表示工具栏新增动作；带 row 表示当前表格行的状态流转动作。
 */
function openAction(action, row = null) {
  const disabled = row && (typeof action.disabled === 'function' ? action.disabled(row) : action.disabled)
  if (disabled) {
    error.value = typeof action.disabledReason === 'function'
      ? action.disabledReason(row)
      : (action.disabledReason || '当前操作暂不可用')
    return
  }
  if (action.confirm && !window.confirm(typeof action.confirm === 'function' ? action.confirm(row) : action.confirm)) return
  if (action.preview) return openPreview(action, row)
  if (!(action.fields || []).length) return execute(action, row, {})
  dialog.value = { action, row }
}

/** 配置值既可为常量，也可为根据当前行计算的函数。 */
function resolveActionValue(value, row) {
  return typeof value === 'function' ? value(row) : value
}

/** 关闭文件预览时释放临时 Blob URL，避免长时间操作后浏览器内存累积。 */
function closePreview() {
  if (preview.value?.url) URL.revokeObjectURL(preview.value.url)
  preview.value = null
}

/** 调用二进制接口预览二维码、标签或 PDF，并在可用时给出公开追溯页入口。 */
async function openPreview(action, row) {
  busy.value = true
  error.value = ''
  notice.value = ''
  closePreview()
  try {
    const config = action.preview || {}
    const path = resolveActionValue(config.path || action.path, row)
    const blob = await api.blob(path)
    preview.value = {
      title: resolveActionValue(config.title, row) || action.label,
      subtitle: resolveActionValue(config.subtitle, row) || row?.serialNo || '',
      url: URL.createObjectURL(blob),
      mimeType: blob.type,
      openUrl: '',
      traceUrl: row?.accessToken ? `/trace-public?token=${encodeURIComponent(row.accessToken)}` : ''
    }
    preview.value.openUrl = preview.value.url
  } catch (cause) {
    error.value = incompleteMessage(action.label, cause)
  } finally {
    busy.value = false
  }
}

/** 把 AI 排产建议转换为“创建工单”表单默认值，最终保存仍须用户确认。 */
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

/**
 * 统一执行业务动作：动态计算 URL/请求体，调用配置中的 HTTP method，成功后关闭弹窗并刷新数据。
 * ApiError 由 display 工具翻译成用户可读中文，原始后端规则仍是最终判断依据。
 */
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

// 切换业务模块或页签后重置选择并加载数据；首次挂载也执行相同流程。
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
  <!-- 页面标题、页签、工具栏和数据表都来自 modules.js，不在组件中硬编码业务模块。 -->
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
    <div v-if="preview" class="dialog-mask" @mousedown.self="closePreview">
      <section class="dialog file-preview-dialog" role="dialog" aria-modal="true">
        <header>
          <div><span>文件预览</span><h2>{{ preview.title }}</h2><small v-if="preview.subtitle">{{ preview.subtitle }}</small></div>
          <button type="button" class="icon-button" title="关闭" @click="closePreview"><X :size="20" /></button>
        </header>
        <div class="file-preview-body">
          <img v-if="preview.mimeType?.startsWith('image/')" :src="preview.url" :alt="preview.title" />
          <iframe v-else :src="preview.url" :title="preview.title"></iframe>
        </div>
        <footer class="file-preview-actions">
          <a :href="preview.openUrl" target="_blank" rel="noopener"><ExternalLink :size="16" />新窗口打开</a>
          <a v-if="preview.traceUrl" :href="preview.traceUrl" target="_blank" rel="noopener"><ExternalLink :size="16" />查看公开追溯页</a>
        </footer>
      </section>
    </div>
  </main>
  <main v-else class="workspace-page"><p class="notice error">模块不存在</p></main>
</template>
