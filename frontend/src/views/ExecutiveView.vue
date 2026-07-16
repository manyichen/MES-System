<script setup>
import { onMounted, ref } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { api } from '../api/http'
import { codeLabel, localizeMessage } from '../utils/display.js'

const data = ref({ metrics: [], productionLines: [], alerts: [], departmentReports: [], auditFindings: [] })
const error = ref('')
async function load() { try { data.value = await api.get('/dashboard/executive') } catch (cause) { error.value = `经营数据加载未完成：${localizeMessage(cause.message)}` } }
onMounted(load)
</script>

<template>
  <main class="workspace-page executive-page">
    <header class="page-header"><div><span>经营管理</span><h1>公司总体运营</h1></div><button type="button" class="icon-button" title="刷新" @click="load"><RefreshCw :size="19" /></button></header>
    <p v-if="error" class="notice error">{{ error }}</p>
    <section class="executive-metrics"><div v-for="item in data.metrics" :key="item.key" :class="item.tone"><span>{{ item.label }}</span><strong>{{ item.value }}<small>{{ item.unit }}</small></strong><p>{{ item.detail }}</p></div></section>
    <section class="executive-grid">
      <div class="executive-table"><header><span>现场生产</span><h2>产线运行</h2></header><table><thead><tr><th>产线</th><th>状态</th><th>在制工单</th><th>计划/完成</th><th>故障设备</th></tr></thead><tbody><tr v-for="line in data.productionLines" :key="line.lineId"><td>{{ line.lineName }}</td><td>{{ codeLabel(line.lineStatus, 'lineStatus') }}</td><td>{{ line.activeWorkOrders }}</td><td>{{ line.plannedQty }} / {{ line.actualQty }}</td><td>{{ line.equipmentFaults }}</td></tr></tbody></table></div>
      <div class="alert-feed"><header><span>经营风险</span><h2>异常提醒</h2></header><article v-for="alert in data.alerts" :key="`${alert.domain}-${alert.title}`"><strong>{{ alert.title }}</strong><p>{{ alert.detail }}</p><span>{{ codeLabel(alert.domain, 'moduleCode') }} · {{ codeLabel(alert.severity, 'riskLevel') }}</span></article><p v-if="!data.alerts.length" class="empty-message">暂无经营风险</p></div>
    </section>
    <section class="department-band"><article v-for="report in data.departmentReports" :key="report.department"><span>{{ report.department }}</span><h2>{{ report.metricValue }}<small>{{ report.unit }}</small></h2><strong>{{ report.metricLabel }}</strong><p>{{ report.summary }}</p></article></section>
  </main>
</template>
