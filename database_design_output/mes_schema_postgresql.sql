-- PostgreSQL schema generated from mes_table_metadata.json
-- Target database: MESSystem

CREATE TABLE IF NOT EXISTS mes_customer_order (
    order_id BIGSERIAL,
    order_no VARCHAR(40) NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(40) NOT NULL,
    product_model VARCHAR(80) NOT NULL,
    order_qty INTEGER NOT NULL DEFAULT 0,
    unit VARCHAR(20) NOT NULL DEFAULT '条',
    delivery_date DATE NOT NULL,
    priority_level SMALLINT NOT NULL DEFAULT 3,
    order_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PLAN',
    source_system VARCHAR(30) DEFAULT 'ERP',
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id)
);
COMMENT ON TABLE mes_customer_order IS '客户订单表：存储轮胎客户订单主数据，作为生产任务来源。';
COMMENT ON COLUMN mes_customer_order.order_id IS '订单主键';
COMMENT ON COLUMN mes_customer_order.order_no IS '订单编号';
COMMENT ON COLUMN mes_customer_order.customer_name IS '客户名称';
COMMENT ON COLUMN mes_customer_order.product_id IS '产品主键';
COMMENT ON COLUMN mes_customer_order.product_code IS '产品编码快照';
COMMENT ON COLUMN mes_customer_order.product_model IS '轮胎型号/规格';
COMMENT ON COLUMN mes_customer_order.order_qty IS '订单数量';
COMMENT ON COLUMN mes_customer_order.unit IS '计量单位';
COMMENT ON COLUMN mes_customer_order.delivery_date IS '交付日期';
COMMENT ON COLUMN mes_customer_order.priority_level IS '优先级，1最高';
COMMENT ON COLUMN mes_customer_order.order_status IS '订单状态';
COMMENT ON COLUMN mes_customer_order.source_system IS '来源系统';
COMMENT ON COLUMN mes_customer_order.remark IS '备注';
COMMENT ON COLUMN mes_customer_order.created_at IS '创建时间';
COMMENT ON COLUMN mes_customer_order.updated_at IS '更新时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_customer_order_order_no ON mes_customer_order (order_no);
CREATE INDEX IF NOT EXISTS idx_mes_customer_order_order_status ON mes_customer_order (order_status);

CREATE TABLE IF NOT EXISTS mes_production_task (
    task_id BIGSERIAL,
    task_no VARCHAR(40) NOT NULL,
    order_id BIGINT NOT NULL,
    planner_id BIGINT NOT NULL,
    plan_qty INTEGER NOT NULL DEFAULT 0,
    planned_start_time TIMESTAMP NOT NULL,
    planned_end_time TIMESTAMP NOT NULL,
    target_line_id BIGINT,
    task_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    kitting_status VARCHAR(30) NOT NULL DEFAULT 'NOT_ANALYZED',
    release_time TIMESTAMP,
    close_time TIMESTAMP,
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id)
);
COMMENT ON TABLE mes_production_task IS '生产任务表：存储PMC计划员排产形成的生产任务。';
COMMENT ON COLUMN mes_production_task.task_id IS '任务主键';
COMMENT ON COLUMN mes_production_task.task_no IS '任务编号';
COMMENT ON COLUMN mes_production_task.order_id IS '来源订单';
COMMENT ON COLUMN mes_production_task.planner_id IS 'PMC计划员';
COMMENT ON COLUMN mes_production_task.plan_qty IS '计划数量';
COMMENT ON COLUMN mes_production_task.planned_start_time IS '计划开始时间';
COMMENT ON COLUMN mes_production_task.planned_end_time IS '计划完成时间';
COMMENT ON COLUMN mes_production_task.target_line_id IS '目标产线';
COMMENT ON COLUMN mes_production_task.task_status IS '任务状态';
COMMENT ON COLUMN mes_production_task.kitting_status IS '齐套状态';
COMMENT ON COLUMN mes_production_task.release_time IS '发布时间';
COMMENT ON COLUMN mes_production_task.close_time IS '闭环时间';
COMMENT ON COLUMN mes_production_task.remark IS '备注';
COMMENT ON COLUMN mes_production_task.created_at IS '创建时间';
COMMENT ON COLUMN mes_production_task.updated_at IS '更新时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_production_task_task_no ON mes_production_task (task_no);
CREATE INDEX IF NOT EXISTS idx_mes_production_task_task_status ON mes_production_task (task_status);
CREATE INDEX IF NOT EXISTS idx_mes_production_task_kitting_status ON mes_production_task (kitting_status);

CREATE TABLE IF NOT EXISTS mes_kitting_analysis (
    analysis_id BIGSERIAL,
    analysis_no VARCHAR(40) NOT NULL,
    task_id BIGINT NOT NULL,
    analysis_scope VARCHAR(100) NOT NULL,
    result_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    snapshot_time TIMESTAMP NOT NULL,
    material_ok SMALLINT NOT NULL DEFAULT 0,
    line_ok SMALLINT NOT NULL DEFAULT 0,
    equipment_ok SMALLINT NOT NULL DEFAULT 0,
    process_ok SMALLINT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (analysis_id)
);
COMMENT ON TABLE mes_kitting_analysis IS '齐套分析表：存储任务发布前的物料、产线、设备、工序齐套分析记录。';
COMMENT ON COLUMN mes_kitting_analysis.analysis_id IS '分析主键';
COMMENT ON COLUMN mes_kitting_analysis.analysis_no IS '分析编号';
COMMENT ON COLUMN mes_kitting_analysis.task_id IS '生产任务';
COMMENT ON COLUMN mes_kitting_analysis.analysis_scope IS '分析范围';
COMMENT ON COLUMN mes_kitting_analysis.result_status IS '分析结果';
COMMENT ON COLUMN mes_kitting_analysis.snapshot_time IS '数据快照时间';
COMMENT ON COLUMN mes_kitting_analysis.material_ok IS '物料是否齐套';
COMMENT ON COLUMN mes_kitting_analysis.line_ok IS '产线是否可用';
COMMENT ON COLUMN mes_kitting_analysis.equipment_ok IS '设备是否可用';
COMMENT ON COLUMN mes_kitting_analysis.process_ok IS '工艺是否完整';
COMMENT ON COLUMN mes_kitting_analysis.created_by IS '分析人';
COMMENT ON COLUMN mes_kitting_analysis.created_at IS '创建时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_kitting_analysis_analysis_no ON mes_kitting_analysis (analysis_no);
CREATE INDEX IF NOT EXISTS idx_mes_kitting_analysis_result_status ON mes_kitting_analysis (result_status);

CREATE TABLE IF NOT EXISTS mes_kitting_shortage_item (
    shortage_item_id BIGSERIAL,
    analysis_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    shortage_type VARCHAR(30) NOT NULL,
    resource_id BIGINT,
    resource_code VARCHAR(60),
    resource_name VARCHAR(100),
    required_qty NUMERIC(18,4) DEFAULT 0,
    available_qty NUMERIC(18,4) DEFAULT 0,
    shortage_qty NUMERIC(18,4) DEFAULT 0,
    impact_desc VARCHAR(500),
    suggestion VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (shortage_item_id)
);
COMMENT ON TABLE mes_kitting_shortage_item IS '齐套缺口明细表：存储齐套分析发现的物料、产线、设备、工序缺口。';
COMMENT ON COLUMN mes_kitting_shortage_item.shortage_item_id IS '缺口明细主键';
COMMENT ON COLUMN mes_kitting_shortage_item.analysis_id IS '齐套分析';
COMMENT ON COLUMN mes_kitting_shortage_item.task_id IS '生产任务';
COMMENT ON COLUMN mes_kitting_shortage_item.shortage_type IS '缺口类型 MATERIAL/LINE/EQUIPMENT/PROCESS';
COMMENT ON COLUMN mes_kitting_shortage_item.resource_id IS '资源主键';
COMMENT ON COLUMN mes_kitting_shortage_item.resource_code IS '资源编码';
COMMENT ON COLUMN mes_kitting_shortage_item.resource_name IS '资源名称';
COMMENT ON COLUMN mes_kitting_shortage_item.required_qty IS '需求数量';
COMMENT ON COLUMN mes_kitting_shortage_item.available_qty IS '可用数量';
COMMENT ON COLUMN mes_kitting_shortage_item.shortage_qty IS '缺口数量';
COMMENT ON COLUMN mes_kitting_shortage_item.impact_desc IS '影响说明';
COMMENT ON COLUMN mes_kitting_shortage_item.suggestion IS '处理建议';
COMMENT ON COLUMN mes_kitting_shortage_item.created_at IS '创建时间';
CREATE INDEX IF NOT EXISTS idx_mes_kitting_shortage_item_shortage_type ON mes_kitting_shortage_item (shortage_type);

CREATE TABLE IF NOT EXISTS mes_shortage_alert (
    alert_id BIGSERIAL,
    alert_no VARCHAR(40) NOT NULL,
    task_id BIGINT NOT NULL,
    analysis_id BIGINT,
    alert_type VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    alert_status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    receiver_role VARCHAR(50),
    alert_content VARCHAR(1000) NOT NULL,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (alert_id)
);
COMMENT ON TABLE mes_shortage_alert IS '欠料预警表：存储欠料及资源缺口预警处理记录。';
COMMENT ON COLUMN mes_shortage_alert.alert_id IS '预警主键';
COMMENT ON COLUMN mes_shortage_alert.alert_no IS '预警编号';
COMMENT ON COLUMN mes_shortage_alert.task_id IS '生产任务';
COMMENT ON COLUMN mes_shortage_alert.analysis_id IS '齐套分析';
COMMENT ON COLUMN mes_shortage_alert.alert_type IS '预警类型';
COMMENT ON COLUMN mes_shortage_alert.severity IS '严重级别';
COMMENT ON COLUMN mes_shortage_alert.alert_status IS '处理状态';
COMMENT ON COLUMN mes_shortage_alert.receiver_role IS '接收角色';
COMMENT ON COLUMN mes_shortage_alert.alert_content IS '预警内容';
COMMENT ON COLUMN mes_shortage_alert.resolved_at IS '解决时间';
COMMENT ON COLUMN mes_shortage_alert.created_at IS '创建时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_shortage_alert_alert_no ON mes_shortage_alert (alert_no);
CREATE INDEX IF NOT EXISTS idx_mes_shortage_alert_alert_type ON mes_shortage_alert (alert_type);
CREATE INDEX IF NOT EXISTS idx_mes_shortage_alert_alert_status ON mes_shortage_alert (alert_status);

CREATE TABLE IF NOT EXISTS mes_work_order (
    work_order_id BIGSERIAL,
    work_order_no VARCHAR(40) NOT NULL,
    task_id BIGINT NOT NULL,
    product_id BIGINT,
    line_id BIGINT NOT NULL,
    process_id BIGINT,
    planned_qty INTEGER NOT NULL DEFAULT 0,
    actual_qty INTEGER NOT NULL DEFAULT 0,
    priority_level SMALLINT NOT NULL DEFAULT 3,
    work_order_status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    batch_no VARCHAR(50),
    dispatch_time TIMESTAMP,
    receive_time TIMESTAMP,
    completed_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (work_order_id)
);
COMMENT ON TABLE mes_work_order IS '生产工单表：存储由生产任务拆分生成的车间执行工单。';
COMMENT ON COLUMN mes_work_order.work_order_id IS '工单主键';
COMMENT ON COLUMN mes_work_order.work_order_no IS '工单编号';
COMMENT ON COLUMN mes_work_order.task_id IS '生产任务';
COMMENT ON COLUMN mes_work_order.product_id IS '产品';
COMMENT ON COLUMN mes_work_order.line_id IS '产线';
COMMENT ON COLUMN mes_work_order.process_id IS '工序';
COMMENT ON COLUMN mes_work_order.planned_qty IS '计划数量';
COMMENT ON COLUMN mes_work_order.actual_qty IS '实际数量';
COMMENT ON COLUMN mes_work_order.priority_level IS '优先级';
COMMENT ON COLUMN mes_work_order.work_order_status IS '工单状态';
COMMENT ON COLUMN mes_work_order.batch_no IS '生产批次';
COMMENT ON COLUMN mes_work_order.dispatch_time IS '派发时间';
COMMENT ON COLUMN mes_work_order.receive_time IS '接收时间';
COMMENT ON COLUMN mes_work_order.completed_time IS '完成时间';
COMMENT ON COLUMN mes_work_order.created_at IS '创建时间';
COMMENT ON COLUMN mes_work_order.updated_at IS '更新时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_work_order_work_order_no ON mes_work_order (work_order_no);
CREATE INDEX IF NOT EXISTS idx_mes_work_order_work_order_status ON mes_work_order (work_order_status);
CREATE INDEX IF NOT EXISTS idx_mes_work_order_batch_no ON mes_work_order (batch_no);

CREATE TABLE IF NOT EXISTS mes_work_order_operation_log (
    operation_log_id BIGSERIAL,
    work_order_id BIGINT NOT NULL,
    operation_type VARCHAR(30) NOT NULL,
    before_status VARCHAR(30),
    after_status VARCHAR(30),
    operator_id BIGINT NOT NULL,
    operation_reason VARCHAR(500),
    operation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    undoable SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (operation_log_id)
);
COMMENT ON TABLE mes_work_order_operation_log IS '工单调度操作日志表：记录工单下发、暂停、优先级调整、撤销等调度操作。';
COMMENT ON COLUMN mes_work_order_operation_log.operation_log_id IS '日志主键';
COMMENT ON COLUMN mes_work_order_operation_log.work_order_id IS '工单';
COMMENT ON COLUMN mes_work_order_operation_log.operation_type IS '操作类型';
COMMENT ON COLUMN mes_work_order_operation_log.before_status IS '操作前状态';
COMMENT ON COLUMN mes_work_order_operation_log.after_status IS '操作后状态';
COMMENT ON COLUMN mes_work_order_operation_log.operator_id IS '操作人';
COMMENT ON COLUMN mes_work_order_operation_log.operation_reason IS '操作原因';
COMMENT ON COLUMN mes_work_order_operation_log.operation_time IS '操作时间';
COMMENT ON COLUMN mes_work_order_operation_log.undoable IS '是否可撤销';
CREATE INDEX IF NOT EXISTS idx_mes_work_order_operation_log_operation_type ON mes_work_order_operation_log (operation_type);

CREATE TABLE IF NOT EXISTS mes_material_requisition (
    requisition_id BIGSERIAL,
    requisition_no VARCHAR(40) NOT NULL,
    work_order_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    request_status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    request_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by BIGINT,
    approved_time TIMESTAMP,
    remark VARCHAR(500),
    PRIMARY KEY (requisition_id)
);
COMMENT ON TABLE mes_material_requisition IS '领料任务表：存储生产工单对应的物料申请任务。';
COMMENT ON COLUMN mes_material_requisition.requisition_id IS '领料任务主键';
COMMENT ON COLUMN mes_material_requisition.requisition_no IS '领料单号';
COMMENT ON COLUMN mes_material_requisition.work_order_id IS '生产工单';
COMMENT ON COLUMN mes_material_requisition.requested_by IS '申请人';
COMMENT ON COLUMN mes_material_requisition.request_status IS '领料状态';
COMMENT ON COLUMN mes_material_requisition.request_time IS '申请时间';
COMMENT ON COLUMN mes_material_requisition.approved_by IS '审批人';
COMMENT ON COLUMN mes_material_requisition.approved_time IS '审批时间';
COMMENT ON COLUMN mes_material_requisition.remark IS '备注';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_material_requisition_requisition_no ON mes_material_requisition (requisition_no);
CREATE INDEX IF NOT EXISTS idx_mes_material_requisition_request_status ON mes_material_requisition (request_status);

CREATE TABLE IF NOT EXISTS mes_material_requisition_item (
    requisition_item_id BIGSERIAL,
    requisition_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    required_qty NUMERIC(18,4) NOT NULL DEFAULT 0,
    issued_qty NUMERIC(18,4) NOT NULL DEFAULT 0,
    unit VARCHAR(20) NOT NULL,
    batch_no VARCHAR(50),
    item_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (requisition_item_id)
);
COMMENT ON TABLE mes_material_requisition_item IS '领料任务明细表：存储领料任务所需物料明细。';
COMMENT ON COLUMN mes_material_requisition_item.requisition_item_id IS '明细主键';
COMMENT ON COLUMN mes_material_requisition_item.requisition_id IS '领料任务';
COMMENT ON COLUMN mes_material_requisition_item.material_id IS '物料';
COMMENT ON COLUMN mes_material_requisition_item.required_qty IS '需求数量';
COMMENT ON COLUMN mes_material_requisition_item.issued_qty IS '已发数量';
COMMENT ON COLUMN mes_material_requisition_item.unit IS '单位';
COMMENT ON COLUMN mes_material_requisition_item.batch_no IS '指定批次';
COMMENT ON COLUMN mes_material_requisition_item.item_status IS '明细状态';
CREATE INDEX IF NOT EXISTS idx_mes_material_requisition_item_item_status ON mes_material_requisition_item (item_status);

CREATE TABLE IF NOT EXISTS mes_material (
    material_id BIGSERIAL,
    material_code VARCHAR(40) NOT NULL,
    material_name VARCHAR(100) NOT NULL,
    material_type VARCHAR(30) NOT NULL,
    specification VARCHAR(100),
    unit VARCHAR(20) NOT NULL,
    shelf_life_days INTEGER,
    enabled SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (material_id)
);
COMMENT ON TABLE mes_material IS '物料主数据表：存储轮胎生产所需原材料、半成品、辅料等物料主数据。';
COMMENT ON COLUMN mes_material.material_id IS '物料主键';
COMMENT ON COLUMN mes_material.material_code IS '物料编码';
COMMENT ON COLUMN mes_material.material_name IS '物料名称';
COMMENT ON COLUMN mes_material.material_type IS '物料类型';
COMMENT ON COLUMN mes_material.specification IS '规格';
COMMENT ON COLUMN mes_material.unit IS '单位';
COMMENT ON COLUMN mes_material.shelf_life_days IS '保质期天数';
COMMENT ON COLUMN mes_material.enabled IS '是否启用';
COMMENT ON COLUMN mes_material.created_at IS '创建时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_material_material_code ON mes_material (material_code);
CREATE INDEX IF NOT EXISTS idx_mes_material_material_type ON mes_material (material_type);

CREATE TABLE IF NOT EXISTS mes_inventory (
    inventory_id BIGSERIAL,
    material_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    batch_no VARCHAR(50) NOT NULL,
    available_qty NUMERIC(18,4) NOT NULL DEFAULT 0,
    reserved_qty NUMERIC(18,4) NOT NULL DEFAULT 0,
    frozen_qty NUMERIC(18,4) NOT NULL DEFAULT 0,
    quality_status VARCHAR(30) NOT NULL DEFAULT 'QUALIFIED',
    last_check_time TIMESTAMP,
    PRIMARY KEY (inventory_id)
);
COMMENT ON TABLE mes_inventory IS '库存表：存储物料库存数量、批次、库位和质量状态。';
COMMENT ON COLUMN mes_inventory.inventory_id IS '库存主键';
COMMENT ON COLUMN mes_inventory.material_id IS '物料';
COMMENT ON COLUMN mes_inventory.warehouse_id IS '仓库';
COMMENT ON COLUMN mes_inventory.location_id IS '库位';
COMMENT ON COLUMN mes_inventory.batch_no IS '批次号';
COMMENT ON COLUMN mes_inventory.available_qty IS '可用数量';
COMMENT ON COLUMN mes_inventory.reserved_qty IS '占用数量';
COMMENT ON COLUMN mes_inventory.frozen_qty IS '冻结数量';
COMMENT ON COLUMN mes_inventory.quality_status IS '质量状态';
COMMENT ON COLUMN mes_inventory.last_check_time IS '最近核对时间';
CREATE INDEX IF NOT EXISTS idx_mes_inventory_batch_no ON mes_inventory (batch_no);
CREATE INDEX IF NOT EXISTS idx_mes_inventory_quality_status ON mes_inventory (quality_status);

CREATE TABLE IF NOT EXISTS mes_warehouse (
    warehouse_id BIGSERIAL,
    warehouse_code VARCHAR(40) NOT NULL,
    warehouse_name VARCHAR(100) NOT NULL,
    warehouse_type VARCHAR(30) NOT NULL,
    enabled SMALLINT NOT NULL DEFAULT 1,
    PRIMARY KEY (warehouse_id)
);
COMMENT ON TABLE mes_warehouse IS '仓库表：存储仓库基础信息。';
COMMENT ON COLUMN mes_warehouse.warehouse_id IS '仓库主键';
COMMENT ON COLUMN mes_warehouse.warehouse_code IS '仓库编码';
COMMENT ON COLUMN mes_warehouse.warehouse_name IS '仓库名称';
COMMENT ON COLUMN mes_warehouse.warehouse_type IS '仓库类型';
COMMENT ON COLUMN mes_warehouse.enabled IS '是否启用';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_warehouse_warehouse_code ON mes_warehouse (warehouse_code);

CREATE TABLE IF NOT EXISTS mes_warehouse_location (
    location_id BIGSERIAL,
    warehouse_id BIGINT NOT NULL,
    location_code VARCHAR(40) NOT NULL,
    location_name VARCHAR(100) NOT NULL,
    enabled SMALLINT NOT NULL DEFAULT 1,
    PRIMARY KEY (location_id)
);
COMMENT ON TABLE mes_warehouse_location IS '库位表：存储仓库库位信息。';
COMMENT ON COLUMN mes_warehouse_location.location_id IS '库位主键';
COMMENT ON COLUMN mes_warehouse_location.warehouse_id IS '仓库';
COMMENT ON COLUMN mes_warehouse_location.location_code IS '库位编码';
COMMENT ON COLUMN mes_warehouse_location.location_name IS '库位名称';
COMMENT ON COLUMN mes_warehouse_location.enabled IS '是否启用';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_warehouse_location_location_code ON mes_warehouse_location (location_code);

CREATE TABLE IF NOT EXISTS mes_inventory_transaction (
    transaction_id BIGSERIAL,
    transaction_no VARCHAR(40) NOT NULL,
    material_id BIGINT NOT NULL,
    inventory_id BIGINT,
    transaction_type VARCHAR(30) NOT NULL,
    qty NUMERIC(18,4) NOT NULL DEFAULT 0,
    source_doc_type VARCHAR(30),
    source_doc_id BIGINT,
    operator_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (transaction_id)
);
COMMENT ON TABLE mes_inventory_transaction IS '库存流水表：记录物料入库、出库、冻结、释放、盘点调整等库存变动。';
COMMENT ON COLUMN mes_inventory_transaction.transaction_id IS '流水主键';
COMMENT ON COLUMN mes_inventory_transaction.transaction_no IS '流水编号';
COMMENT ON COLUMN mes_inventory_transaction.material_id IS '物料';
COMMENT ON COLUMN mes_inventory_transaction.inventory_id IS '库存';
COMMENT ON COLUMN mes_inventory_transaction.transaction_type IS '变动类型';
COMMENT ON COLUMN mes_inventory_transaction.qty IS '变动数量';
COMMENT ON COLUMN mes_inventory_transaction.source_doc_type IS '来源单据类型';
COMMENT ON COLUMN mes_inventory_transaction.source_doc_id IS '来源单据ID';
COMMENT ON COLUMN mes_inventory_transaction.operator_id IS '操作人';
COMMENT ON COLUMN mes_inventory_transaction.created_at IS '创建时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_inventory_transaction_transaction_no ON mes_inventory_transaction (transaction_no);
CREATE INDEX IF NOT EXISTS idx_mes_inventory_transaction_transaction_type ON mes_inventory_transaction (transaction_type);

CREATE TABLE IF NOT EXISTS mes_picking_task (
    picking_task_id BIGSERIAL,
    picking_task_no VARCHAR(40) NOT NULL,
    requisition_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    task_status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    assigned_to BIGINT,
    start_time TIMESTAMP,
    finish_time TIMESTAMP,
    PRIMARY KEY (picking_task_id)
);
COMMENT ON TABLE mes_picking_task IS '备货拣货任务表：存储仓库根据领料任务生成的备货与拣货任务。';
COMMENT ON COLUMN mes_picking_task.picking_task_id IS '拣货任务主键';
COMMENT ON COLUMN mes_picking_task.picking_task_no IS '拣货任务号';
COMMENT ON COLUMN mes_picking_task.requisition_id IS '领料任务';
COMMENT ON COLUMN mes_picking_task.warehouse_id IS '仓库';
COMMENT ON COLUMN mes_picking_task.task_status IS '任务状态';
COMMENT ON COLUMN mes_picking_task.assigned_to IS '拣货人';
COMMENT ON COLUMN mes_picking_task.start_time IS '开始时间';
COMMENT ON COLUMN mes_picking_task.finish_time IS '完成时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_picking_task_picking_task_no ON mes_picking_task (picking_task_no);
CREATE INDEX IF NOT EXISTS idx_mes_picking_task_task_status ON mes_picking_task (task_status);

CREATE TABLE IF NOT EXISTS mes_robot (
    robot_id BIGSERIAL,
    robot_code VARCHAR(40) NOT NULL,
    robot_name VARCHAR(100) NOT NULL,
    robot_status VARCHAR(30) NOT NULL DEFAULT 'IDLE',
    battery_level NUMERIC(5,2),
    current_location VARCHAR(100),
    enabled SMALLINT NOT NULL DEFAULT 1,
    PRIMARY KEY (robot_id)
);
COMMENT ON TABLE mes_robot IS '运输机器人表：存储AGV/运输机器人基础信息。';
COMMENT ON COLUMN mes_robot.robot_id IS '机器人主键';
COMMENT ON COLUMN mes_robot.robot_code IS '机器人编码';
COMMENT ON COLUMN mes_robot.robot_name IS '机器人名称';
COMMENT ON COLUMN mes_robot.robot_status IS '机器人状态';
COMMENT ON COLUMN mes_robot.battery_level IS '电量百分比';
COMMENT ON COLUMN mes_robot.current_location IS '当前位置';
COMMENT ON COLUMN mes_robot.enabled IS '是否启用';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_robot_robot_code ON mes_robot (robot_code);
CREATE INDEX IF NOT EXISTS idx_mes_robot_robot_status ON mes_robot (robot_status);

CREATE TABLE IF NOT EXISTS mes_robot_delivery_task (
    delivery_task_id BIGSERIAL,
    delivery_task_no VARCHAR(40) NOT NULL,
    picking_task_id BIGINT NOT NULL,
    robot_id BIGINT,
    from_location_id BIGINT NOT NULL,
    to_line_id BIGINT NOT NULL,
    delivery_status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    load_time TIMESTAMP,
    handover_time TIMESTAMP,
    PRIMARY KEY (delivery_task_id)
);
COMMENT ON TABLE mes_robot_delivery_task IS '机器人配送任务表：存储物料装载、配送和交接任务。';
COMMENT ON COLUMN mes_robot_delivery_task.delivery_task_id IS '配送任务主键';
COMMENT ON COLUMN mes_robot_delivery_task.delivery_task_no IS '配送任务号';
COMMENT ON COLUMN mes_robot_delivery_task.picking_task_id IS '拣货任务';
COMMENT ON COLUMN mes_robot_delivery_task.robot_id IS '机器人';
COMMENT ON COLUMN mes_robot_delivery_task.from_location_id IS '起点库位';
COMMENT ON COLUMN mes_robot_delivery_task.to_line_id IS '目标产线';
COMMENT ON COLUMN mes_robot_delivery_task.delivery_status IS '配送状态';
COMMENT ON COLUMN mes_robot_delivery_task.load_time IS '装载时间';
COMMENT ON COLUMN mes_robot_delivery_task.handover_time IS '交接时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_robot_delivery_task_delivery_task_no ON mes_robot_delivery_task (delivery_task_no);
CREATE INDEX IF NOT EXISTS idx_mes_robot_delivery_task_delivery_status ON mes_robot_delivery_task (delivery_status);

CREATE TABLE IF NOT EXISTS mes_work_report (
    report_id BIGSERIAL,
    report_no VARCHAR(40) NOT NULL,
    work_order_id BIGINT NOT NULL,
    batch_no VARCHAR(50),
    operator_id BIGINT NOT NULL,
    report_qty INTEGER NOT NULL DEFAULT 0,
    qualified_qty INTEGER NOT NULL DEFAULT 0,
    defect_qty INTEGER NOT NULL DEFAULT 0,
    work_hours NUMERIC(10,2) DEFAULT 0,
    report_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    report_status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    PRIMARY KEY (report_id)
);
COMMENT ON TABLE mes_work_report IS '生产报工表：存储操作工工单报工、产量和工时数据。';
COMMENT ON COLUMN mes_work_report.report_id IS '报工主键';
COMMENT ON COLUMN mes_work_report.report_no IS '报工编号';
COMMENT ON COLUMN mes_work_report.work_order_id IS '工单';
COMMENT ON COLUMN mes_work_report.batch_no IS '生产批次';
COMMENT ON COLUMN mes_work_report.operator_id IS '操作工';
COMMENT ON COLUMN mes_work_report.report_qty IS '报工数量';
COMMENT ON COLUMN mes_work_report.qualified_qty IS '合格数量';
COMMENT ON COLUMN mes_work_report.defect_qty IS '不合格数量';
COMMENT ON COLUMN mes_work_report.work_hours IS '工时';
COMMENT ON COLUMN mes_work_report.report_time IS '报工时间';
COMMENT ON COLUMN mes_work_report.report_status IS '报工状态';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_work_report_report_no ON mes_work_report (report_no);
CREATE INDEX IF NOT EXISTS idx_mes_work_report_report_status ON mes_work_report (report_status);

CREATE TABLE IF NOT EXISTS mes_piecework_wage (
    wage_id BIGSERIAL,
    report_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    piece_rate NUMERIC(10,4) NOT NULL DEFAULT 0,
    qualified_qty INTEGER NOT NULL DEFAULT 0,
    wage_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    settlement_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (wage_id)
);
COMMENT ON TABLE mes_piecework_wage IS '计件工资表：存储报工对应计件工资核算结果。';
COMMENT ON COLUMN mes_piecework_wage.wage_id IS '计件工资主键';
COMMENT ON COLUMN mes_piecework_wage.report_id IS '报工';
COMMENT ON COLUMN mes_piecework_wage.operator_id IS '操作工';
COMMENT ON COLUMN mes_piecework_wage.piece_rate IS '单件费率';
COMMENT ON COLUMN mes_piecework_wage.qualified_qty IS '合格数量';
COMMENT ON COLUMN mes_piecework_wage.wage_amount IS '工资金额';
COMMENT ON COLUMN mes_piecework_wage.settlement_status IS '结算状态';
COMMENT ON COLUMN mes_piecework_wage.created_at IS '创建时间';
CREATE INDEX IF NOT EXISTS idx_mes_piecework_wage_settlement_status ON mes_piecework_wage (settlement_status);

CREATE TABLE IF NOT EXISTS mes_quality_inspection (
    inspection_id BIGSERIAL,
    inspection_no VARCHAR(40) NOT NULL,
    work_order_id BIGINT NOT NULL,
    work_report_id BIGINT,
    sample_qty INTEGER NOT NULL DEFAULT 0,
    inspection_status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    inspector_id BIGINT NOT NULL,
    inspection_time TIMESTAMP,
    judgement_result VARCHAR(30),
    PRIMARY KEY (inspection_id)
);
COMMENT ON TABLE mes_quality_inspection IS '质量抽检单表：存储抽检工单、检验对象和检验结果。';
COMMENT ON COLUMN mes_quality_inspection.inspection_id IS '抽检单主键';
COMMENT ON COLUMN mes_quality_inspection.inspection_no IS '抽检单号';
COMMENT ON COLUMN mes_quality_inspection.work_order_id IS '工单';
COMMENT ON COLUMN mes_quality_inspection.work_report_id IS '报工';
COMMENT ON COLUMN mes_quality_inspection.sample_qty IS '抽检数量';
COMMENT ON COLUMN mes_quality_inspection.inspection_status IS '抽检状态';
COMMENT ON COLUMN mes_quality_inspection.inspector_id IS '质检员';
COMMENT ON COLUMN mes_quality_inspection.inspection_time IS '检验时间';
COMMENT ON COLUMN mes_quality_inspection.judgement_result IS '判定结果';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_quality_inspection_inspection_no ON mes_quality_inspection (inspection_no);
CREATE INDEX IF NOT EXISTS idx_mes_quality_inspection_inspection_status ON mes_quality_inspection (inspection_status);
CREATE INDEX IF NOT EXISTS idx_mes_quality_inspection_judgement_result ON mes_quality_inspection (judgement_result);

CREATE TABLE IF NOT EXISTS mes_quality_inspection_item (
    inspection_item_id BIGSERIAL,
    inspection_id BIGINT NOT NULL,
    item_code VARCHAR(40) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    standard_value VARCHAR(100),
    actual_value VARCHAR(100),
    item_result VARCHAR(30) NOT NULL,
    remark VARCHAR(500),
    PRIMARY KEY (inspection_item_id)
);
COMMENT ON TABLE mes_quality_inspection_item IS '质量检验项目表：存储抽检项目、标准值、实测值和项目判定。';
COMMENT ON COLUMN mes_quality_inspection_item.inspection_item_id IS '检验项目主键';
COMMENT ON COLUMN mes_quality_inspection_item.inspection_id IS '抽检单';
COMMENT ON COLUMN mes_quality_inspection_item.item_code IS '项目编码';
COMMENT ON COLUMN mes_quality_inspection_item.item_name IS '项目名称';
COMMENT ON COLUMN mes_quality_inspection_item.standard_value IS '标准值';
COMMENT ON COLUMN mes_quality_inspection_item.actual_value IS '实测值';
COMMENT ON COLUMN mes_quality_inspection_item.item_result IS '项目判定';
COMMENT ON COLUMN mes_quality_inspection_item.remark IS '备注';
CREATE INDEX IF NOT EXISTS idx_mes_quality_inspection_item_item_result ON mes_quality_inspection_item (item_result);

CREATE TABLE IF NOT EXISTS mes_rework_order (
    rework_order_id BIGSERIAL,
    rework_order_no VARCHAR(40) NOT NULL,
    source_work_order_id BIGINT NOT NULL,
    inspection_id BIGINT,
    rework_reason VARCHAR(500) NOT NULL,
    rework_status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    assigned_line_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP,
    PRIMARY KEY (rework_order_id)
);
COMMENT ON TABLE mes_rework_order IS '返工单表：存储质量不合格后的返工任务和追溯信息。';
COMMENT ON COLUMN mes_rework_order.rework_order_id IS '返工单主键';
COMMENT ON COLUMN mes_rework_order.rework_order_no IS '返工单号';
COMMENT ON COLUMN mes_rework_order.source_work_order_id IS '来源工单';
COMMENT ON COLUMN mes_rework_order.inspection_id IS '来源质检单';
COMMENT ON COLUMN mes_rework_order.rework_reason IS '返工原因';
COMMENT ON COLUMN mes_rework_order.rework_status IS '返工状态';
COMMENT ON COLUMN mes_rework_order.assigned_line_id IS '返工产线';
COMMENT ON COLUMN mes_rework_order.created_at IS '创建时间';
COMMENT ON COLUMN mes_rework_order.closed_at IS '关闭时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_rework_order_rework_order_no ON mes_rework_order (rework_order_no);
CREATE INDEX IF NOT EXISTS idx_mes_rework_order_rework_status ON mes_rework_order (rework_status);

CREATE TABLE IF NOT EXISTS mes_quality_trace (
    trace_id BIGSERIAL,
    trace_no VARCHAR(40) NOT NULL,
    order_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    work_order_id BIGINT NOT NULL,
    batch_no VARCHAR(50),
    inspection_id BIGINT,
    rework_order_id BIGINT,
    trace_status VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (trace_id)
);
COMMENT ON TABLE mes_quality_trace IS '质量追溯表：存储订单、任务、工单、质检、返工之间的追溯关系。';
COMMENT ON COLUMN mes_quality_trace.trace_id IS '追溯主键';
COMMENT ON COLUMN mes_quality_trace.trace_no IS '追溯编号';
COMMENT ON COLUMN mes_quality_trace.order_id IS '订单';
COMMENT ON COLUMN mes_quality_trace.task_id IS '任务';
COMMENT ON COLUMN mes_quality_trace.work_order_id IS '工单';
COMMENT ON COLUMN mes_quality_trace.batch_no IS '批次号';
COMMENT ON COLUMN mes_quality_trace.inspection_id IS '质检单';
COMMENT ON COLUMN mes_quality_trace.rework_order_id IS '返工单';
COMMENT ON COLUMN mes_quality_trace.trace_status IS '追溯状态';
COMMENT ON COLUMN mes_quality_trace.created_at IS '创建时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_quality_trace_trace_no ON mes_quality_trace (trace_no);
CREATE INDEX IF NOT EXISTS idx_mes_quality_trace_batch_no ON mes_quality_trace (batch_no);
CREATE INDEX IF NOT EXISTS idx_mes_quality_trace_trace_status ON mes_quality_trace (trace_status);

CREATE TABLE IF NOT EXISTS mes_equipment (
    equipment_id BIGSERIAL,
    equipment_code VARCHAR(40) NOT NULL,
    equipment_name VARCHAR(100) NOT NULL,
    equipment_type VARCHAR(30) NOT NULL,
    line_id BIGINT,
    equipment_status VARCHAR(30) NOT NULL DEFAULT 'IDLE',
    last_maintenance_time TIMESTAMP,
    enabled SMALLINT NOT NULL DEFAULT 1,
    PRIMARY KEY (equipment_id)
);
COMMENT ON TABLE mes_equipment IS '设备表：存储生产设备基础信息和运行状态。';
COMMENT ON COLUMN mes_equipment.equipment_id IS '设备主键';
COMMENT ON COLUMN mes_equipment.equipment_code IS '设备编码';
COMMENT ON COLUMN mes_equipment.equipment_name IS '设备名称';
COMMENT ON COLUMN mes_equipment.equipment_type IS '设备类型';
COMMENT ON COLUMN mes_equipment.line_id IS '所属产线';
COMMENT ON COLUMN mes_equipment.equipment_status IS '设备状态';
COMMENT ON COLUMN mes_equipment.last_maintenance_time IS '最近维护时间';
COMMENT ON COLUMN mes_equipment.enabled IS '是否启用';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_equipment_equipment_code ON mes_equipment (equipment_code);
CREATE INDEX IF NOT EXISTS idx_mes_equipment_equipment_type ON mes_equipment (equipment_type);
CREATE INDEX IF NOT EXISTS idx_mes_equipment_equipment_status ON mes_equipment (equipment_status);

CREATE TABLE IF NOT EXISTS mes_equipment_repair_report (
    repair_report_id BIGSERIAL,
    repair_report_no VARCHAR(40) NOT NULL,
    equipment_id BIGINT NOT NULL,
    work_order_id BIGINT,
    fault_level VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    fault_desc VARCHAR(1000) NOT NULL,
    reporter_id BIGINT NOT NULL,
    report_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    repair_status VARCHAR(30) NOT NULL DEFAULT 'REPORTED',
    PRIMARY KEY (repair_report_id)
);
COMMENT ON TABLE mes_equipment_repair_report IS '设备故障报修表：存储设备故障上报和报修单信息。';
COMMENT ON COLUMN mes_equipment_repair_report.repair_report_id IS '报修主键';
COMMENT ON COLUMN mes_equipment_repair_report.repair_report_no IS '报修单号';
COMMENT ON COLUMN mes_equipment_repair_report.equipment_id IS '设备';
COMMENT ON COLUMN mes_equipment_repair_report.work_order_id IS '关联工单';
COMMENT ON COLUMN mes_equipment_repair_report.fault_level IS '故障等级';
COMMENT ON COLUMN mes_equipment_repair_report.fault_desc IS '故障描述';
COMMENT ON COLUMN mes_equipment_repair_report.reporter_id IS '报修人';
COMMENT ON COLUMN mes_equipment_repair_report.report_time IS '报修时间';
COMMENT ON COLUMN mes_equipment_repair_report.repair_status IS '报修状态';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_equipment_repair_report_repair_report_no ON mes_equipment_repair_report (repair_report_no);
CREATE INDEX IF NOT EXISTS idx_mes_equipment_repair_report_repair_status ON mes_equipment_repair_report (repair_status);

CREATE TABLE IF NOT EXISTS mes_maintenance_order (
    maintenance_order_id BIGSERIAL,
    maintenance_order_no VARCHAR(40) NOT NULL,
    repair_report_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    maintainer_id BIGINT,
    maintenance_status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    dispatch_time TIMESTAMP,
    finish_time TIMESTAMP,
    result_desc VARCHAR(1000),
    PRIMARY KEY (maintenance_order_id)
);
COMMENT ON TABLE mes_maintenance_order IS '维修工单表：存储设备维修派发、处理和结果反馈。';
COMMENT ON COLUMN mes_maintenance_order.maintenance_order_id IS '维修工单主键';
COMMENT ON COLUMN mes_maintenance_order.maintenance_order_no IS '维修工单号';
COMMENT ON COLUMN mes_maintenance_order.repair_report_id IS '报修单';
COMMENT ON COLUMN mes_maintenance_order.equipment_id IS '设备';
COMMENT ON COLUMN mes_maintenance_order.maintainer_id IS '维修人';
COMMENT ON COLUMN mes_maintenance_order.maintenance_status IS '维修状态';
COMMENT ON COLUMN mes_maintenance_order.dispatch_time IS '派发时间';
COMMENT ON COLUMN mes_maintenance_order.finish_time IS '完成时间';
COMMENT ON COLUMN mes_maintenance_order.result_desc IS '维修结果';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_maintenance_order_maintenance_order_no ON mes_maintenance_order (maintenance_order_no);
CREATE INDEX IF NOT EXISTS idx_mes_maintenance_order_maintenance_status ON mes_maintenance_order (maintenance_status);

CREATE TABLE IF NOT EXISTS mes_maintenance_plan (
    maintenance_plan_id BIGSERIAL,
    equipment_id BIGINT NOT NULL,
    plan_cycle VARCHAR(30) NOT NULL,
    next_plan_time TIMESTAMP NOT NULL,
    plan_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (maintenance_plan_id)
);
COMMENT ON TABLE mes_maintenance_plan IS '设备维护计划表：存储设备周期维护计划。';
COMMENT ON COLUMN mes_maintenance_plan.maintenance_plan_id IS '维护计划主键';
COMMENT ON COLUMN mes_maintenance_plan.equipment_id IS '设备';
COMMENT ON COLUMN mes_maintenance_plan.plan_cycle IS '维护周期';
COMMENT ON COLUMN mes_maintenance_plan.next_plan_time IS '下次计划时间';
COMMENT ON COLUMN mes_maintenance_plan.plan_status IS '计划状态';
COMMENT ON COLUMN mes_maintenance_plan.created_at IS '创建时间';
CREATE INDEX IF NOT EXISTS idx_mes_maintenance_plan_next_plan_time ON mes_maintenance_plan (next_plan_time);
CREATE INDEX IF NOT EXISTS idx_mes_maintenance_plan_plan_status ON mes_maintenance_plan (plan_status);

CREATE TABLE IF NOT EXISTS mes_product (
    product_id BIGSERIAL,
    product_code VARCHAR(40) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_model VARCHAR(80) NOT NULL,
    specification VARCHAR(100),
    enabled SMALLINT NOT NULL DEFAULT 1,
    PRIMARY KEY (product_id)
);
COMMENT ON TABLE mes_product IS '产品主数据表：存储轮胎产品型号、规格和基础信息。';
COMMENT ON COLUMN mes_product.product_id IS '产品主键';
COMMENT ON COLUMN mes_product.product_code IS '产品编码';
COMMENT ON COLUMN mes_product.product_name IS '产品名称';
COMMENT ON COLUMN mes_product.product_model IS '产品型号';
COMMENT ON COLUMN mes_product.specification IS '规格';
COMMENT ON COLUMN mes_product.enabled IS '是否启用';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_product_product_code ON mes_product (product_code);
CREATE INDEX IF NOT EXISTS idx_mes_product_product_model ON mes_product (product_model);

CREATE TABLE IF NOT EXISTS mes_product_bom (
    bom_id BIGSERIAL,
    product_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    usage_qty NUMERIC(18,4) NOT NULL DEFAULT 0,
    unit VARCHAR(20) NOT NULL,
    process_id BIGINT,
    enabled SMALLINT NOT NULL DEFAULT 1,
    PRIMARY KEY (bom_id)
);
COMMENT ON TABLE mes_product_bom IS '产品BOM表：存储产品用料参数。';
COMMENT ON COLUMN mes_product_bom.bom_id IS 'BOM主键';
COMMENT ON COLUMN mes_product_bom.product_id IS '产品';
COMMENT ON COLUMN mes_product_bom.material_id IS '物料';
COMMENT ON COLUMN mes_product_bom.usage_qty IS '单位用量';
COMMENT ON COLUMN mes_product_bom.unit IS '单位';
COMMENT ON COLUMN mes_product_bom.process_id IS '使用工序';
COMMENT ON COLUMN mes_product_bom.enabled IS '是否启用';

CREATE TABLE IF NOT EXISTS mes_process_route (
    process_id BIGSERIAL,
    product_id BIGINT NOT NULL,
    process_code VARCHAR(40) NOT NULL,
    process_name VARCHAR(100) NOT NULL,
    process_seq INTEGER NOT NULL,
    standard_hours NUMERIC(10,2),
    required_equipment_type VARCHAR(30),
    enabled SMALLINT NOT NULL DEFAULT 1,
    PRIMARY KEY (process_id)
);
COMMENT ON TABLE mes_process_route IS '工艺路线表：存储轮胎产品生产工序和工艺顺序。';
COMMENT ON COLUMN mes_process_route.process_id IS '工序主键';
COMMENT ON COLUMN mes_process_route.product_id IS '产品';
COMMENT ON COLUMN mes_process_route.process_code IS '工序编码';
COMMENT ON COLUMN mes_process_route.process_name IS '工序名称';
COMMENT ON COLUMN mes_process_route.process_seq IS '工序顺序';
COMMENT ON COLUMN mes_process_route.standard_hours IS '标准工时';
COMMENT ON COLUMN mes_process_route.required_equipment_type IS '所需设备类型';
COMMENT ON COLUMN mes_process_route.enabled IS '是否启用';
CREATE INDEX IF NOT EXISTS idx_mes_process_route_process_seq ON mes_process_route (process_seq);

CREATE TABLE IF NOT EXISTS mes_production_line (
    line_id BIGSERIAL,
    line_code VARCHAR(40) NOT NULL,
    line_name VARCHAR(100) NOT NULL,
    line_type VARCHAR(30),
    daily_capacity INTEGER,
    line_status VARCHAR(30) NOT NULL DEFAULT 'IDLE',
    enabled SMALLINT NOT NULL DEFAULT 1,
    PRIMARY KEY (line_id)
);
COMMENT ON TABLE mes_production_line IS '生产产线表：存储生产线基础信息、产能和状态。';
COMMENT ON COLUMN mes_production_line.line_id IS '产线主键';
COMMENT ON COLUMN mes_production_line.line_code IS '产线编码';
COMMENT ON COLUMN mes_production_line.line_name IS '产线名称';
COMMENT ON COLUMN mes_production_line.line_type IS '产线类型';
COMMENT ON COLUMN mes_production_line.daily_capacity IS '日产能';
COMMENT ON COLUMN mes_production_line.line_status IS '产线状态';
COMMENT ON COLUMN mes_production_line.enabled IS '是否启用';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_production_line_line_code ON mes_production_line (line_code);
CREATE INDEX IF NOT EXISTS idx_mes_production_line_line_status ON mes_production_line (line_status);

CREATE TABLE IF NOT EXISTS mes_sync_log (
    sync_log_id BIGSERIAL,
    source_system VARCHAR(30) NOT NULL,
    sync_object VARCHAR(50) NOT NULL,
    business_key VARCHAR(100),
    sync_status VARCHAR(30) NOT NULL,
    error_message VARCHAR(1000),
    sync_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sync_log_id)
);
COMMENT ON TABLE mes_sync_log IS '数据同步日志表：记录ERP/WMS/设备等外部系统同步结果。';
COMMENT ON COLUMN mes_sync_log.sync_log_id IS '同步日志主键';
COMMENT ON COLUMN mes_sync_log.source_system IS '来源系统';
COMMENT ON COLUMN mes_sync_log.sync_object IS '同步对象';
COMMENT ON COLUMN mes_sync_log.business_key IS '业务键';
COMMENT ON COLUMN mes_sync_log.sync_status IS '同步状态';
COMMENT ON COLUMN mes_sync_log.error_message IS '错误信息';
COMMENT ON COLUMN mes_sync_log.sync_time IS '同步时间';
CREATE INDEX IF NOT EXISTS idx_mes_sync_log_source_system ON mes_sync_log (source_system);
CREATE INDEX IF NOT EXISTS idx_mes_sync_log_sync_object ON mes_sync_log (sync_object);
CREATE INDEX IF NOT EXISTS idx_mes_sync_log_sync_status ON mes_sync_log (sync_status);

CREATE TABLE IF NOT EXISTS mes_product_trace (
    product_trace_id BIGSERIAL,
    trace_code VARCHAR(60) NOT NULL,
    order_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    work_order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    batch_no VARCHAR(50) NOT NULL,
    trace_status VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_trace_id)
);
COMMENT ON TABLE mes_product_trace IS '产品追溯表：存储订单、物料、工序、质量的产品追溯主链路。';
COMMENT ON COLUMN mes_product_trace.product_trace_id IS '产品追溯主键';
COMMENT ON COLUMN mes_product_trace.trace_code IS '追溯码';
COMMENT ON COLUMN mes_product_trace.order_id IS '订单';
COMMENT ON COLUMN mes_product_trace.task_id IS '任务';
COMMENT ON COLUMN mes_product_trace.work_order_id IS '工单';
COMMENT ON COLUMN mes_product_trace.product_id IS '产品';
COMMENT ON COLUMN mes_product_trace.batch_no IS '生产批次';
COMMENT ON COLUMN mes_product_trace.trace_status IS '追溯状态';
COMMENT ON COLUMN mes_product_trace.created_at IS '创建时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_product_trace_trace_code ON mes_product_trace (trace_code);
CREATE INDEX IF NOT EXISTS idx_mes_product_trace_batch_no ON mes_product_trace (batch_no);
CREATE INDEX IF NOT EXISTS idx_mes_product_trace_trace_status ON mes_product_trace (trace_status);

CREATE TABLE IF NOT EXISTS mes_dashboard_metric (
    metric_id BIGSERIAL,
    metric_date DATE NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value NUMERIC(18,4) NOT NULL DEFAULT 0,
    metric_unit VARCHAR(20),
    dimension_key VARCHAR(100),
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (metric_id)
);
COMMENT ON TABLE mes_dashboard_metric IS '综合生产看板指标表：存储产量、订单、质量、设备等看板聚合指标。';
COMMENT ON COLUMN mes_dashboard_metric.metric_id IS '指标主键';
COMMENT ON COLUMN mes_dashboard_metric.metric_date IS '指标日期';
COMMENT ON COLUMN mes_dashboard_metric.metric_type IS '指标类型';
COMMENT ON COLUMN mes_dashboard_metric.metric_name IS '指标名称';
COMMENT ON COLUMN mes_dashboard_metric.metric_value IS '指标值';
COMMENT ON COLUMN mes_dashboard_metric.metric_unit IS '单位';
COMMENT ON COLUMN mes_dashboard_metric.dimension_key IS '维度键';
COMMENT ON COLUMN mes_dashboard_metric.generated_at IS '生成时间';
CREATE INDEX IF NOT EXISTS idx_mes_dashboard_metric_metric_date ON mes_dashboard_metric (metric_date);
CREATE INDEX IF NOT EXISTS idx_mes_dashboard_metric_metric_type ON mes_dashboard_metric (metric_type);

CREATE TABLE IF NOT EXISTS mes_management_feedback (
    feedback_id BIGSERIAL,
    feedback_no VARCHAR(40) NOT NULL,
    feedback_type VARCHAR(30) NOT NULL,
    related_doc_type VARCHAR(30),
    related_doc_id BIGINT,
    feedback_content VARCHAR(1000) NOT NULL,
    decision_action VARCHAR(1000),
    feedback_status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (feedback_id)
);
COMMENT ON TABLE mes_management_feedback IS '管理决策反馈表：存储异常处理、计划调整和管理反馈记录。';
COMMENT ON COLUMN mes_management_feedback.feedback_id IS '反馈主键';
COMMENT ON COLUMN mes_management_feedback.feedback_no IS '反馈编号';
COMMENT ON COLUMN mes_management_feedback.feedback_type IS '反馈类型';
COMMENT ON COLUMN mes_management_feedback.related_doc_type IS '关联单据类型';
COMMENT ON COLUMN mes_management_feedback.related_doc_id IS '关联单据ID';
COMMENT ON COLUMN mes_management_feedback.feedback_content IS '反馈内容';
COMMENT ON COLUMN mes_management_feedback.decision_action IS '决策动作';
COMMENT ON COLUMN mes_management_feedback.feedback_status IS '处理状态';
COMMENT ON COLUMN mes_management_feedback.created_by IS '创建人';
COMMENT ON COLUMN mes_management_feedback.created_at IS '创建时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_management_feedback_feedback_no ON mes_management_feedback (feedback_no);
CREATE INDEX IF NOT EXISTS idx_mes_management_feedback_feedback_type ON mes_management_feedback (feedback_type);
CREATE INDEX IF NOT EXISTS idx_mes_management_feedback_feedback_status ON mes_management_feedback (feedback_status);

CREATE TABLE IF NOT EXISTS mes_user (
    user_id BIGSERIAL,
    username VARCHAR(50) NOT NULL,
    real_name VARCHAR(100) NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    department VARCHAR(100),
    phone VARCHAR(30),
    enabled SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
);
COMMENT ON TABLE mes_user IS '用户表：存储系统用户基础信息。';
COMMENT ON COLUMN mes_user.user_id IS '用户主键';
COMMENT ON COLUMN mes_user.username IS '登录名';
COMMENT ON COLUMN mes_user.real_name IS '姓名';
COMMENT ON COLUMN mes_user.role_code IS '角色编码';
COMMENT ON COLUMN mes_user.department IS '部门';
COMMENT ON COLUMN mes_user.phone IS '电话';
COMMENT ON COLUMN mes_user.enabled IS '是否启用';
COMMENT ON COLUMN mes_user.created_at IS '创建时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_user_username ON mes_user (username);
CREATE INDEX IF NOT EXISTS idx_mes_user_role_code ON mes_user (role_code);

