<script setup>
/**
 * 总经理驾驶舱与实时运行页，共用 GET /api/dashboard/executive 聚合接口。
 * ExecutiveDashboardService/Dao 在后端汇总产量、质量、产线、告警、部门报告和审计发现；
 * 本页完成图表坐标、进度、状态颜色和数字缓动，不在浏览器重新计算业务统计口径。
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  Activity, Boxes, Building2, CheckCircle2, ClipboardList, Factory, Gauge,
  Radio, RefreshCw, ShieldCheck, TriangleAlert, Wifi, Wrench
} from 'lucide-vue-next'
import { api } from '../api/http'
import { codeLabel, localizeMessage } from '../utils/display.js'

// 保持与 ExecutiveDashboard JSON 的六个聚合区一致，加载前使用空集合稳定布局。
const data = ref({
  metrics: [], productionTrend: [], productionLines: [], alerts: [],
  departmentReports: [], auditFindings: []
})
const route = useRoute()
const error = ref('')
const loading = ref(false)
const now = ref(new Date())
const animatedMetricValues = ref({})
const overviewAnimationKey = ref(0)
let clockTimer
let counterFrame

const iconMap = {
  'order-volume': ClipboardList,
  'qualified-output': CheckCircle2,
  'quality-rate': ShieldCheck,
  'equipment-availability': Gauge,
  'active-work-orders': Factory,
  'management-risks': TriangleAlert
}

// 高频指标从 metrics 的稳定 key 派生；显示值缺失时回退到 0。
const qualityRate = computed(() => data.value.metrics.find(item => item.key === 'quality-rate')?.value || '0.0')
const outputValue = computed(() => data.value.metrics.find(item => item.key === 'qualified-output')?.value || '0')
const riskTotal = computed(() => data.value.metrics.find(item => item.key === 'management-risks')?.value || '0')
const onlineLines = computed(() => data.value.productionLines.filter(line => !['FAULT', 'DISABLED'].includes(line.lineStatus)).length)
const liveMode = computed(() => route.name === 'executive-live')
const metricByKey = key => data.value.metrics.find(item => item.key === key)
const realtimeCards = computed(() => [
  { key: 'online', label: '在线产线', value: onlineLines.value, unit: `/ ${data.value.productionLines.length}`, detail: '当前可参与生产的产线', tone: 'cyan', icon: Wifi },
  { key: 'work-orders', label: '在制工单', value: metricByKey('active-work-orders')?.value || '0', unit: '张', detail: metricByKey('active-work-orders')?.detail || '当前执行中', tone: 'blue', icon: Factory },
  { key: 'equipment', label: '设备可用率', value: metricByKey('equipment-availability')?.value || '0.0', unit: '%', detail: metricByKey('equipment-availability')?.detail || '设备实时状态', tone: 'green', icon: Gauge },
  { key: 'risks', label: '实时风险事项', value: riskTotal.value, unit: '项', detail: '待跟踪与闭环的经营风险', tone: Number(riskTotal.value) ? 'amber' : 'green', icon: TriangleAlert }
])

const maxTrend = computed(() => Math.max(1, ...data.value.productionTrend.flatMap(item => [
  Number(item.reportedQty) || 0, Number(item.qualifiedQty) || 0, Number(item.defectQty) || 0
])))

const trendSeries = computed(() => [
  { key: 'reportedQty', label: '报工产量', color: '#28a8ff', points: chartPoints('reportedQty') },
  { key: 'qualifiedQty', label: '合格产量', color: '#31f2c3', points: chartPoints('qualifiedQty') },
  { key: 'defectQty', label: '不良数量', color: '#ff6b7c', points: chartPoints('defectQty') }
])

// 把最多六条生产线投影到工厂态势示意区的固定位置，保证布局不会随数据抖动。
const factoryNodes = computed(() => {
  const positions = [
    { left: '11%', top: '18%' }, { left: '67%', top: '14%' },
    { left: '5%', top: '62%' }, { left: '72%', top: '61%' },
    { left: '30%', top: '4%' }, { left: '40%', top: '72%' }
  ]
  return data.value.productionLines.slice(0, 6).map((line, index) => ({ ...line, ...positions[index] }))
})

/** 将每日趋势值按 SVG viewBox 转换为折线坐标，maxTrend 防止除零。 */
function chartPoints(key) {
  const list = data.value.productionTrend
  if (!list.length) return ''
  return list.map((item, index) => {
    const x = list.length === 1 ? 300 : 30 + (index * 540 / (list.length - 1))
    const y = 185 - ((Number(item[key]) || 0) / maxTrend.value * 150)
    return `${x},${y}`
  }).join(' ')
}

/** 计算产线实际数量相对计划数量的展示进度，并限制在 0..100。 */
function lineProgress(line) {
  const planned = Number(line.plannedQty) || 0
  return planned > 0 ? Math.min(100, Math.round((Number(line.actualQty) || 0) / planned * 100)) : 0
}

/** 将百分比或风险数转换为部门卡片的进度条宽度。 */
function reportProgress(report) {
  if (report.unit === '%') return Math.min(100, Math.max(0, Number(report.metricValue) || 0))
  return Math.max(8, 100 - Math.min(80, Number(report.riskCount) * 16))
}

/** 将告警严重度映射为 CSS 类，不改变后端原始等级。 */
function severityClass(value) {
  const severity = String(value || '').toUpperCase()
  if (['HIGH', 'CRITICAL', 'DANGER', 'FAULT'].includes(severity)) return 'danger'
  if (['MEDIUM', 'WARNING', 'WARN', 'REPAIRING', 'DOWN'].includes(severity)) return 'warning'
  return 'normal'
}

function lineStatusClass(status) {
  return severityClass(status === 'RUNNING' || status === 'NORMAL' || status === 'IDLE' ? 'NORMAL' : status)
}

function formatTime(value) {
  if (!value) return '--'
  return new Date(value).toLocaleString('zh-CN', { hour12: false, month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

/** 数字动画期间展示中间值，动画结束后回到接口原值。 */
function displayMetricValue(item) {
  return animatedMetricValues.value[item.key] ?? item.value
}

/** 使用 requestAnimationFrame 做纯展示数字缓动；系统启用“减少动画”时直接显示最终值。 */
function animateMetricValues(metrics) {
  window.cancelAnimationFrame(counterFrame)
  overviewAnimationKey.value += 1
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    animatedMetricValues.value = Object.fromEntries(metrics.map(item => [item.key, item.value]))
    return
  }
  const startedAt = performance.now()
  const duration = 900
  const parsed = metrics.map(item => {
    const text = String(item.value ?? '0')
    const value = Number(text.replaceAll(',', ''))
    const decimals = text.includes('.') ? text.split('.')[1].length : 0
    return { ...item, numericValue: value, decimals }
  })
  const tick = timestamp => {
    const ratio = Math.min(1, (timestamp - startedAt) / duration)
    const progress = 1 - Math.pow(1 - ratio, 3)
    animatedMetricValues.value = Object.fromEntries(parsed.map(item => [
      item.key,
      Number.isFinite(item.numericValue) ? (item.numericValue * progress).toFixed(item.decimals) : item.value
    ]))
    if (ratio < 1) counterFrame = window.requestAnimationFrame(tick)
    else animatedMetricValues.value = Object.fromEntries(metrics.map(item => [item.key, item.value]))
  }
  counterFrame = window.requestAnimationFrame(tick)
}

/** 手动拉取驾驶舱聚合快照，并在成功后启动指标入场动画。 */
async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await api.get('/dashboard/executive')
    data.value = result
    animateMetricValues(result.metrics || [])
  } catch (cause) {
    error.value = `经营数据加载未完成：${localizeMessage(cause.message)}`
  } finally {
    loading.value = false
  }
}

// 首次进入加载数据；每秒只更新时间显示，业务数据由右上角刷新按钮重新请求。
onMounted(() => {
  load()
  clockTimer = window.setInterval(() => { now.value = new Date() }, 1000)
})
// 页面离开时清除时钟和动画帧，避免更新已销毁组件。
onBeforeUnmount(() => {
  window.clearInterval(clockTimer)
  window.cancelAnimationFrame(counterFrame)
})
</script>

<template>
  <main class="executive-page">
    <div :class="['cockpit-frame', liveMode ? 'live-mode' : 'overview-mode']">
      <i class="frame-corner corner-left" /><i class="frame-corner corner-right" />
      <header class="cockpit-header">
        <div class="cockpit-brand"><Radio :size="17" /><span>MES 全域数据中心</span><small>REAL-TIME OPERATION CENTER</small></div>
        <div class="cockpit-title"><span>SUPER ADMIN CONSOLE</span><h1>超级管理员 · {{ liveMode ? '公司实时运行' : '公司经营概况' }}</h1></div>
        <div class="cockpit-clock">
          <span>{{ now.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }) }}</span>
          <strong>{{ now.toLocaleTimeString('zh-CN', { hour12: false }) }}</strong>
          <button type="button" :disabled="loading" title="刷新经营数据" @click="load"><RefreshCw :size="16" :class="{ spinning: loading }" /></button>
        </div>
      </header>

      <nav class="cockpit-page-switch" aria-label="驾驶舱页面切换">
        <RouterLink to="/executive" :class="{ active: !liveMode }"><Building2 :size="15" /><span>公司概况</span><small>OVERVIEW</small></RouterLink>
        <RouterLink to="/executive/live" :class="{ active: liveMode }"><Activity :size="15" /><span>实时运行</span><small>LIVE OPERATION</small></RouterLink>
      </nav>

      <p v-if="error" class="cockpit-notice">{{ error }}</p>

      <section v-if="!liveMode" :key="`metrics-${overviewAnimationKey}`" class="cockpit-metrics" aria-label="核心经营指标">
        <article v-for="item in data.metrics" :key="item.key" :class="['cockpit-metric', item.tone]">
          <div class="metric-icon"><component :is="iconMap[item.key] || Activity" :size="18" /></div>
          <div><span>{{ item.label }}</span><strong>{{ displayMetricValue(item) }}<small>{{ item.unit }}</small></strong><p>{{ item.detail }}</p></div>
          <i />
        </article>
        <p v-if="!data.metrics.length && !loading" class="cockpit-empty">暂无指标数据</p>
      </section>

      <section v-if="!liveMode" :key="`overview-${overviewAnimationKey}`" class="overview-grid">
        <section class="tech-panel department-panel overview-department">
            <header><div><span>DEPARTMENT HEALTH</span><h2>部门经营健康度</h2></div><Boxes :size="18" /></header>
            <div class="department-list">
              <article v-for="report in data.departmentReports" :key="report.department">
                <div class="department-row"><strong>{{ report.department }}</strong><span>{{ report.metricValue }}{{ report.unit }}</span></div>
                <div class="neon-progress"><i :style="{ width: `${reportProgress(report)}%` }" /></div>
                <p>{{ report.metricLabel }}<b :class="{ risky: report.riskCount }">风险 {{ report.riskCount }}</b></p>
              </article>
              <p v-if="!data.departmentReports.length" class="cockpit-empty">暂无部门数据</p>
            </div>
        </section>

        <div class="cockpit-column overview-center">
          <section class="tech-panel trend-panel overview-trend">
            <header><div><span>7-DAY OUTPUT TREND</span><h2>近七日产出趋势</h2></div><Activity :size="18" /></header>
            <div class="trend-legend"><span v-for="series in trendSeries" :key="series.key"><i :style="{ background: series.color }" />{{ series.label }}</span></div>
            <svg class="trend-chart" viewBox="0 0 600 220" role="img" aria-label="近七日产量折线图">
              <defs><filter id="lineGlow"><feGaussianBlur stdDeviation="2" result="blur" /><feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge></filter></defs>
              <line v-for="index in 5" :key="index" x1="30" x2="580" :y1="35 + (index - 1) * 37.5" :y2="35 + (index - 1) * 37.5" class="chart-grid" />
              <polyline v-for="series in trendSeries" :key="series.key" class="trend-line" pathLength="1" :points="series.points" fill="none" :stroke="series.color" stroke-width="3" filter="url(#lineGlow)" />
              <g v-for="(item, index) in data.productionTrend" :key="item.day">
                <text :x="data.productionTrend.length === 1 ? 300 : 30 + index * 540 / (data.productionTrend.length - 1)" y="212" text-anchor="middle">{{ item.day }}</text>
              </g>
              <text v-if="!data.productionTrend.length" x="300" y="115" text-anchor="middle" class="chart-empty">暂无趋势数据</text>
            </svg>
          </section>
          <section class="tech-panel company-brief-panel">
            <header><div><span>MANAGEMENT SUMMARY</span><h2>部门经营摘要</h2></div><Building2 :size="18" /></header>
            <div class="company-brief-list">
              <article v-for="report in data.departmentReports" :key="`brief-${report.department}`"><span>{{ report.department }}</span><strong>{{ report.summary }}</strong><small>{{ report.ownerRole }} · {{ report.period }}</small></article>
              <p v-if="!data.departmentReports.length" class="cockpit-empty">暂无经营摘要</p>
            </div>
          </section>
        </div>

        <section class="tech-panel audit-panel overview-audit">
            <header><div><span>MANAGEMENT AUDIT</span><h2>管理审计关注</h2></div><ShieldCheck :size="18" /></header>
            <div class="audit-list">
              <article v-for="finding in data.auditFindings" :key="`${finding.department}-${finding.title}`" :class="severityClass(finding.severity)">
                <div><span>{{ finding.department }}</span><b>{{ codeLabel(finding.severity, 'riskLevel') }}</b></div>
                <strong>{{ finding.title }}</strong><p>{{ finding.detail }}</p><small>{{ finding.nextStep }}</small>
              </article>
              <p v-if="!data.auditFindings.length" class="cockpit-empty">暂无审计关注项</p>
            </div>
        </section>
      </section>

      <template v-else>
        <section class="realtime-metrics" aria-label="实时运行指标">
          <article v-for="item in realtimeCards" :key="item.key" :class="['cockpit-metric', item.tone]">
            <div class="metric-icon"><component :is="item.icon" :size="18" /></div>
            <div><span>{{ item.label }}</span><strong>{{ item.value }}<small>{{ item.unit }}</small></strong><p>{{ item.detail }}</p></div><i />
          </article>
        </section>

        <section class="realtime-grid">
          <section class="tech-panel alert-panel realtime-alerts">
            <header><div><span>RISK MONITOR</span><h2>实时经营风险</h2></div><TriangleAlert :size="18" /></header>
            <div class="alert-list">
              <article v-for="alert in data.alerts" :key="`${alert.domain}-${alert.title}-${alert.occurredAt}`" :class="severityClass(alert.severity)">
                <i /><div><strong>{{ alert.title }}</strong><p>{{ alert.detail }}</p><span>{{ codeLabel(alert.domain, 'moduleCode') }} · {{ formatTime(alert.occurredAt) }}</span></div>
              </article>
              <div v-if="!data.alerts.length" class="risk-clear"><CheckCircle2 :size="25" /><span>当前无待处理经营风险</span></div>
            </div>
          </section>

          <section class="tech-panel factory-panel realtime-factory">
            <header><div><span>FACTORY LIVE MAP</span><h2>全厂实时运行态势</h2></div><span class="live-badge"><i /> LIVE</span></header>
            <div class="factory-stage">
              <div class="scan-line" /><div class="factory-grid-lines" />
              <div class="factory-core"><div class="core-orbit orbit-one" /><div class="core-orbit orbit-two" /><Factory :size="34" /><span>综合合格率</span><strong>{{ qualityRate }}<small>%</small></strong><p>合格产出 {{ outputValue }} 条</p></div>
              <article v-for="node in factoryNodes" :key="node.lineId" class="factory-node" :class="lineStatusClass(node.lineStatus)" :style="{ left: node.left, top: node.top }"><i /><div><strong>{{ node.lineName }}</strong><span>{{ codeLabel(node.lineStatus, 'lineStatus') }} · 工单 {{ node.activeWorkOrders }}</span></div></article>
              <div v-if="!factoryNodes.length" class="stage-empty">暂无产线运行数据</div>
              <div class="stage-summary"><span>在线产线 <b>{{ onlineLines }}/{{ data.productionLines.length }}</b></span><span>风险事项 <b>{{ riskTotal }}</b></span></div>
            </div>
          </section>

          <section class="tech-panel line-panel realtime-lines">
            <header><div><span>PRODUCTION LINES</span><h2>产线负荷与设备</h2></div><Gauge :size="18" /></header>
            <div class="line-list">
              <article v-for="line in data.productionLines" :key="line.lineId"><div><strong>{{ line.lineName }}</strong><span :class="lineStatusClass(line.lineStatus)">{{ codeLabel(line.lineStatus, 'lineStatus') }}</span></div><p><span>计划 {{ line.plannedQty }}</span><span>完成 {{ line.actualQty }}</span><span>设备 {{ line.equipmentTotal }}</span></p><div class="line-progress"><i :style="{ width: `${lineProgress(line)}%` }" /><b>{{ lineProgress(line) }}%</b></div><small v-if="line.equipmentFaults" class="fault-text"><Wrench :size="11" /> {{ line.equipmentFaults }} 台故障设备</small></article>
              <p v-if="!data.productionLines.length" class="cockpit-empty">暂无产线数据</p>
            </div>
          </section>
        </section>
      </template>

      <footer class="cockpit-footer">
        <span>数据生成时间 {{ formatTime(data.generatedAt) }}</span><i /><strong>双星轮胎 MES · 经营决策数据中心</strong><i /><span>数据每次刷新实时聚合</span>
      </footer>
    </div>
  </main>
</template>
