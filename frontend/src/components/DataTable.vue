<script setup>
/**
 * 通用数据表格：根据 modules.js 的 columns/actions 渲染列表，不感知具体 MES 模块。
 * 为控制桌面与移动端宽度最多展示九列；单元格值由 display.js 统一翻译状态、角色和时间。
 */
import { computed } from 'vue'
import { MoreHorizontal } from 'lucide-vue-next'
import { businessValue } from '../utils/display.js'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  actions: { type: Array, default: () => [] }
})
const emit = defineEmits(['action'])

// 限制列数保证布局稳定，详细字段仍可在针对性的页面或接口响应中查看。
const visibleColumns = computed(() => props.columns.slice(0, 9))

/** 将常见业务状态映射为绿/红/黄视觉语义。 */
function tone(value) {
  const status = String(value || '').toUpperCase()
  if (/(APPROVED|PASS|READY|COMPLETED|CLOSED|RUNNING|RECEIVED)/.test(status)) return 'good'
  if (/(REJECTED|FAIL|FAULT|SHORTAGE|URGENT|HIGH|DISABLED)/.test(status)) return 'bad'
  if (/(PENDING|CREATED|SUBMITTED|REPORTED|MEDIUM|IN_PROGRESS)/.test(status)) return 'warn'
  return ''
}

/** 动作禁用条件既支持固定布尔值，也支持基于当前行状态动态计算。 */
function isActionDisabled(action, row) {
  return typeof action.disabled === 'function' ? action.disabled(row) : Boolean(action.disabled)
}

/** 为禁用按钮提供具体原因，便于用户理解缺失的前置状态。 */
function actionTitle(action, row) {
  if (!isActionDisabled(action, row)) return action.title || action.label
  return typeof action.disabledReason === 'function'
    ? action.disabledReason(row)
    : (action.disabledReason || '当前操作暂不可用')
}
</script>

<template>
  <!-- 表格只发出 action 事件，接口执行由上层 ModuleWorkspace 统一处理。 -->
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
