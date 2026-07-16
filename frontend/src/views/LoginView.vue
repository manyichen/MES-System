<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, LockKeyhole, UserRound } from 'lucide-vue-next'
import { useSessionStore } from '../stores/session'
import { localizeMessage } from '../utils/display.js'

const accounts = [
  ['superadmin', '超级管理员'], ['admin', '系统管理员'], ['mes_hr', '人事经理'], ['mes_general', '总经理'],
  ['mes_pmc', 'PMC 计划员'], ['mes_workshop', '车间管理员'], ['mes_operator', '生产操作工'],
  ['mes_warehouse', '仓库管理员'], ['mes_quality_mgr', '质量主管'], ['mes_inspector', '质检员'],
  ['mes_process', '工艺工程师'], ['mes_equipment_mgr', '设备管理员'], ['mes_maintainer', '设备维护员']
]
const username = ref('superadmin')
const password = ref('')
const error = ref('')
const busy = ref(false)
const session = useSessionStore()
const route = useRoute()
const router = useRouter()

async function submit() {
  busy.value = true
  error.value = ''
  try {
    await session.login(username.value, password.value)
    router.replace(String(route.query.redirect || '/'))
  } catch (cause) {
    error.value = `登录未完成：${localizeMessage(cause.message)}`
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-identity">
      <div class="login-brand"><img src="/assets/mes-icon.svg" alt="" /><span>双星轮胎制造执行系统</span></div>
      <div><span class="kicker">MES</span><h1>生产现场统一工作台</h1><p>计划、仓储、生产、质量与设备业务在同一条制造链路中协同。</p></div>
      <ol class="production-steps"><li>计划</li><li>备料</li><li>生产</li><li>质检</li><li>入库</li></ol>
    </section>
    <section class="login-panel">
      <form @submit.prevent="submit">
        <span class="kicker">用户登录</span>
        <h2>双星轮胎 MES</h2>
        <label><span>账号</span><div class="input-with-icon"><UserRound :size="18" /><select v-model="username" autocomplete="username"><option v-for="item in accounts" :key="item[0]" :value="item[0]">{{ item[1] }} · {{ item[0] }}</option></select></div></label>
        <label><span>密码</span><div class="input-with-icon"><LockKeyhole :size="18" /><input v-model="password" type="password" autocomplete="current-password" required /></div></label>
        <p v-if="error" class="login-error">{{ error }}</p>
        <button type="submit" :disabled="busy">{{ busy ? '正在登录...' : '登录' }}<ArrowRight :size="18" /></button>
      </form>
    </section>
  </main>
</template>
