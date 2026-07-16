const text = (key, label, required = true) => ({ key, label, type: 'text', required })
const number = (key, label, required = true) => ({ key, label, type: 'number', required })
const decimal = (key, label, required = true) => ({ key, label, type: 'decimal', required })
const date = (key, label, required = true) => ({ key, label, type: 'date', required })
const select = (key, label, options, required = true) => ({ key, label, type: 'select', options, required })
const json = (key, label, example, required = true) => ({ key, label, type: 'json', example, required })
const col = (key, label) => ({ key, label })

export const navigation = [
  { key: 'dashboard', label: '工作台', icon: 'LayoutDashboard', to: '/' },
  { key: 'executive', label: '经营驾驶舱', icon: 'ChartNoAxesCombined', to: '/executive', roles: ['GENERAL_MANAGER'] },
  { key: 'planning', label: '计划与工单', icon: 'ClipboardList', to: '/module/planning', permissions: ['planning.read', 'planning.work_order.read'] },
  { key: 'production', label: '生产报工', icon: 'Factory', to: '/module/production', permissions: ['production.read'] },
  { key: 'warehouse', label: '仓储物流', icon: 'Warehouse', to: '/module/warehouse', permissions: ['warehouse.read', 'warehouse.requisition.create'] },
  { key: 'quality', label: '质量管理', icon: 'BadgeCheck', to: '/module/quality', permissions: ['quality.read'] },
  { key: 'equipment', label: '设备维护', icon: 'Wrench', to: '/module/equipment', permissions: ['equipment.read', 'equipment.fault.report'] },
  { key: 'process', label: '工艺与主数据', icon: 'Route', to: '/module/process', permissions: ['process.read', 'master.read'] },
  { key: 'trace', label: '产品追溯', icon: 'ScanLine', to: '/module/trace', permissions: ['trace.read'] },
  { key: 'feedback', label: '管理反馈', icon: 'MessageSquareText', to: '/module/feedback', permissions: ['feedback.read', 'feedback.create'] },
  { key: 'access', label: '用户与权限', icon: 'ShieldCheck', to: '/module/access', permissions: ['user.read', 'role.read', 'system.health.read'] },
  { key: 'profile', label: '个人资料', icon: 'UserRound', to: '/profile' }
]

export const modules = {
  planning: {
    title: '计划与工单', eyebrow: '生产计划',
    sections: [
      {
        key: 'orders', title: '客户订单', endpoint: '/orders', rowKey: 'orderId',
        columns: [col('orderNo', '订单号'), col('customerName', '客户'), col('productId', '产品ID'), col('orderQty', '数量'), col('deliveryDate', '交期'), col('orderStatus', '状态')],
        actions: [{ label: '新建订单', permission: 'planning.order.create', method: 'post', path: '/orders', fields: [text('customerName', '客户名称'), number('productId', '产品ID'), number('orderQty', '订单数量'), date('deliveryDate', '交付日期', false), number('priorityLevel', '优先级', false)] }]
      },
      {
        key: 'tasks', title: '生产任务', endpoint: '/production-tasks', rowKey: 'taskId',
        columns: [col('taskNo', '任务号'), col('orderId', '订单ID'), col('productId', '产品ID'), col('planQty', '计划数量'), col('targetLineId', '目标产线'), col('kittingStatus', '齐套'), col('taskStatus', '状态')],
        actions: [{ label: '创建任务', permission: 'planning.task.create', method: 'post', path: '/production-tasks', fields: [number('orderId', '订单ID'), number('targetLineId', '目标产线ID', false), number('planQty', '计划数量', false)] }],
        rowActions: [
          { label: '齐套分析', permission: 'planning.task.release', method: 'post', path: row => `/production-tasks/${row.taskId}/kitting` },
          { label: '发布缺料预警', permission: 'planning.task.release', method: 'post', path: row => `/production-tasks/${row.taskId}/shortage-alerts`, visible: row => row.kittingStatus === 'SHORTAGE' }
        ]
      },
      {
        key: 'workOrders', title: '制造工单', endpoint: '/work-orders', rowKey: 'workOrderId',
        columns: [col('workOrderNo', '工单号'), col('taskId', '任务ID'), col('lineId', '产线'), col('processId', '工序'), col('plannedQty', '计划数'), col('assignedTo', '派发给'), col('workOrderStatus', '状态')],
        actions: [{ label: '制定工单', permission: 'planning.work_order.create', method: 'post', path: '/work-orders', fields: [number('taskId', '任务ID'), number('lineId', '产线ID'), number('processId', '工序ID'), number('plannedQty', '计划数量', false), text('batchNo', '生产批次', false)] }],
        rowActions: [
          { label: '派工', permission: 'planning.work_order.dispatch', method: 'post', path: (row, value) => `/work-orders/${row.workOrderId}/dispatch?operatorId=${value.operatorId}`, fields: [number('operatorId', '操作工ID')], visible: row => row.workOrderStatus === 'CREATED' },
          { label: '接单', permission: 'planning.work_order.receive', method: 'post', path: row => `/work-orders/${row.workOrderId}/receive`, visible: row => row.workOrderStatus === 'DISPATCHED' },
          { label: '退回', permission: 'planning.work_order.receive', method: 'post', path: row => `/work-orders/${row.workOrderId}/reject`, fields: [text('reason', '退回原因', false)], visible: row => row.workOrderStatus === 'DISPATCHED' }
        ]
      },
      {
        key: 'shortages', title: '缺料预警', endpoint: '/shortage-alerts', rowKey: 'alertId',
        columns: [col('alertNo', '预警号'), col('taskId', '任务ID'), col('materialName', '物料'), col('requiredQty', '需求'), col('availableQty', '可用'), col('shortageQty', '缺口'), col('alertStatus', '状态')],
        rowActions: [{ label: '接收预警', permission: 'warehouse.inventory.adjust', method: 'post', path: row => `/shortage-alerts/${row.alertId}/accept`, visible: row => row.alertStatus === 'OPEN' }]
      }
    ]
  },
  production: {
    title: '生产报工', eyebrow: '制造执行',
    sections: [
      { key: 'workOrders', title: '我的制造工单', endpoint: '/work-orders', rowKey: 'workOrderId', columns: [col('workOrderNo', '工单号'), col('batchNo', '批次'), col('plannedQty', '计划数'), col('actualQty', '完成数'), col('workOrderStatus', '状态')], rowActions: [{ label: '接单', permission: 'planning.work_order.receive', method: 'post', path: row => `/work-orders/${row.workOrderId}/receive`, visible: row => row.workOrderStatus === 'DISPATCHED' }] },
      {
        key: 'reports', title: '生产报工', endpoint: '/work-reports', rowKey: 'reportId',
        columns: [col('reportNo', '报工单'), col('workOrderId', '工单ID'), col('operatorId', '操作工'), col('reportQty', '报工数'), col('qualifiedQty', '合格数'), col('defectQty', '不良数'), col('reportStatus', '状态')],
        actions: [{ label: '提交报工', permission: 'production.report.create', method: 'post', path: '/work-reports', fields: [number('workOrderId', '工单ID'), number('reportQty', '报工数量'), number('qualifiedQty', '合格数量'), number('defectQty', '不良数量'), decimal('workHours', '工时', false)] }],
        rowActions: [
          { label: '审核通过', permission: 'production.report.review', method: 'post', path: row => `/work-reports/${row.reportId}/approve`, visible: row => row.reportStatus === 'SUBMITTED' },
          { label: '驳回', permission: 'production.report.review', method: 'post', path: row => `/work-reports/${row.reportId}/reject`, fields: [text('reason', '驳回原因')], visible: row => row.reportStatus === 'SUBMITTED' }
        ]
      },
      { key: 'wages', title: '计件工资', endpoint: '/piecework-wages', rowKey: 'wageId', columns: [col('wageId', 'ID'), col('reportId', '报工ID'), col('operatorId', '操作工'), col('recordCount', '计件数'), col('pieceRate', '单价'), col('wageAmount', '金额'), col('settlementStatus', '结算状态')] }
    ]
  },
  warehouse: {
    title: '仓储物流', eyebrow: '库存与配送',
    sections: [
      { key: 'requisitions', title: '领料申请', endpoint: '/requisitions', rowKey: 'requisitionId', columns: [col('requisitionNo', '领料单'), col('workOrderId', '工单'), col('warehouseId', '仓库'), col('requestedBy', '申请人'), col('requestStatus', '状态')], actions: [{ label: '发起领料', permission: 'warehouse.requisition.create', method: 'post', path: '/requisitions', fields: [number('workOrderId', '工单ID'), number('warehouseId', '目标仓库ID'), json('items', '领料明细', [{ materialId: 1, requiredQty: 10, unit: 'kg', batchNo: '' }]), text('remark', '备注', false)] }], rowActions: [
        { label: '接收', permission: 'warehouse.requisition.approve', method: 'post', path: row => `/requisitions/${row.requisitionId}/receive`, visible: row => row.requestStatus === 'CREATED' },
        { label: '批准', permission: 'warehouse.requisition.approve', method: 'post', path: row => `/requisitions/${row.requisitionId}/approve`, visible: row => row.requestStatus === 'RECEIVED' },
        { label: '驳回', permission: 'warehouse.requisition.approve', method: 'post', path: row => `/requisitions/${row.requisitionId}/reject`, fields: [text('remark', '驳回原因')], visible: row => ['CREATED', 'RECEIVED'].includes(row.requestStatus) }
      ] },
      { key: 'inventory', title: '库存台账', endpoint: '/inventory', rowKey: 'inventoryId', columns: [col('inventoryId', 'ID'), col('materialId', '物料'), col('warehouseId', '仓库'), col('locationId', '库位'), col('batchNo', '批次'), col('availableQty', '可用'), col('reservedQty', '预留'), col('qualityStatus', '质量状态')], actions: [{ label: '外部采购入库', permission: 'warehouse.inventory.adjust', method: 'post', path: '/inventory/external-purchase', fields: [number('materialId', '物料ID'), number('warehouseId', '仓库ID'), number('locationId', '库位ID', false), decimal('qty', '采购数量'), text('batchNo', '批次号', false), text('remark', '采购说明', false)] }] },
      { key: 'picking', title: '拣货任务', endpoint: '/picking-tasks', rowKey: 'pickingTaskId', columns: [col('pickingTaskNo', '拣货单'), col('requisitionId', '领料单'), col('warehouseId', '仓库'), col('taskStatus', '状态'), col('assignedTo', '执行人')], rowActions: [{ label: '完成拣货', permission: 'warehouse.picking.execute', method: 'post', path: row => `/picking-tasks/${row.pickingTaskId}/complete`, visible: row => row.taskStatus === 'CREATED' }] },
      { key: 'delivery', title: '配送任务', endpoint: '/robot-delivery-tasks', rowKey: 'deliveryTaskId', columns: [col('deliveryTaskNo', '配送单'), col('pickingTaskId', '拣货任务'), col('robotId', '机器人'), col('deliveryStatus', '状态'), col('targetLocation', '目标位置')], rowActions: [
        { label: '确认到达', permission: 'warehouse.delivery.execute', method: 'post', path: row => `/robot-delivery-tasks/${row.deliveryTaskId}/arrive`, visible: row => row.deliveryStatus === 'PENDING' },
        { label: '确认收料', permission: 'warehouse.requisition.create', method: 'post', path: row => `/robot-delivery-tasks/${row.deliveryTaskId}/confirm-receipt`, visible: row => row.deliveryStatus === 'ARRIVED' }
      ] },
      { key: 'materials', title: '物料主数据', endpoint: '/materials', rowKey: 'materialId', columns: [col('materialCode', '物料编码'), col('materialName', '名称'), col('materialType', '类型'), col('specification', '规格'), col('unit', '单位')], actions: [{ label: '新增物料', permission: 'warehouse.master.manage', method: 'post', path: '/materials', fields: [text('materialCode', '物料编码', false), text('materialName', '物料名称'), text('materialType', '物料类型'), text('specification', '规格', false), text('unit', '单位')] }] },
      { key: 'warehouses', title: '仓库', endpoint: '/warehouses', rowKey: 'warehouseId', columns: [col('warehouseCode', '仓库编码'), col('warehouseName', '名称'), col('warehouseType', '类型'), col('enabled', '启用')], actions: [{ label: '新增仓库', permission: 'warehouse.master.manage', method: 'post', path: '/warehouses', fields: [text('warehouseCode', '仓库编码', false), text('warehouseName', '仓库名称'), text('warehouseType', '仓库类型')] }] },
      { key: 'locations', title: '库位', endpoint: '/warehouses/locations', rowKey: 'locationId', columns: [col('locationCode', '库位编码'), col('locationName', '名称'), col('warehouseId', '仓库'), col('locationStatus', '状态')], actions: [{ label: '新增库位', permission: 'warehouse.master.manage', method: 'post', path: '/warehouses/locations', fields: [number('warehouseId', '仓库ID'), text('locationCode', '库位编码'), text('locationName', '库位名称')] }] }
    ]
  },
  quality: {
    title: '质量管理', eyebrow: '检验与返工',
    sections: [
      { key: 'inspections', title: '质量检验', endpoint: '/quality-inspections', rowKey: 'inspectionId', columns: [col('inspectionNo', '质检单'), col('workReportId', '报工ID'), col('assignedTo', '质检员'), col('inspectionStatus', '状态'), col('judgementResult', '判定')], actions: [{ label: '创建质检', permission: 'quality.inspection.create', method: 'post', path: '/quality-inspections', fields: [number('workReportId', '报工ID'), text('inspectionType', '检验类型', false)] }], rowActions: [
        { label: '分配质检员', permission: 'quality.inspection.assign', method: 'post', path: (row, value) => `/quality-inspections/${row.inspectionId}/assign?inspectorId=${value.inspectorId}`, fields: [number('inspectorId', '质检员ID')], visible: row => row.inspectionStatus === 'CREATED' },
        { label: '录入检验项', permission: 'quality.inspect', method: 'post', path: row => `/quality-inspections/${row.inspectionId}/items`, fields: [text('itemCode', '项目编码'), text('itemName', '项目名称'), text('standardValue', '标准值'), text('actualValue', '实测值'), select('itemResult', '项目结论', ['PASS', 'FAIL'])] },
        { label: '提交审核', permission: 'quality.inspect', method: 'post', path: row => `/quality-inspections/${row.inspectionId}/submit`, visible: row => ['CREATED', 'IN_PROGRESS'].includes(row.inspectionStatus) },
        { label: '质量判定', permission: 'quality.review', method: 'post', path: row => `/quality-inspections/${row.inspectionId}/judge`, fields: [select('judgementResult', '判定结果', ['PASS', 'FAIL', 'REWORK']), text('remark', '审核意见', false)], visible: row => row.inspectionStatus === 'SUBMITTED' }
      ] },
      { key: 'rework', title: '返工单', endpoint: '/rework-orders', rowKey: 'reworkOrderId', columns: [col('reworkOrderNo', '返工单'), col('inspectionId', '质检ID'), col('workOrderId', '工单ID'), col('reworkReason', '原因'), col('reworkStatus', '状态')], rowActions: [
        { label: '派发返工', permission: 'quality.rework.manage', method: 'post', path: row => `/rework-orders/${row.reworkOrderId}/dispatch`, visible: row => row.reworkStatus === 'CREATED' },
        { label: '完成返工', permission: 'quality.rework.manage', method: 'post', path: row => `/rework-orders/${row.reworkOrderId}/finish`, visible: row => row.reworkStatus === 'DISPATCHED' }
      ] }
    ]
  },
  equipment: {
    title: '设备维护', eyebrow: '设备保障',
    sections: [
      { key: 'equipment', title: '设备台账', endpoint: '/equipment', rowKey: 'equipmentId', columns: [col('equipmentCode', '设备编码'), col('equipmentName', '名称'), col('equipmentType', '类型'), col('lineId', '产线'), col('equipmentStatus', '状态'), col('enabled', '启用')], actions: [{ label: '新增设备', permission: 'equipment.manage', method: 'post', path: '/equipment', fields: [text('equipmentCode', '设备编码'), text('equipmentName', '设备名称'), text('equipmentType', '设备类型'), number('lineId', '产线ID', false)] }, { label: '设备报修', permission: 'equipment.fault.report', method: 'post', path: '/equipment-repair-reports', fields: [text('repairReportNo', '报修单号', false), number('equipmentId', '设备ID'), number('workOrderId', '关联工单ID', false), select('faultLevel', '故障级别', ['LOW', 'MEDIUM', 'HIGH', 'URGENT']), text('faultDesc', '故障描述')] }], rowActions: [{ label: '更新状态', permission: 'equipment.manage', method: 'put', path: row => `/equipment/${row.equipmentId}/status`, fields: [select('status', '设备状态', ['RUNNING', 'IDLE', 'FAULT', 'MAINTENANCE'])] }] },
      { key: 'repairs', title: '设备报修', endpoint: '/equipment-repair-reports', rowKey: 'repairReportId', columns: [col('repairReportNo', '报修单'), col('equipmentId', '设备'), col('faultLevel', '级别'), col('faultDesc', '故障描述'), col('repairStatus', '状态')], rowActions: [
        { label: '审核并转维修', permission: 'equipment.repair.review', method: 'post', path: row => `/equipment-repair-reports/${row.repairReportId}/approve`, visible: row => row.repairStatus === 'REPORTED' },
        { label: '补生成维修单', permission: 'equipment.maintenance.assign', method: 'post', path: row => `/equipment-repair-reports/${row.repairReportId}/to-maintenance-order`, visible: row => row.repairStatus === 'APPROVED' }
      ] },
      { key: 'maintenance', title: '维修工单', endpoint: '/maintenance-orders', rowKey: 'maintenanceOrderId', columns: [col('maintenanceOrderNo', '维修单'), col('equipmentId', '设备'), col('maintainerId', '维护员'), col('maintenanceStatus', '状态'), col('resultDesc', '维修结果')], rowActions: [
        { label: '派工', permission: 'equipment.maintenance.assign', method: 'post', path: (row, value) => `/maintenance-orders/${row.maintenanceOrderId}/assign?maintainerId=${value.maintainerId}`, fields: [number('maintainerId', '维护员ID')], visible: row => row.maintenanceStatus === 'CREATED' },
        { label: '提交维修结果', permission: 'equipment.maintenance.execute', method: 'post', path: row => `/maintenance-orders/${row.maintenanceOrderId}/finish`, fields: [text('resultDesc', '维修结果')], visible: row => ['ASSIGNED', 'IN_PROGRESS'].includes(row.maintenanceStatus) },
        { label: '验收', permission: 'equipment.maintenance.accept', method: 'post', path: row => `/maintenance-orders/${row.maintenanceOrderId}/accept`, visible: row => row.maintenanceStatus === 'FINISHED' }
      ] },
      { key: 'plans', title: '维护计划', endpoint: '/maintenance-plans', rowKey: 'maintenancePlanId', columns: [col('maintenancePlanId', 'ID'), col('equipmentId', '设备'), col('planCycle', '周期'), col('nextPlanTime', '下次维护'), col('planStatus', '状态')], actions: [{ label: '新增维护计划', permission: 'equipment.manage', method: 'post', path: '/maintenance-plans', fields: [number('equipmentId', '设备ID'), text('planCycle', '维护周期'), text('nextPlanTime', '下次维护时间'), select('planStatus', '状态', ['ACTIVE', 'PAUSED'])] }] }
    ]
  },
  process: {
    title: '工艺与主数据', eyebrow: '制造方法',
    sections: [
      { key: 'routes', title: '工艺路线', endpoint: '/process-routes', rowKey: 'processId', columns: [col('processCode', '工序编码'), col('processName', '工序名称'), col('productId', '产品'), col('processSeq', '顺序'), col('workCenter', '工作中心'), col('enabled', '启用')], actions: [{ label: '新增工艺路线', permission: 'process.manage', method: 'post', path: '/process-routes', fields: [number('productId', '产品ID', false), text('processCode', '工序编码', false), text('processName', '工序名称'), number('processSeq', '工序顺序'), text('workCenter', '工作中心', false)] }] },
      { key: 'products', title: '产品主数据', endpoint: '/products', rowKey: 'productId', columns: [col('productCode', '产品编码'), col('productName', '名称'), col('productModel', '型号'), col('specification', '规格'), col('enabled', '启用')], actions: [{ label: '新增产品', permission: 'master.manage', method: 'post', path: '/products', fields: [text('productCode', '产品编码', false), text('productName', '产品名称'), text('productModel', '产品型号', false), text('specification', '规格', false)] }] },
      { key: 'lines', title: '生产产线', endpoint: '/production-lines', rowKey: 'lineId', columns: [col('lineCode', '产线编码'), col('lineName', '名称'), col('lineType', '类型'), col('capacityPerDay', '日产能'), col('lineStatus', '状态'), col('enabled', '启用')], actions: [{ label: '新增产线', permission: 'master.manage', method: 'post', path: '/production-lines', fields: [text('lineCode', '产线编码', false), text('lineName', '产线名称'), text('lineType', '产线类型', false), number('capacityPerDay', '日产能', false)] }] }
    ]
  },
  trace: {
    title: '产品追溯', eyebrow: '全流程履历',
    sections: [
      { key: 'traces', title: '产品追溯链', endpoint: '/product-traces', rowKey: 'traceId', columns: [col('traceCode', '追溯码'), col('orderId', '订单'), col('taskId', '任务'), col('workOrderId', '工单'), col('batchNo', '批次'), col('traceStatus', '状态')], actions: [{ label: '创建追溯记录', permission: 'trace.create', method: 'post', path: '/product-traces', fields: [text('traceCode', '追溯码', false), number('orderId', '订单ID'), number('taskId', '任务ID'), number('workOrderId', '工单ID'), text('batchNo', '批次号'), select('traceStatus', '状态', ['NORMAL', 'QUALITY_RISK', 'REWORKED'])] }] },
      { key: 'tires', title: '轮胎二维码', endpoint: '/tire-labels', rowKey: 'tireId', columns: [col('serialNo', '轮胎序列号'), col('productName', '产品'), col('productModel', '规格'), col('batchNo', '批次'), col('warehouseName', '入库仓库'), col('tireStatus', '状态'), col('printCount', '打印次数')], actions: [{ label: '生成轮胎标签', permission: 'trace.tire.generate', method: 'post', path: '/tire-labels/generate', fields: [number('inspectionId', '质检单ID'), number('warehouseId', '仓库ID'), number('quantity', '生成数量', false)] }], rowActions: [{ label: '记录打印', permission: 'trace.tire.print', method: 'post', path: row => `/tire-labels/${row.tireId}/print`, fields: [text('remark', '打印备注', false)] }] }
    ]
  },
  feedback: {
    title: '管理反馈', eyebrow: '异常闭环',
    sections: [{ key: 'feedback', title: '工单管理反馈', endpoint: '/management-feedback?workOrderId=1', rowKey: 'feedbackId', columns: [col('feedbackNo', '反馈编号'), col('workOrderId', '工单'), col('feedbackType', '类型'), col('feedbackContent', '反馈内容'), col('feedbackStatus', '状态')], actions: [{ label: '新增反馈', permission: 'feedback.create', method: 'post', path: '/management-feedback', fields: [number('workOrderId', '工单ID'), text('feedbackType', '反馈类型'), text('feedbackContent', '反馈内容')] }], rowActions: [{ label: '关闭反馈', permission: 'feedback.close', method: 'post', path: row => `/management-feedback/${row.feedbackId}/close`, visible: row => row.feedbackStatus === 'OPEN' }] }]
  },
  access: {
    title: '用户与权限', eyebrow: '系统管理',
    sections: [
      { key: 'users', title: '用户账号', endpoint: '/users', rowKey: 'userId', columns: [col('username', '账号'), col('realName', '姓名'), col('roleCode', '主角色'), col('department', '部门'), col('phone', '电话'), col('enabled', '启用')], actions: [{ label: '新增用户', permission: 'user.create', method: 'post', path: '/users', fields: [text('username', '登录账号'), text('realName', '姓名'), text('password', '初始密码'), text('roleCode', '角色编码'), text('department', '部门', false), text('phone', '电话', false)] }], rowActions: [{ label: '设置角色', permission: 'user.update_role', method: 'put', path: row => `/access/users/${row.userId}/roles`, fields: [json('roleCodes', '角色编码', ['PRODUCTION_OPERATOR'])] }, { label: '设置数据范围', permission: 'data_scope.manage', method: 'put', path: row => `/access/users/${row.userId}/data-scopes`, fields: [json('lineIds', '产线ID', []), json('warehouseIds', '仓库ID', [])] }] },
      { key: 'roles', title: '角色', endpoint: '/access/roles', rowKey: 'roleId', columns: [col('roleCode', '角色编码'), col('roleName', '角色名称'), col('roleType', '类型'), col('roleLevel', '级别'), col('dataScopeType', '数据范围'), col('enabled', '启用')] },
      { key: 'permissions', title: '权限点', endpoint: '/access/permissions', rowKey: 'permissionCode', columns: [col('permissionCode', '权限编码'), col('permissionName', '名称'), col('moduleCode', '模块'), col('resourceType', '资源'), col('actionCode', '动作'), col('riskLevel', '风险')] },
      { key: 'applications', title: '权限申请', endpoint: '/access/permission-applications', rowKey: 'applyId', columns: [col('applyNo', '申请单'), col('applicantId', '申请人'), col('targetUserId', '目标用户'), col('fromRoleCode', '原角色'), col('toRoleCode', '目标角色'), col('applyStatus', '状态')], actions: [{ label: '提交权限申请', permission: 'permission.apply', method: 'post', path: '/access/permission-applications', fields: [number('targetUserId', '目标用户ID'), text('toRoleCode', '目标角色编码'), text('reason', '申请原因')] }], rowActions: [{ label: '审核', permission: 'permission.review', method: 'post', path: row => `/access/permission-applications/${row.applyId}/review`, fields: [select('decision', '审核结论', ['APPROVED', 'REJECTED']), text('comment', '审核意见', false)], visible: row => row.applyStatus === 'SUBMITTED' }, { label: '执行变更', permission: 'role.manage', method: 'post', path: row => `/access/permission-applications/${row.applyId}/apply`, visible: row => row.applyStatus === 'APPROVED' }] }
    ]
  }
}
