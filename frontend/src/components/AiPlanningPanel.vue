<script setup>
import { computed, onMounted, ref } from 'vue'
import { BrainCircuit, RefreshCw, Sparkles } from 'lucide-vue-next'
import { api } from '../api/http'
import { localizeMessage } from '../utils/display.js'

const emit = defineEmits(['apply'])

const tasks = ref([])
const selectedTaskId = ref('')
const horizonDays = ref(7)
const objective = ref('优先满足交期，同时降低缺料和产线冲突风险')
const loadingTasks = ref(false)
const generating = ref(false)
const error = ref('')
const result = ref(null)

const candidates = computed(() => tasks.value.filter(task => (
  task.taskStatus === 'READY' && task.kittingStatus === 'READY'
)))
const selectedTask = computed(() => candidates.value.find(task => (
  String(task.taskId) === String(selectedTaskId.value)
)))
const advice = computed(() => result.value?.advice || {})
const canApply = computed(() => {
  const lineId = Number(advice.value.recommendedLineId)
  return Boolean(selectedTask.value && Number.isFinite(lineId) && lineId > 0)
})

function normalize(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.items)) return data.items
  if (Array.isArray(data?.records)) return data.records
  return data ? [data] : []
}

function formatDateTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ')
}

async function loadCandidates() {
  loadingTasks.value = true
  error.value = ''
  try {
    tasks.value = normalize(await api.get('/production-tasks'))
    if (!candidates.value.some(task => String(task.taskId) === String(selectedTaskId.value))) {
      selectedTaskId.value = candidates.value[0]?.taskId ?? ''
      result.value = null
    }
  } catch (cause) {
    error.value = `候选任务加载未完成：${localizeMessage(cause.message)}`
    tasks.value = []
  } finally {
    loadingTasks.value = false
  }
}

async function generateAdvice() {
  if (!selectedTask.value) return
  generating.value = true
  error.value = ''
  result.value = null
  try {
    result.value = await api.post('/ai/planning/advice', {
      taskIds: [Number(selectedTask.value.taskId)],
      horizonDays: Number(horizonDays.value),
      objective: objective.value.trim()
    })
  } catch (cause) {
    error.value = `AI 排产分析未完成：${localizeMessage(cause.message)}`
  } finally {
    generating.value = false
  }
}

function applyAdvice() {
  if (!canApply.value) return
  emit('apply', {
    taskId: Number(selectedTask.value.taskId),
    lineId: Number(advice.value.recommendedLineId),
    plannedQty: selectedTask.value.planQty == null ? '' : Number(selectedTask.value.planQty)
  })
}

onMounted(loadCandidates)
</script>

<template>
  <section class="ai-planning-panel" aria-labelledby="ai-planning-title">
    <header class="ai-planning-head">
      <div class="ai-planning-title">
        <span class="ai-planning-icon"><BrainCircuit :size="22" /></span>
        <div>
          <span>AI 排产辅助决策</span>
          <h3 id="ai-planning-title">百炼大模型生产工单分析</h3>
        </div>
      </div>
      <button type="button" class="ai-planning-refresh" :disabled="loadingTasks || generating" title="刷新候选任务" @click="loadCandidates">
        <RefreshCw :size="16" />刷新任务
      </button>
    </header>

    <p class="ai-planning-description">
      综合订单交期、齐套结果、物料缺口、工艺路线和产线负荷给出建议。AI 只辅助决策，工单仍需由 PMC 人员确认后创建。
    </p>

    <form class="ai-planning-form" @submit.prevent="generateAdvice">
      <label class="ai-planning-task-field">
        <span>待分析生产任务</span>
        <select v-model="selectedTaskId" :disabled="loadingTasks || !candidates.length" required @change="result = null">
          <option value="" disabled>{{ loadingTasks ? '正在加载...' : '请选择已齐套任务' }}</option>
          <option v-for="task in candidates" :key="task.taskId" :value="task.taskId">
            {{ task.taskNo || `任务 #${task.taskId}` }} · 数量 {{ task.planQty ?? '—' }} · 目标产线 {{ task.targetLineId ?? '未指定' }}
          </option>
        </select>
      </label>
      <label>
        <span>分析周期（天）</span>
        <input v-model.number="horizonDays" type="number" min="1" max="30" required />
      </label>
      <label class="ai-planning-objective-field">
        <span>决策目标</span>
        <input v-model="objective" type="text" maxlength="200" required />
      </label>
      <button type="submit" class="ai-planning-run" :disabled="generating || loadingTasks || !selectedTask">
        <Sparkles :size="17" />{{ generating ? 'AI 正在分析...' : '生成工单决策建议' }}
      </button>
    </form>

    <p v-if="!loadingTasks && !candidates.length" class="ai-planning-empty">
      暂无可分析任务。请先在“生产任务”中完成齐套分析，只有任务状态为“已就绪”且齐套状态为“已齐套”的任务才能制定工单。
    </p>
    <p v-if="error" class="ai-planning-error">{{ error }}</p>

    <div v-if="result" class="ai-planning-result">
      <header>
        <div>
          <span>操作已完成：AI 排产分析 · 模型 {{ result.model || '—' }}</span>
          <h4>{{ advice.overallAdvice || 'AI 分析已完成' }}</h4>
        </div>
        <small>{{ formatDateTime(result.generatedAt) }}</small>
      </header>

      <div class="ai-planning-detail">
        <article>
          <span>订单与任务</span>
          <strong>{{ advice.orderAssignment || '—' }}</strong>
        </article>
        <article>
          <span>推荐产线</span>
          <strong>{{ advice.recommendedLine || '—' }}</strong>
          <small v-if="advice.recommendedLineId">产线 ID：{{ advice.recommendedLineId }}</small>
        </article>
        <article>
          <span>建议排期</span>
          <strong>{{ advice.recommendedStart || '—' }} 至 {{ advice.recommendedEnd || '—' }}</strong>
        </article>
        <article>
          <span>交期判断</span>
          <strong>{{ advice.deadlineAssessment || '—' }}</strong>
        </article>
        <article class="wide">
          <span>排产方法与依据</span>
          <strong>{{ advice.schedulingMethod || '—' }}</strong>
        </article>
      </div>

      <ul v-if="result.validationWarnings?.length" class="ai-planning-warnings">
        <li v-for="warning in result.validationWarnings" :key="warning">{{ warning }}</li>
      </ul>

      <footer>
        <p>采用建议后将预填任务、产线和计划数量；生产批次仍需人工核对。</p>
        <button type="button" :disabled="!canApply" @click="applyAdvice">采用建议制定工单</button>
      </footer>
    </div>
  </section>
</template>
