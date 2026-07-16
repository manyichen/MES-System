-- 云端 MES 调试数据精简脚本。
-- 系统配置表（部门、正式角色、权限、角色权限）保留完整；业务表按状态/场景保留 1-2 条。

-- 先精简带真实外键约束的轮胎实例，随后将保留记录对齐到将被保留的代表数据。
DELETE FROM mes_tire_instance
WHERE tire_id IN (
    SELECT tire_id FROM (
        SELECT tire_id, row_number() OVER (PARTITION BY tire_status ORDER BY tire_id) AS rn
        FROM mes_tire_instance
    ) ranked WHERE rn > 2
);

UPDATE mes_tire_instance
SET work_order_id = COALESCE(
        (SELECT MIN(work_order_id) FROM mes_work_order WHERE work_order_status = 'RECEIVED'),
        (SELECT MIN(work_order_id) FROM mes_work_order)),
    inspection_id = COALESCE(
        (SELECT MIN(inspection_id) FROM mes_quality_inspection WHERE inspection_status = 'APPROVED'),
        (SELECT MIN(inspection_id) FROM mes_quality_inspection)),
    work_report_id = COALESCE(
        (SELECT MIN(report_id) FROM mes_work_report WHERE report_status = 'APPROVED'),
        (SELECT MIN(report_id) FROM mes_work_report)),
    product_id = (SELECT MIN(product_id) FROM mes_product),
    warehouse_id = (SELECT MIN(warehouse_id) FROM mes_warehouse),
    location_id = (SELECT MIN(location_id) FROM mes_warehouse_location
                   WHERE warehouse_id = (SELECT MIN(warehouse_id) FROM mes_warehouse)),
    created_by = (SELECT user_id FROM mes_user WHERE username = 'admin');

-- 订单、计划、齐套和工单：每种状态最多保留 2 条。
DELETE FROM mes_customer_order
WHERE order_id IN (
    SELECT order_id FROM (
        SELECT order_id, row_number() OVER (PARTITION BY order_status ORDER BY order_id) AS rn
        FROM mes_customer_order
    ) ranked WHERE rn > 2
);

DELETE FROM mes_production_task
WHERE task_id IN (
    SELECT task_id FROM (
        SELECT task_id, row_number() OVER (
            PARTITION BY task_status, kitting_status ORDER BY task_id) AS rn
        FROM mes_production_task
    ) ranked WHERE rn > 1
);

DELETE FROM mes_kitting_analysis
WHERE analysis_id IN (
    SELECT analysis_id FROM (
        SELECT analysis_id, row_number() OVER (PARTITION BY result_status ORDER BY analysis_id) AS rn
        FROM mes_kitting_analysis
    ) ranked WHERE rn > 2
);

DELETE FROM mes_kitting_shortage_item
WHERE shortage_item_id IN (
    SELECT shortage_item_id FROM (
        SELECT shortage_item_id, row_number() OVER (PARTITION BY shortage_type ORDER BY shortage_item_id) AS rn
        FROM mes_kitting_shortage_item
    ) ranked WHERE rn > 2
);

DELETE FROM mes_shortage_alert
WHERE alert_id IN (
    SELECT alert_id FROM (
        SELECT alert_id, row_number() OVER (PARTITION BY alert_status ORDER BY alert_id) AS rn
        FROM mes_shortage_alert
    ) ranked WHERE rn > 2
);

DELETE FROM mes_work_order
WHERE work_order_id IN (
    SELECT work_order_id FROM (
        SELECT work_order_id, row_number() OVER (PARTITION BY work_order_status ORDER BY work_order_id) AS rn
        FROM mes_work_order
    ) ranked WHERE rn > 2
);

DELETE FROM mes_work_order_operation_log
WHERE operation_log_id IN (
    SELECT operation_log_id FROM (
        SELECT operation_log_id, row_number() OVER (PARTITION BY operation_type ORDER BY operation_log_id) AS rn
        FROM mes_work_order_operation_log
    ) ranked WHERE rn > 2
);

-- 仓储物流：领料、明细、拣货、配送、库存按状态/类型保留代表数据。
DELETE FROM mes_material_requisition
WHERE requisition_id IN (
    SELECT requisition_id FROM (
        SELECT requisition_id, row_number() OVER (PARTITION BY request_status ORDER BY requisition_id) AS rn
        FROM mes_material_requisition
    ) ranked WHERE rn > 2
);

DELETE FROM mes_material_requisition_item
WHERE requisition_item_id IN (
    SELECT requisition_item_id FROM (
        SELECT requisition_item_id, row_number() OVER (PARTITION BY item_status ORDER BY requisition_item_id) AS rn
        FROM mes_material_requisition_item
    ) ranked WHERE rn > 2
);

DELETE FROM mes_inventory
WHERE inventory_id IN (
    SELECT inventory_id FROM (
        SELECT inventory_id, row_number() OVER (PARTITION BY quality_status ORDER BY inventory_id) AS rn
        FROM mes_inventory
    ) ranked WHERE rn > 2
);

DELETE FROM mes_inventory_transaction
WHERE transaction_id IN (
    SELECT transaction_id FROM (
        SELECT transaction_id, row_number() OVER (PARTITION BY transaction_type ORDER BY transaction_id) AS rn
        FROM mes_inventory_transaction
    ) ranked WHERE rn > 2
);

DELETE FROM mes_picking_task
WHERE picking_task_id IN (
    SELECT picking_task_id FROM (
        SELECT picking_task_id, row_number() OVER (PARTITION BY task_status ORDER BY picking_task_id) AS rn
        FROM mes_picking_task
    ) ranked WHERE rn > 2
);

DELETE FROM mes_robot
WHERE robot_id IN (
    SELECT robot_id FROM (
        SELECT robot_id, row_number() OVER (PARTITION BY robot_status ORDER BY robot_id) AS rn
        FROM mes_robot
    ) ranked WHERE rn > 2
);

DELETE FROM mes_robot_delivery_task
WHERE delivery_task_id IN (
    SELECT delivery_task_id FROM (
        SELECT delivery_task_id, row_number() OVER (PARTITION BY delivery_status ORDER BY delivery_task_id) AS rn
        FROM mes_robot_delivery_task
    ) ranked WHERE rn > 2
);

-- 生产、质量和追溯：每种业务状态最多保留 2 条。
DELETE FROM mes_work_report
WHERE report_id IN (
    SELECT report_id FROM (
        SELECT report_id, row_number() OVER (PARTITION BY report_status ORDER BY report_id) AS rn
        FROM mes_work_report
    ) ranked WHERE rn > 2
);

DELETE FROM mes_piecework_wage
WHERE wage_id IN (
    SELECT wage_id FROM (
        SELECT wage_id, row_number() OVER (PARTITION BY settlement_status ORDER BY wage_id) AS rn
        FROM mes_piecework_wage
    ) ranked WHERE rn > 2
);

DELETE FROM mes_quality_inspection
WHERE inspection_id IN (
    SELECT inspection_id FROM (
        SELECT inspection_id, row_number() OVER (PARTITION BY inspection_status ORDER BY inspection_id) AS rn
        FROM mes_quality_inspection
    ) ranked WHERE rn > 2
);

DELETE FROM mes_quality_inspection_item
WHERE inspection_item_id IN (
    SELECT inspection_item_id FROM (
        SELECT inspection_item_id, row_number() OVER (PARTITION BY item_result ORDER BY inspection_item_id) AS rn
        FROM mes_quality_inspection_item
    ) ranked WHERE rn > 2
);

DELETE FROM mes_rework_order
WHERE rework_order_id IN (
    SELECT rework_order_id FROM (
        SELECT rework_order_id, row_number() OVER (PARTITION BY rework_status ORDER BY rework_order_id) AS rn
        FROM mes_rework_order
    ) ranked WHERE rn > 2
);

DELETE FROM mes_quality_trace
WHERE trace_id IN (
    SELECT trace_id FROM (
        SELECT trace_id, row_number() OVER (PARTITION BY trace_status ORDER BY trace_id) AS rn
        FROM mes_quality_trace
    ) ranked WHERE rn > 2
);

DELETE FROM mes_product_trace
WHERE product_trace_id IN (
    SELECT product_trace_id FROM (
        SELECT product_trace_id, row_number() OVER (PARTITION BY trace_status ORDER BY product_trace_id) AS rn
        FROM mes_product_trace
    ) ranked WHERE rn > 2
);

-- 设备场景：设备、报修、维修、维护计划均按状态保留代表数据。
DELETE FROM mes_equipment
WHERE equipment_id IN (
    SELECT equipment_id FROM (
        SELECT equipment_id, row_number() OVER (PARTITION BY equipment_status ORDER BY equipment_id) AS rn
        FROM mes_equipment
    ) ranked WHERE rn > 2
);

DELETE FROM mes_equipment_repair_report
WHERE repair_report_id IN (
    SELECT repair_report_id FROM (
        SELECT repair_report_id, row_number() OVER (PARTITION BY repair_status ORDER BY repair_report_id) AS rn
        FROM mes_equipment_repair_report
    ) ranked WHERE rn > 2
);

DELETE FROM mes_maintenance_order
WHERE maintenance_order_id IN (
    SELECT maintenance_order_id FROM (
        SELECT maintenance_order_id, row_number() OVER (PARTITION BY maintenance_status ORDER BY maintenance_order_id) AS rn
        FROM mes_maintenance_order
    ) ranked WHERE rn > 2
);

DELETE FROM mes_maintenance_plan
WHERE maintenance_plan_id IN (
    SELECT maintenance_plan_id FROM (
        SELECT maintenance_plan_id, row_number() OVER (PARTITION BY plan_status ORDER BY maintenance_plan_id) AS rn
        FROM mes_maintenance_plan
    ) ranked WHERE rn > 2
);

DELETE FROM mes_management_feedback
WHERE feedback_id IN (
    SELECT feedback_id FROM (
        SELECT feedback_id, row_number() OVER (
            PARTITION BY feedback_status, feedback_type ORDER BY feedback_id) AS rn
        FROM mes_management_feedback
    ) ranked WHERE rn > 1
);

-- 权限申请保留每种审批状态，审计日志按事件和结果各保留 2 条；会话全部清空。
DELETE FROM mes_permission_apply
WHERE apply_id IN (
    SELECT apply_id FROM (
        SELECT apply_id, row_number() OVER (PARTITION BY apply_status ORDER BY apply_id) AS rn
        FROM mes_permission_apply
    ) ranked WHERE rn > 2
);

DELETE FROM mes_audit_log
WHERE audit_id IN (
    SELECT audit_id FROM (
        SELECT audit_id, row_number() OVER (PARTITION BY event_type, result ORDER BY audit_id DESC) AS rn
        FROM mes_audit_log
    ) ranked WHERE rn > 2
);

DELETE FROM mes_user_session;
DELETE FROM mes_dashboard_metric;

-- 主数据按主要业务场景精简：产品 2 个、物料每类型 2 个、产线每状态 2 条、仓库每类型 2 个。
DELETE FROM mes_product
WHERE product_id IN (
    SELECT product_id FROM (
        SELECT product_id, row_number() OVER (PARTITION BY enabled ORDER BY product_id) AS rn
        FROM mes_product
    ) ranked WHERE rn > 2
);

DELETE FROM mes_material
WHERE material_id IN (
    SELECT material_id FROM (
        SELECT material_id, row_number() OVER (PARTITION BY material_type ORDER BY material_id) AS rn
        FROM mes_material
    ) ranked WHERE rn > 2
);

DELETE FROM mes_production_line
WHERE line_id IN (
    SELECT line_id FROM (
        SELECT line_id, row_number() OVER (PARTITION BY line_status ORDER BY line_id) AS rn
        FROM mes_production_line
    ) ranked WHERE rn > 2
);

DELETE FROM mes_warehouse
WHERE warehouse_id IN (
    SELECT warehouse_id FROM (
        SELECT warehouse_id, row_number() OVER (PARTITION BY warehouse_type ORDER BY warehouse_id) AS rn
        FROM mes_warehouse
    ) ranked WHERE rn > 2
);

DELETE FROM mes_warehouse_location
WHERE warehouse_id NOT IN (SELECT warehouse_id FROM mes_warehouse)
   OR location_id IN (
       SELECT location_id FROM (
           SELECT location_id, row_number() OVER (PARTITION BY warehouse_id ORDER BY location_id) AS rn
           FROM mes_warehouse_location
       ) ranked WHERE rn > 1
   );

DELETE FROM mes_process_route
WHERE process_id IN (
    SELECT process_id FROM (
        SELECT process_id, row_number() OVER (
            PARTITION BY COALESCE(required_equipment_type, '<NULL>') ORDER BY process_id) AS rn
        FROM mes_process_route
    ) ranked WHERE rn > 1
);

DELETE FROM mes_product_bom
WHERE bom_id NOT IN (SELECT bom_id FROM mes_product_bom ORDER BY bom_id LIMIT 2);

DELETE FROM mes_quality_standard
WHERE standard_id NOT IN (SELECT standard_id FROM mes_quality_standard ORDER BY standard_id LIMIT 2);

DELETE FROM mes_quality_standard_item
WHERE standard_item_id NOT IN (SELECT standard_item_id FROM mes_quality_standard_item ORDER BY standard_item_id LIMIT 2);

-- 将保留业务记录的上下游引用统一对齐到代表主数据，避免调试数据产生逻辑孤儿。
UPDATE mes_customer_order
SET product_id = (SELECT MIN(product_id) FROM mes_product),
    product_code = (SELECT product_code FROM mes_product ORDER BY product_id LIMIT 1),
    product_model = (SELECT product_model FROM mes_product ORDER BY product_id LIMIT 1),
    created_by = (SELECT user_id FROM mes_user WHERE username = 'mes_pmc'),
    updated_by = (SELECT user_id FROM mes_user WHERE username = 'mes_pmc');

UPDATE mes_production_task
SET order_id = COALESCE(
        (SELECT MIN(order_id) FROM mes_customer_order WHERE order_status = 'PLANNED'),
        (SELECT MIN(order_id) FROM mes_customer_order)),
    planner_id = (SELECT user_id FROM mes_user WHERE username = 'mes_pmc'),
    target_line_id = COALESCE(
        (SELECT MIN(line_id) FROM mes_production_line WHERE line_status = 'RUNNING'),
        (SELECT MIN(line_id) FROM mes_production_line)),
    created_by = (SELECT user_id FROM mes_user WHERE username = 'mes_pmc'),
    updated_by = (SELECT user_id FROM mes_user WHERE username = 'mes_pmc');

UPDATE mes_kitting_analysis
SET task_id = CASE result_status
        WHEN 'SHORTAGE' THEN COALESCE(
            (SELECT MIN(task_id) FROM mes_production_task WHERE kitting_status = 'SHORTAGE'),
            (SELECT MIN(task_id) FROM mes_production_task))
        ELSE COALESCE(
            (SELECT MIN(task_id) FROM mes_production_task WHERE kitting_status = 'READY'),
            (SELECT MIN(task_id) FROM mes_production_task)) END,
    created_by = (SELECT user_id FROM mes_user WHERE username = 'mes_pmc');

UPDATE mes_kitting_shortage_item
SET analysis_id = COALESCE(
        (SELECT MIN(analysis_id) FROM mes_kitting_analysis WHERE result_status = 'SHORTAGE'),
        (SELECT MIN(analysis_id) FROM mes_kitting_analysis)),
    task_id = COALESCE(
        (SELECT MIN(task_id) FROM mes_production_task WHERE kitting_status = 'SHORTAGE'),
        (SELECT MIN(task_id) FROM mes_production_task)),
    resource_id = (SELECT MIN(material_id) FROM mes_material),
    resource_code = (SELECT material_code FROM mes_material ORDER BY material_id LIMIT 1),
    resource_name = (SELECT material_name FROM mes_material ORDER BY material_id LIMIT 1);

UPDATE mes_shortage_alert
SET task_id = COALESCE(
        (SELECT MIN(task_id) FROM mes_production_task WHERE kitting_status = 'SHORTAGE'),
        (SELECT MIN(task_id) FROM mes_production_task)),
    analysis_id = COALESCE(
        (SELECT MIN(analysis_id) FROM mes_kitting_analysis WHERE result_status = 'SHORTAGE'),
        (SELECT MIN(analysis_id) FROM mes_kitting_analysis)),
    material_id = (SELECT MIN(material_id) FROM mes_material),
    material_code = (SELECT material_code FROM mes_material ORDER BY material_id LIMIT 1),
    material_name = (SELECT material_name FROM mes_material ORDER BY material_id LIMIT 1),
    receiver_role = 'WAREHOUSE_ADMIN',
    accepted_by = CASE WHEN alert_status = 'ACCEPTED'
        THEN (SELECT user_id FROM mes_user WHERE username = 'mes_warehouse') ELSE NULL END;

UPDATE mes_process_route
SET product_id = (SELECT MIN(product_id) FROM mes_product);

UPDATE mes_product_bom
SET product_id = (SELECT MIN(product_id) FROM mes_product),
    material_id = (SELECT MIN(material_id) FROM mes_material),
    process_id = (SELECT MIN(process_id) FROM mes_process_route);

UPDATE mes_work_order
SET task_id = COALESCE(
        (SELECT MIN(task_id) FROM mes_production_task WHERE task_status = 'RELEASED'),
        (SELECT MIN(task_id) FROM mes_production_task)),
    product_id = (SELECT MIN(product_id) FROM mes_product),
    line_id = COALESCE(
        (SELECT MIN(line_id) FROM mes_production_line WHERE line_status = 'RUNNING'),
        (SELECT MIN(line_id) FROM mes_production_line)),
    process_id = (SELECT MIN(process_id) FROM mes_process_route),
    assigned_to = CASE WHEN work_order_status = 'CREATED' THEN NULL
        ELSE (SELECT user_id FROM mes_user WHERE username = 'mes_operator') END,
    accepted_by = CASE WHEN work_order_status = 'RECEIVED'
        THEN (SELECT user_id FROM mes_user WHERE username = 'mes_operator') ELSE NULL END,
    updated_by = (SELECT user_id FROM mes_user WHERE username = 'mes_workshop');

UPDATE mes_work_order_operation_log
SET work_order_id = COALESCE(
        (SELECT MIN(work_order_id) FROM mes_work_order
         WHERE work_order_status = mes_work_order_operation_log.after_status),
        (SELECT MIN(work_order_id) FROM mes_work_order)),
    operator_id = CASE WHEN operation_type IN ('RECEIVE', 'REJECT')
        THEN (SELECT user_id FROM mes_user WHERE username = 'mes_operator')
        ELSE (SELECT user_id FROM mes_user WHERE username = 'mes_workshop') END;

UPDATE mes_material_requisition
SET work_order_id = COALESCE(
        (SELECT MIN(work_order_id) FROM mes_work_order WHERE work_order_status = 'RECEIVED'),
        (SELECT MIN(work_order_id) FROM mes_work_order)),
    requested_by = (SELECT user_id FROM mes_user WHERE username = 'mes_operator'),
    approved_by = CASE WHEN request_status IN ('APPROVED', 'COMPLETED', 'REJECTED')
        THEN (SELECT user_id FROM mes_user WHERE username = 'mes_warehouse') ELSE NULL END,
    warehouse_id = (SELECT MIN(warehouse_id) FROM mes_warehouse);

UPDATE mes_material_requisition_item
SET requisition_id = COALESCE(
        (SELECT MIN(requisition_id) FROM mes_material_requisition
         WHERE request_status = CASE WHEN item_status = 'COMPLETED' THEN 'COMPLETED' ELSE 'CREATED' END),
        (SELECT MIN(requisition_id) FROM mes_material_requisition)),
    material_id = (SELECT MIN(material_id) FROM mes_material);

UPDATE mes_inventory
SET material_id = (SELECT MIN(material_id) FROM mes_material),
    warehouse_id = (SELECT MIN(warehouse_id) FROM mes_warehouse),
    location_id = (SELECT MIN(location_id) FROM mes_warehouse_location
                   WHERE warehouse_id = (SELECT MIN(warehouse_id) FROM mes_warehouse));

UPDATE mes_inventory_transaction
SET material_id = (SELECT MIN(material_id) FROM mes_material),
    inventory_id = (SELECT MIN(inventory_id) FROM mes_inventory),
    source_doc_id = CASE WHEN source_doc_type = 'PICKING_TASK'
        THEN (SELECT MIN(picking_task_id) FROM mes_picking_task) ELSE source_doc_id END,
    operator_id = (SELECT user_id FROM mes_user WHERE username = 'mes_warehouse');

UPDATE mes_picking_task
SET requisition_id = COALESCE(
        (SELECT MIN(requisition_id) FROM mes_material_requisition
         WHERE request_status = CASE WHEN task_status = 'COMPLETED' THEN 'COMPLETED' ELSE 'APPROVED' END),
        (SELECT MIN(requisition_id) FROM mes_material_requisition)),
    warehouse_id = (SELECT MIN(warehouse_id) FROM mes_warehouse),
    assigned_to = (SELECT user_id FROM mes_user WHERE username = 'mes_warehouse');

UPDATE mes_robot
SET warehouse_id = (SELECT MIN(warehouse_id) FROM mes_warehouse);

UPDATE mes_robot_delivery_task
SET picking_task_id = (SELECT MIN(picking_task_id) FROM mes_picking_task),
    robot_id = (SELECT MIN(robot_id) FROM mes_robot),
    from_location_id = (SELECT MIN(location_id) FROM mes_warehouse_location),
    to_line_id = (SELECT MIN(line_id) FROM mes_production_line);

UPDATE mes_work_report
SET work_order_id = COALESCE(
        (SELECT MIN(work_order_id) FROM mes_work_order WHERE work_order_status = 'RECEIVED'),
        (SELECT MIN(work_order_id) FROM mes_work_order)),
    operator_id = (SELECT user_id FROM mes_user WHERE username = 'mes_operator'),
    approved_by = CASE WHEN report_status = 'APPROVED'
        THEN (SELECT user_id FROM mes_user WHERE username = 'mes_workshop') ELSE NULL END;

UPDATE mes_piecework_wage
SET report_id = COALESCE(
        (SELECT MIN(report_id) FROM mes_work_report WHERE report_status = 'APPROVED'),
        (SELECT MIN(report_id) FROM mes_work_report)),
    operator_id = (SELECT user_id FROM mes_user WHERE username = 'mes_operator');

UPDATE mes_quality_inspection
SET work_order_id = COALESCE(
        (SELECT MIN(work_order_id) FROM mes_work_order WHERE work_order_status = 'RECEIVED'),
        (SELECT MIN(work_order_id) FROM mes_work_order)),
    work_report_id = CASE WHEN inspection_status = 'CREATED'
        THEN COALESCE((SELECT MIN(report_id) FROM mes_work_report WHERE report_status = 'SUBMITTED'),
                      (SELECT MIN(report_id) FROM mes_work_report))
        ELSE COALESCE((SELECT MIN(report_id) FROM mes_work_report WHERE report_status = 'APPROVED'),
                      (SELECT MIN(report_id) FROM mes_work_report)) END,
    inspector_id = (SELECT user_id FROM mes_user WHERE username = 'mes_inspector'),
    assigned_to = (SELECT user_id FROM mes_user WHERE username = 'mes_inspector'),
    submitted_by = CASE WHEN inspection_status = 'CREATED' THEN NULL
        ELSE (SELECT user_id FROM mes_user WHERE username = 'mes_inspector') END,
    reviewed_by = CASE WHEN inspection_status = 'CREATED' THEN NULL
        ELSE (SELECT user_id FROM mes_user WHERE username = 'mes_quality_mgr') END;

UPDATE mes_quality_inspection_item
SET inspection_id = COALESCE(
        (SELECT MIN(inspection_id) FROM mes_quality_inspection WHERE inspection_status = 'APPROVED'),
        (SELECT MIN(inspection_id) FROM mes_quality_inspection));

UPDATE mes_rework_order
SET source_work_order_id = COALESCE(
        (SELECT MIN(work_order_id) FROM mes_work_order WHERE work_order_status = 'RECEIVED'),
        (SELECT MIN(work_order_id) FROM mes_work_order)),
    inspection_id = COALESCE(
        (SELECT MIN(inspection_id) FROM mes_quality_inspection WHERE judgement_result = 'REWORK'),
        (SELECT MIN(inspection_id) FROM mes_quality_inspection)),
    assigned_line_id = (SELECT MIN(line_id) FROM mes_production_line),
    approved_by = (SELECT user_id FROM mes_user WHERE username = 'mes_quality_mgr');

UPDATE mes_product_trace
SET order_id = (SELECT MIN(order_id) FROM mes_customer_order),
    task_id = (SELECT MIN(task_id) FROM mes_production_task),
    work_order_id = (SELECT MIN(work_order_id) FROM mes_work_order),
    product_id = (SELECT MIN(product_id) FROM mes_product);

UPDATE mes_quality_trace
SET order_id = (SELECT MIN(order_id) FROM mes_customer_order),
    task_id = (SELECT MIN(task_id) FROM mes_production_task),
    work_order_id = (SELECT MIN(work_order_id) FROM mes_work_order),
    inspection_id = (SELECT MIN(inspection_id) FROM mes_quality_inspection),
    rework_order_id = CASE WHEN trace_status = 'REWORKED'
        THEN (SELECT MIN(rework_order_id) FROM mes_rework_order) ELSE NULL END;

UPDATE mes_equipment
SET line_id = (SELECT MIN(line_id) FROM mes_production_line);

UPDATE mes_equipment_repair_report
SET equipment_id = COALESCE(
        (SELECT MIN(equipment_id) FROM mes_equipment WHERE equipment_status = 'FAULT'),
        (SELECT MIN(equipment_id) FROM mes_equipment)),
    work_order_id = (SELECT MIN(work_order_id) FROM mes_work_order),
    reporter_id = (SELECT user_id FROM mes_user WHERE username = 'mes_workshop'),
    reviewed_by = CASE WHEN repair_status <> 'REPORTED'
        THEN (SELECT user_id FROM mes_user WHERE username = 'mes_equipment_mgr') ELSE NULL END;

UPDATE mes_maintenance_order
SET repair_report_id = COALESCE(
        (SELECT MIN(repair_report_id) FROM mes_equipment_repair_report WHERE repair_status = 'CONVERTED'),
        (SELECT MIN(repair_report_id) FROM mes_equipment_repair_report)),
    equipment_id = (SELECT MIN(equipment_id) FROM mes_equipment),
    maintainer_id = (SELECT user_id FROM mes_user WHERE username = 'mes_maintainer'),
    accepted_by = CASE WHEN maintenance_status IN ('ACCEPTED', 'CLOSED')
        THEN (SELECT user_id FROM mes_user WHERE username = 'mes_equipment_mgr') ELSE NULL END,
    verified_by = CASE WHEN maintenance_status IN ('ACCEPTED', 'CLOSED')
        THEN (SELECT user_id FROM mes_user WHERE username = 'mes_equipment_mgr') ELSE NULL END;

UPDATE mes_maintenance_plan
SET equipment_id = (SELECT MIN(equipment_id) FROM mes_equipment);

UPDATE mes_management_feedback
SET related_doc_type = 'WORK_ORDER',
    related_doc_id = (SELECT MIN(work_order_id) FROM mes_work_order),
    created_by = (SELECT user_id FROM mes_user WHERE username = 'mes_pmc'),
    handled_by = CASE WHEN feedback_status = 'CLOSED'
        THEN (SELECT user_id FROM mes_user WHERE username = 'mes_general') ELSE NULL END;

UPDATE mes_quality_standard
SET product_id = (SELECT MIN(product_id) FROM mes_product),
    process_id = (SELECT MIN(process_id) FROM mes_process_route),
    created_by = (SELECT user_id FROM mes_user WHERE username = 'mes_process'),
    approved_by = (SELECT user_id FROM mes_user WHERE username = 'mes_quality_mgr');

UPDATE mes_quality_standard_item
SET standard_id = (SELECT MIN(standard_id) FROM mes_quality_standard);

-- 权限申请和系统配置中的人员引用统一到正式验收账号。
UPDATE mes_permission_apply
SET applicant_id = (SELECT user_id FROM mes_user WHERE username = 'mes_hr'),
    target_user_id = (SELECT user_id FROM mes_user WHERE username = 'mes_operator'),
    reviewer_id = CASE WHEN apply_status IN ('APPLIED', 'APPROVED', 'REJECTED')
        THEN (SELECT user_id FROM mes_user WHERE username = 'admin') ELSE NULL END;

UPDATE mes_department
SET manager_user_id = NULL
WHERE manager_user_id IS NOT NULL
  AND manager_user_id NOT IN (
      SELECT user_id FROM mes_user WHERE username IN (
          'admin', 'mes_hr', 'mes_general', 'mes_pmc', 'mes_workshop', 'mes_operator',
          'mes_warehouse', 'mes_quality_mgr', 'mes_inspector', 'mes_process',
          'mes_equipment_mgr', 'mes_maintainer'));

UPDATE mes_role_permission
SET granted_by = (SELECT user_id FROM mes_user WHERE username = 'admin');

-- 正式账号固定为 12 个角色各 1 个，删除旧维护员、只读、仓管员及其他调试账号。
DELETE FROM mes_user_line_scope;
DELETE FROM mes_user_warehouse_scope;
DELETE FROM mes_user_role;

DELETE FROM mes_user
WHERE username NOT IN (
    'admin', 'mes_hr', 'mes_general', 'mes_pmc', 'mes_workshop', 'mes_operator',
    'mes_warehouse', 'mes_quality_mgr', 'mes_inspector', 'mes_process',
    'mes_equipment_mgr', 'mes_maintainer'
);

DELETE FROM mes_role_permission
WHERE role_id IN (
    SELECT role_id FROM mes_role WHERE role_code NOT IN (
        'SYSTEM_ADMIN', 'HR_MANAGER', 'GENERAL_MANAGER', 'PMC_PLANNER',
        'WORKSHOP_MANAGER', 'PRODUCTION_OPERATOR', 'WAREHOUSE_ADMIN',
        'QUALITY_MANAGER', 'QUALITY_INSPECTOR', 'PROCESS_ENGINEER',
        'EQUIPMENT_ADMIN', 'EQUIPMENT_MAINTAINER'));

DELETE FROM mes_role_data_scope
WHERE role_id IN (
    SELECT role_id FROM mes_role WHERE role_code NOT IN (
        'SYSTEM_ADMIN', 'HR_MANAGER', 'GENERAL_MANAGER', 'PMC_PLANNER',
        'WORKSHOP_MANAGER', 'PRODUCTION_OPERATOR', 'WAREHOUSE_ADMIN',
        'QUALITY_MANAGER', 'QUALITY_INSPECTOR', 'PROCESS_ENGINEER',
        'EQUIPMENT_ADMIN', 'EQUIPMENT_MAINTAINER'));

DELETE FROM mes_role
WHERE role_code NOT IN (
    'SYSTEM_ADMIN', 'HR_MANAGER', 'GENERAL_MANAGER', 'PMC_PLANNER',
    'WORKSHOP_MANAGER', 'PRODUCTION_OPERATOR', 'WAREHOUSE_ADMIN',
    'QUALITY_MANAGER', 'QUALITY_INSPECTOR', 'PROCESS_ENGINEER',
    'EQUIPMENT_ADMIN', 'EQUIPMENT_MAINTAINER'
);

INSERT INTO mes_user_role (user_id, role_id, assigned_by, assigned_at)
SELECT u.user_id, role.role_id,
       (SELECT user_id FROM mes_user WHERE username = 'admin'), CURRENT_TIMESTAMP
FROM mes_user u
JOIN mes_role role ON role.role_code = u.role_code
ON CONFLICT (user_id, role_id) DO UPDATE
SET assigned_by = EXCLUDED.assigned_by, assigned_at = EXCLUDED.assigned_at, expires_at = NULL;

INSERT INTO mes_user_line_scope (user_id, line_id, assigned_by, assigned_at)
SELECT workshop.user_id,
       COALESCE((SELECT MIN(line_id) FROM mes_production_line WHERE line_status = 'RUNNING'),
                (SELECT MIN(line_id) FROM mes_production_line)),
       admin.user_id, CURRENT_TIMESTAMP
FROM mes_user workshop CROSS JOIN mes_user admin
WHERE workshop.username = 'mes_workshop' AND admin.username = 'admin';

INSERT INTO mes_user_warehouse_scope (user_id, warehouse_id, assigned_by, assigned_at)
SELECT warehouse_user.user_id, (SELECT MIN(warehouse_id) FROM mes_warehouse),
       admin.user_id, CURRENT_TIMESTAMP
FROM mes_user warehouse_user CROSS JOIN mes_user admin
WHERE warehouse_user.username = 'mes_warehouse' AND admin.username = 'admin';
