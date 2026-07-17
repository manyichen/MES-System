<script setup>
import { computed } from 'vue'
import { MoreHorizontal } from 'lucide-vue-next'
import { businessValue } from '../utils/display.js'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  actions: { type: Array, default: () => [] }
})
const emit = defineEmits(['action'])

const visibleColumns = computed(() => props.columns.slice(0, 9))

function tone(value) {
  const status = String(value || '').toUpperCase()
  if (/(APPROVED|PASS|READY|COMPLETED|CLOSED|RUNNING|RECEIVED)/.test(status)) return 'good'
  if (/(REJECTED|FAIL|FAULT|SHORTAGE|URGENT|HIGH|DISABLED)/.test(status)) return 'bad'
  if (/(PENDING|CREATED|SUBMITTED|REPORTED|MEDIUM|IN_PROGRESS)/.test(status)) return 'warn'
  return ''
}

function isActionDisabled(action, row) {
  return typeof action.disabled === 'function' ? action.disabled(row) : Boolean(action.disabled)
}

function actionTitle(action, row) {
  if (!isActionDisabled(action, row)) return action.title || action.label
  return typeof action.disabledReason === 'function'
    ? action.disabledReason(row)
    : (action.disabledReason || '当前操作暂不可用')
}
</script>

<template>
  <div class="table-wrap">
    <table>
      <thead>
        <tr>
          <th v-for="column in visibleColumns" :key="column.key">{{ column.label }}</th>
          <th v-if="actions.length" class="action-column"><MoreHorizontal :size="17" /></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, index) in rows" :key="row.id ?? row[Object.keys(row)[0]] ?? index">
          <td v-for="column in visibleColumns" :key="column.key">
            <span
              :class="['cell-value', { wide: column.wide }, tone(row[column.key])]"
              :title="businessValue(column.key, row[column.key])"
            >{{ businessValue(column.key, row[column.key]) }}</span>
          </td>
          <td v-if="actions.length" class="row-actions">
            <button
              v-for="action in actions.filter(item => !item.visible || item.visible(row))"
              :key="action.label"
              type="button"
              class="table-action"
              :disabled="isActionDisabled(action, row)"
              :title="actionTitle(action, row)"
              @click="emit('action', action, row)"
            >{{ action.label }}</button>
          </td>
        </tr>
        <tr v-if="!rows.length">
          <td :colspan="visibleColumns.length + (actions.length ? 1 : 0)" class="empty-cell">暂无数据</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
