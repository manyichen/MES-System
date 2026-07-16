<script setup>
import { reactive, watch } from 'vue'
import { X } from 'lucide-vue-next'

const props = defineProps({ action: Object, row: Object, busy: Boolean })
const emit = defineEmits(['close', 'submit'])
const values = reactive({})

watch(() => props.action, (action) => {
  for (const key of Object.keys(values)) delete values[key]
  for (const field of action?.fields || []) {
    const initial = action?.defaults?.[field.key] ?? props.row?.[field.key]
    values[field.key] = initial ?? (field.type === 'json' ? JSON.stringify(field.example ?? [], null, 2) : '')
  }
}, { immediate: true })

function submit() {
  const output = {}
  for (const field of props.action?.fields || []) {
    let value = values[field.key]
    if (field.type === 'number' || field.type === 'decimal') {
      value = value === '' || value === null ? null : Number(value)
    } else if (field.type === 'json') {
      try { value = JSON.parse(value || 'null') } catch { return window.alert(`${field.label}必须是有效 JSON`) }
    }
    if (field.required && (value === '' || value === null || value === undefined)) {
      return window.alert(`请填写${field.label}`)
    }
    output[field.key] = value
  }
  emit('submit', output)
}
</script>

<template>
  <div class="dialog-mask" @mousedown.self="emit('close')">
    <section class="dialog" role="dialog" aria-modal="true">
      <header>
        <div><span>业务操作</span><h2>{{ action.label }}</h2></div>
        <button type="button" class="icon-button" title="关闭" @click="emit('close')"><X :size="20" /></button>
      </header>
      <form @submit.prevent="submit">
        <label v-for="field in action.fields || []" :key="field.key">
          <span>{{ field.label }}</span>
          <select v-if="field.type === 'select'" v-model="values[field.key]" :required="field.required">
            <option value="" disabled>请选择</option>
            <option v-for="option in field.options" :key="option" :value="option">{{ option }}</option>
          </select>
          <textarea v-else-if="field.type === 'json'" v-model="values[field.key]" rows="7" :required="field.required" />
          <input v-else v-model="values[field.key]" :type="field.type === 'decimal' ? 'number' : field.type" :step="field.type === 'decimal' ? '0.01' : undefined" :required="field.required" />
        </label>
        <footer>
          <button type="button" class="secondary" @click="emit('close')">取消</button>
          <button type="submit" :disabled="busy">{{ busy ? '处理中...' : '确认' }}</button>
        </footer>
      </form>
    </section>
  </div>
</template>
