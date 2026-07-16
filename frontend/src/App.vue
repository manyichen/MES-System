<script setup>
import { onMounted } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppShell from './components/AppShell.vue'
import { useSessionStore } from './stores/session'

const route = useRoute()
const session = useSessionStore()

onMounted(() => session.restore())
</script>

<template>
  <RouterView v-if="route.meta.public" />
  <AppShell v-else-if="session.authenticated">
    <RouterView />
  </AppShell>
  <RouterView v-else />
</template>
