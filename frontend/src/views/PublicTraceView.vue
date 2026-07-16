<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { BadgeCheck, FileText } from 'lucide-vue-next'
import { api } from '../api/http'
import { businessValue, fieldLabel, localizeMessage } from '../utils/display.js'

const route = useRoute()
const trace = ref(null)
const error = ref('')
const token = computed(() => String(route.query.token || ''))
onMounted(async () => {
  if (!token.value) { error.value = '追溯令牌不能为空'; return }
  try { trace.value = await api.get(`/public/tire-traces/${encodeURIComponent(token.value)}`) } catch (cause) { error.value = `产品追溯加载未完成：${localizeMessage(cause.message)}` }
})
</script>

<template>
  <main class="public-trace-page">
    <header><img src="/assets/mes-icon.svg" alt="" /><div><span>双星轮胎</span><h1>产品质量追溯</h1></div></header>
    <p v-if="error" class="notice error">{{ error }}</p>
    <section v-else-if="trace" class="trace-document">
      <div class="trace-verdict"><BadgeCheck :size="34" /><div><span>追溯状态</span><strong>{{ businessValue(trace.tireStatus ? 'tireStatus' : 'traceStatus', trace.tireStatus || trace.traceStatus || '有效') }}</strong></div></div>
      <dl><div v-for="(value, key) in trace" :key="key"><dt>{{ fieldLabel(key) }}</dt><dd>{{ businessValue(key, value) }}</dd></div></dl>
      <a :href="`/api/public/tire-traces/${encodeURIComponent(token)}/document`" target="_blank" rel="noopener"><FileText :size="18" />查看产品追溯 PDF</a>
    </section>
  </main>
</template>
