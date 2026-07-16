import assert from 'node:assert/strict'
import { modules } from '../src/config/modules.js'

const actions = Object.values(modules).flatMap(module =>
  module.sections.flatMap(section => [...(section.actions || []), ...(section.rowActions || [])])
)

for (const action of actions) {
  for (const field of action.fields || []) {
    if (/Id$/.test(field.key)) {
      assert.ok(['lookup', 'hidden'].includes(field.type), `${action.label}.${field.key} 仍要求手填 ID`)
    }
    if (['lookup', 'multi-lookup', 'line-items'].includes(field.type)) {
      assert.ok(field.source?.endpoint, `${action.label}.${field.key} 缺少选项接口`)
      assert.ok(field.source?.valueKey, `${action.label}.${field.key} 缺少选项值字段`)
    }
  }
}

const byPath = path => actions.find(action => action.path === path)
const workOrder = byPath('/work-orders')
assert.equal(workOrder.fields.find(field => field.key === 'taskId').type, 'lookup')
assert.equal(workOrder.fields.find(field => field.key === 'processId').source.dependsOn, 'taskId')

const requisition = byPath('/requisitions')
assert.equal(requisition.fields.find(field => field.key === 'items').type, 'line-items')

const tire = byPath('/tire-labels/generate')
assert.equal(tire.fields.find(field => field.key === 'inspectionId').type, 'lookup')
assert.equal(tire.fields.find(field => field.key === 'workOrderId').type, 'hidden')

console.log(`Dynamic form option checks passed for ${actions.length} actions`)
