import assert from 'node:assert/strict'
import { businessValue, codeLabel, completedMessage, incompleteMessage, localizeMessage } from '../src/utils/display.js'

const cases = [
  ['workOrderStatus', 'CREATED', '已创建'],
  ['kittingStatus', 'READY', '已齐套'],
  ['settlementStatus', 'SETTLED', '已结算'],
  ['qualityStatus', 'QUALIFIED', '合格'],
  ['warehouseType', 'FINISHED', '成品仓'],
  ['faultLevel', 'URGENT', '紧急'],
  ['planCycle', 'MONTHLY', '每月'],
  ['workCenter', 'CURING', '硫化'],
  ['roleCode', 'SUPER_ADMIN', '超级管理员'],
  ['moduleCode', 'planning', '计划与工单'],
  ['resourceType', 'work_order', '制造工单'],
  ['actionCode', 'dispatch', '派工'],
  ['riskLevel', 'CRITICAL', '严重'],
  ['toRoleCode', 'QUALITY_MANAGER', '质量主管'],
  ['fromRoleCode', 'PRODUCTION_OPERATOR', '生产操作工']
]

for (const [key, value, expected] of cases) {
  assert.equal(businessValue(key, value), expected, `${key} 未正确显示为中文`)
}

assert.equal(codeLabel('APPROVED', 'inspectionStatus'), '审核通过')
assert.match(businessValue('batchNo', 'BATCH-20260716-001'), /^生产批次 /)
assert.equal(completedMessage('创建生产工单'), '操作已完成：创建生产工单')
assert.match(incompleteMessage('创建生产工单', new Error('work order is required')), /^操作未完成：创建生产工单。/)
assert.doesNotMatch(localizeMessage('work order is required'), /required|work order/i)

console.log(`Chinese display checks passed for ${cases.length} required business fields`)
