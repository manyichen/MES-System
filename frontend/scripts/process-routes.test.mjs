import assert from 'node:assert/strict'
import { groupProcessRoutes, modules } from '../src/config/modules.js'

assert.deepEqual(
  modules.process.sections.map(section => section.key),
  ['products', 'boms', 'routes', 'routeSteps', 'lines', 'syncLogs'],
  '工艺与主数据页签应覆盖产品、BOM、路线总览、工序维护、产线和同步日志'
)

const grouped = groupProcessRoutes([
  { processId: 12, productId: 1, productCode: 'P-001', productName: '全钢轮胎', productModel: '12R22.5', processSeq: 2, processName: '成型', workCenter: 'BUILDING', enabled: 1 },
  { processId: 11, productId: 1, productCode: 'P-001', productName: '全钢轮胎', productModel: '12R22.5', processSeq: 1, processName: '密炼', workCenter: 'MIXING', enabled: 1 },
  { processId: 21, productId: 2, productCode: 'P-002', productName: '半钢轮胎', productModel: '205/55R16', processSeq: 1, processName: '配料', workCenter: 'MIXING', enabled: 1 },
  { processId: 22, productId: 2, productCode: 'P-002', productName: '半钢轮胎', productModel: '205/55R16', processSeq: 1, processName: '复核', workCenter: 'QUALITY', enabled: 0 }
])

assert.equal(grouped.length, 2, '同一产品的工序应合并为一条完整路线')
assert.equal(grouped[0].routeFlow, '1 密炼  →  2 成型', '工序应按顺序展示')
assert.equal(grouped[0].processCount, 2)
assert.equal(grouped[0].workCenters, 'MIXING、BUILDING')
assert.equal(grouped[0].sequenceStatus, '顺序正常')
assert.equal(grouped[0].enabled, 1)
assert.equal(grouped[1].sequenceStatus, '顺序冲突：1', '重复顺序应给出提示')
assert.equal(grouped[1].enabled, 0, '只要有一个工序未启用，整条路线应显示未启用')

console.log('process route grouping tests passed')
