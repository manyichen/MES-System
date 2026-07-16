<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Save } from 'lucide-vue-next'
import { api } from '../api/http'
import { useSessionStore } from '../stores/session'
import { codeLabel, localizeMessage } from '../utils/display.js'

const session = useSessionStore()
const form = reactive({ realName: '', phone: '', email: '', avatarUrl: '', profileBio: '' })
const notice = ref('')
const error = ref('')
const busy = ref(false)

function assign(value) {
  Object.assign(form, {
    realName: value.realName || '', phone: value.phone || '', email: value.email || '',
    avatarUrl: value.avatarUrl || '', profileBio: value.profileBio || ''
  })
}

async function load() {
  try { assign(await api.get('/profile')) } catch (cause) { error.value = `个人资料加载未完成：${localizeMessage(cause.message)}` }
}

async function save() {
  busy.value = true; error.value = ''; notice.value = ''
  try {
    const value = await api.put('/profile', form)
    assign(value)
    if (session.session) session.session.user = { ...session.session.user, ...value }
    notice.value = '个人资料已更新'
  } catch (cause) { error.value = `个人资料保存未完成：${localizeMessage(cause.message)}` } finally { busy.value = false }
}

onMounted(load)
</script>

<template>
  <main class="workspace-page">
    <header class="page-header"><div><span>个人中心</span><h1>个人资料</h1></div></header>
    <section class="profile-layout">
      <aside class="profile-identity"><div class="profile-avatar">{{ form.realName?.slice(0, 1) || '用' }}</div><h2>{{ form.realName || session.user.username }}</h2><p>{{ codeLabel(session.user.roleCode, 'roleCode') }}</p><dl><div><dt>账号</dt><dd>{{ session.user.username }}</dd></div><div><dt>部门</dt><dd>{{ session.user.department || '-' }}</dd></div></dl></aside>
      <form class="profile-form" @submit.prevent="save">
        <label><span>姓名</span><input v-model="form.realName" required maxlength="100" /></label>
        <label><span>电话</span><input v-model="form.phone" maxlength="50" /></label>
        <label><span>邮箱</span><input v-model="form.email" type="email" maxlength="150" /></label>
        <label><span>头像地址</span><input v-model="form.avatarUrl" type="url" maxlength="1000" /></label>
        <label class="wide"><span>个人简介</span><textarea v-model="form.profileBio" rows="5" maxlength="500" /></label>
        <p v-if="notice" class="notice success">{{ notice }}</p><p v-if="error" class="notice error">{{ error }}</p>
        <footer><button type="submit" :disabled="busy"><Save :size="17" />{{ busy ? '保存中...' : '保存资料' }}</button></footer>
      </form>
    </section>
  </main>
</template>
