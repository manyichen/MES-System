import assert from 'node:assert/strict'
import { enrichInventoryRows, modules } from '../src/config/modules.js'

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

const productionTask = byPath('/production-tasks')
assert.ok(productionTask)
assert.equal(productionTask.fields.some(field => field.key === 'targetLineId'), false)

const taskSection = modules.planning.sections.find(section => section.key === 'tasks')
const analyzeKitting = taskSection.rowActions.find(action => action.label === '齐套分析')
assert.equal(analyzeKitting.disabled({ kittingAnalyzable: true }), false)
assert.equal(analyzeKitting.disabled({ kittingAnalyzable: false }), true)
assert.equal(analyzeKitting.disabledReason({ kittingBlockedReason: '产品未配置启用的BOM物料' }), '产品未配置启用的BOM物料')
assert.equal(taskSection.transformRows([{ kittingAnalyzable: true }])[0].kittingReadiness, '可分析')
assert.equal(taskSection.transformRows([{ kittingAnalyzable: false, kittingBlockedReason: '缺少BOM' }])[0].kittingReadiness, '缺少BOM')
const publishShortage = taskSection.rowActions.find(action => action.label === '发布缺料预警')
assert.equal(publishShortage.visible({ kittingStatus: 'SHORTAGE', shortageAlertPublished: false }), true)
assert.equal(publishShortage.visible({ kittingStatus: 'SHORTAGE', shortageAlertPublished: true }), false)

const workOrderSection = modules.planning.sections.find(section => section.key === 'workOrders')
assert.equal(workOrderSection.rowActions.some(action => action.label === '退回'), false)

const requisition = byPath('/requisitions')
assert.equal(requisition.fields.find(field => field.key === 'items').type, 'line-items')

const inventory = modules.warehouse.sections.find(section => section.key === 'inventory')
assert.equal(inventory.title, '库存余额')
assert.ok(inventory.columns.some(column => column.key === 'materialName'))
assert.ok(inventory.columns.some(column => column.key === 'warehouseMaterialAvailableQty'))
const purchase = byPath('/inventory/external-purchase')
assert.equal(purchase.fields.find(field => field.key === 'materialId').source.assign.warehouseId, 'defaultWarehouseId')
assert.equal(purchase.fields.find(field => field.key === 'warehouseId').source.dependsOn, 'materialId')

const enrichedInventory = await enrichInventoryRows([
  { inventoryId: 1, materialId: 8, warehouseId: 40, locationId: 400, batchNo: 'B-1', availableQty: 30, qualityStatus: 'QUALIFIED' },
  { inventoryId: 2, materialId: 8, warehouseId: 40, locationId: 401, batchNo: 'B-2', availableQty: 45, qualityStatus: 'QUALIFIED' }
], async endpoint => ({
  '/materials': [{ materialId: 8, materialCode: 'MAT-008', materialName: '天然橡胶', specification: 'RSS3', unit: 'kg' }],
  '/warehouses': [{ warehouseId: 40, warehouseName: '原材料仓' }],
  '/warehouses/locations': [{ locationId: 400, locationName: 'A-01' }, { locationId: 401, locationName: 'A-02' }]
})[endpoint])
assert.equal(enrichedInventory[0].materialName, '天然橡胶')
assert.equal(enrichedInventory[0].warehouseName, '原材料仓')
assert.equal(enrichedInventory[0].locationName, 'A-01')
assert.equal(enrichedInventory[0].warehouseMaterialAvailableQty, 75)

const tire = byPath('/tire-labels/generate')
assert.equal(tire.fields.find(field => field.key === 'inspectionId').type, 'lookup')
assert.equal(tire.fields.find(field => field.key === 'workOrderId').type, 'hidden')

console.log(`Dynamic form option checks passed for ${actions.length} actions`)
