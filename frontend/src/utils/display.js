export const ROLE_LABELS = Object.freeze({
  SUPER_ADMIN: '超级管理员', SYSTEM_ADMIN: '系统管理员', SYSTEM_MAINTAINER: '系统维护员',
  HR_MANAGER: '人事经理', GENERAL_MANAGER: '总经理', PMC_PLANNER: 'PMC 计划员',
  WORKSHOP_MANAGER: '车间管理员', PRODUCTION_OPERATOR: '生产操作工',
  WAREHOUSE_ADMIN: '仓库管理员', QUALITY_MANAGER: '质量主管', QUALITY_INSPECTOR: '质检员',
  PROCESS_ENGINEER: '工艺工程师', EQUIPMENT_ADMIN: '设备管理员',
  EQUIPMENT_MAINTAINER: '设备维护员', VIEWER: '只读用户'
})

const STATUS_LABELS = Object.freeze({
  CREATED: '已创建', PENDING: '待处理', PENDING_PLAN: '待排产', PLANNED: '已纳入计划',
  READY: '已就绪', SHORTAGE: '缺料', RELEASED: '已发布', DISPATCHED: '已派工',
  RECEIVED: '已接收', RUNNING: '进行中', FINISHED: '已完成', COMPLETED: '已完成',
  CLOSED: '已关闭', CANCELLED: '已取消', SUBMITTED: '已提交审核', APPROVED: '审核通过',
  REJECTED: '已驳回', IN_PROGRESS: '处理中', ASSIGNED: '已分配', REPORTED: '已上报',
  REPAIRING: '维修中', ARRIVED: '已到达', CONFIRMED: '已确认', RESOLVED: '已解决',
  ACCEPTED: '已接收', APPLIED: '已执行', DRAFT: '草稿', FAILED: '失败', OPEN: '待处理',
  PAUSED: '已暂停', ACTIVE: '启用', DISABLED: '停用', SCHEDULED: '已计划',
  REWORK_REQUIRED: '需要返工', REWORKED: '已返工', UNASSIGNED: '未分配', UNREAD: '未读',
  VOID: '已作废', IN_STOCK: '已入库', RETIRED: '已停用', LOCKED: '已锁定',
  NOT_ANALYZED: '尚未分析', ANALYZED: '已分析', HANDLED: '已处理', SUCCESS: '成功',
  ERROR: '异常', ONLINE: '在线', OFFLINE: '离线', CHARGING: '充电中', DELIVERING: '配送中'
})

const VALUE_LABELS = Object.freeze({
  ...STATUS_LABELS, ...ROLE_LABELS,
  PASS: '合格', FAIL: '不合格', QUALIFIED: '合格', UNQUALIFIED: '不合格',
  NORMAL: '正常', QUALITY_RISK: '质量风险', REWORK: '返工', QUARANTINED: '隔离',
  SETTLED: '已结算', UNSETTLED: '未结算', PAID: '已支付', UNPAID: '未支付',
  LOW: '低', MEDIUM: '中', HIGH: '高', URGENT: '紧急', CRITICAL: '严重',
  DAILY: '每日', WEEKLY: '每周', MONTHLY: '每月', QUARTERLY: '每季度', YEARLY: '每年',
  RAW: '原材料', AUX: '辅助材料', WIP: '在制品', FINISHED_GOODS: '成品',
  MIXING: '炼胶', BUILDING: '成型', CURING: '硫化', FINISHING: '终检包装', GENERAL: '通用',
  MIXER: '密炼设备', BUILDING_MACHINE: '成型设备', CURING_PRESS: '硫化设备',
  INSPECTION: '检验设备', CONVEYOR: '输送设备', OTHER: '其他',
  FINAL: '成品检验', PROCESS: '过程检验', PRODUCTION: '生产', QUALITY: '质量',
  EQUIPMENT: '设备', MATERIAL: '物料', DELIVERY: '交付', WAREHOUSE: '仓储',
  ALL: '全部数据', DEPT: '本部门数据', SELF: '本人数据', CUSTOM: '自定义范围',
  BUSINESS: '业务角色', SYSTEM: '系统角色', AUTO: '自动', MANUAL: '人工'
})

const MODULE_LABELS = Object.freeze({
  system: '系统管理', dashboard: '综合看板', master: '主数据', planning: '计划与工单',
  production: '生产报工', warehouse: '仓储物流', quality: '质量管理', process: '工艺管理',
  equipment: '设备维护', trace: '产品追溯', feedback: '管理反馈'
})

const RESOURCE_LABELS = Object.freeze({
  health: '系统健康', role: '角色', user: '用户', business_data: '业务数据', demo_data: '演示数据',
  data_scope: '数据范围', permission_apply: '权限申请', audit_log: '审计日志', master_data: '主数据',
  plan: '生产计划', planning: '计划与工单', customer_order: '客户订单', production_task: '生产任务',
  work_order: '制造工单', rework_plan: '返工重排计划', warehouse_business: '仓储业务',
  warehouse_master: '仓储主数据', warehouse: '仓库', requisition: '领料申请', inventory: '库存',
  picking_task: '拣货任务', delivery_task: '配送任务', work_report: '生产报工',
  piecework_wage: '计件工资', quality_business: '质量业务', quality_inspection: '质量检验',
  inspection: '质量检验', rework_order: '返工单', process_standard: '工艺标准',
  equipment_business: '设备业务', equipment: '设备台账', repair_report: '设备报修',
  maintenance_order: '维修工单', product_trace: '产品追溯', trace: '追溯记录',
  tire_instance: '轮胎实例', tire_label: '轮胎标签', management_feedback: '管理反馈',
  dashboard: '综合看板'
})

const ACTION_LABELS = Object.freeze({
  read: '查看', read_own: '查看本人数据', read_self: '查看本人数据', read_summary: '查看汇总',
  read_all: '查看全部', create: '创建', manage: '管理', delete: '删除', release: '发布',
  dispatch: '派工', receive: '接收', approve: '审核通过', reject: '驳回', execute: '执行',
  adjust: '调整', update: '更新', update_own: '修改本人数据', update_role: '修改角色',
  review: '审核', assign: '分配', accept: '验收', close: '关闭', generate: '生成', print: '打印'
})

const FIELD_LABELS = Object.freeze({
  tireId: '轮胎 ID', serialNo: '轮胎序列号', traceCode: '追溯码', workOrderId: '制造工单 ID',
  workOrderNo: '制造工单号', inspectionId: '质检单 ID', inspectionNo: '质检单号',
  workReportId: '报工单 ID', productId: '产品 ID', productCode: '产品编码', productName: '产品名称',
  productModel: '产品型号', productionLine: '生产产线', warehouseId: '仓库 ID',
  warehouseName: '仓库名称', locationId: '库位 ID', locationName: '库位名称', batchNo: '生产批次',
  tireStatus: '轮胎状态', traceStatus: '追溯状态', qualifiedAt: '质检合格时间', inboundAt: '入库时间',
  printCount: '打印次数', lastPrintedAt: '最近打印时间', createdAt: '创建时间', publicToken: '公开追溯令牌'
})

const CONTEXT_LABELS = Object.freeze({
  warehouseType: { RAW: '原材料仓', WIP: '在制品仓', FINISHED: '成品仓' },
  materialType: { RAW: '原材料', AUX: '辅助材料', WIP: '在制品', FINISHED: '成品' },
  kittingStatus: { PENDING: '待齐套分析', READY: '已齐套', SHORTAGE: '缺料' },
  qualityStatus: { PENDING: '待检验', QUALIFIED: '合格', UNQUALIFIED: '不合格', QUARANTINED: '隔离' },
  settlementStatus: { PENDING: '待结算', SETTLED: '已结算', UNSETTLED: '未结算', PAID: '已支付', UNPAID: '未支付' },
  faultLevel: { LOW: '低级', MEDIUM: '中级', HIGH: '高级', URGENT: '紧急', GENERAL: '一般' }
})

export function codeLabel(value, key = '') {
  if (value === null || value === undefined || value === '') return '—'
  const text = String(value)
  const code = text.toUpperCase()
  if (key === 'moduleCode') return MODULE_LABELS[text.toLowerCase()] || text
  if (key === 'resourceType') return RESOURCE_LABELS[text.toLowerCase()] || text
  if (key === 'actionCode') return ACTION_LABELS[text.toLowerCase()] || text
  if (['roleCode', 'fromRoleCode', 'toRoleCode'].includes(key)) return ROLE_LABELS[code] || text
  return CONTEXT_LABELS[key]?.[code] || VALUE_LABELS[code] || text
}

export function localizeText(value) {
  if (value === null || value === undefined) return ''
  return String(value).replace(/[A-Za-z][A-Za-z0-9_]*/g, token => {
    const code = token.toUpperCase()
    return VALUE_LABELS[code] || MODULE_LABELS[token.toLowerCase()]
      || RESOURCE_LABELS[token.toLowerCase()] || ACTION_LABELS[token.toLowerCase()] || token
  })
}

export function businessValue(key, value) {
  if (value === null || value === undefined || value === '') return '—'
  if (Array.isArray(value)) return value.map(item => codeLabel(item, key)).join('、') || '—'
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (key === 'enabled') return Number(value) === 1 ? '已启用' : '已停用'
  if (key === 'batchNo') return `生产批次 ${value}`
  if (key === 'roleLevel') return `第 ${value} 级`
  if (key === 'priorityLevel') return `第 ${value} 优先级`
  if (typeof value === 'object') return JSON.stringify(value)
  return codeLabel(value, key)
}

export function fieldLabel(key) {
  return FIELD_LABELS[key] || key
}

const MESSAGE_REPLACEMENTS = [
  [/task productId is required/i, '生产任务未关联产品，请先补全客户订单的产品信息'],
  [/product bom is required before kitting analysis/i, '产品未配置启用的BOM物料，请先维护产品BOM'],
  [/not found/i, '未找到相关数据'], [/is required/i, '必填信息不能为空'],
  [/must pass kitting analysis before creating work order/i, '必须先完成齐套分析才能创建生产工单'],
  [/must be an available production line/i, '请选择当前可用的生产产线'],
  [/must match the production task product/i, '所选工序必须与生产任务产品匹配'],
  [/permission denied|forbidden|unauthorized/i, '当前账号没有执行该操作的权限'],
  [/only .* can /i, '当前角色不能执行该操作'], [/database operation failed/i, '数据库操作未完成']
]

export function localizeMessage(message) {
  const text = String(message || '').trim()
  if (!text) return '请检查当前业务状态和填写内容'
  if (/[一-鿿]/.test(text)) return localizeText(text)
  for (const [pattern, replacement] of MESSAGE_REPLACEMENTS) {
    if (pattern.test(text)) return replacement
  }
  return '请检查当前业务状态、操作权限和填写内容'
}

export function completedMessage(actionLabel) {
  return `操作已完成：${actionLabel}`
}

export function incompleteMessage(actionLabel, cause) {
  return `操作未完成：${actionLabel}。${localizeMessage(cause?.message || cause)}`
}
