INSERT INTO mes_user(username, real_name, role_code, department, phone, enabled)
VALUES
('pmc001', 'PMC计划员', 'PMC_PLANNER', '计划部', '13800000001', 1),
('wh001', '仓储人员', 'WAREHOUSE_KEEPER', '仓储部', '13800000002', 1),
('op001', '生产操作工', 'WORKSHOP_OPERATOR', '生产车间', '13800000003', 1),
('qc001', '质检员', 'QUALITY_INSPECTOR', '质量部', '13800000004', 1),
('eq001', '设备管理员', 'EQUIPMENT_ADMIN', '设备部', '13800000005', 1)
ON CONFLICT (username) DO UPDATE SET
real_name = EXCLUDED.real_name,
role_code = EXCLUDED.role_code,
department = EXCLUDED.department,
phone = EXCLUDED.phone,
enabled = EXCLUDED.enabled;

INSERT INTO mes_product(product_code, product_name, product_model, specification, enabled)
VALUES
('TYRE-2055516', '乘用轮胎 205/55R16', '205/55R16', '半钢子午线轮胎', 1),
('TYRE-2256517', 'SUV轮胎 225/65R17', '225/65R17', '半钢子午线轮胎', 1),
('TYRE-1956515', '经济型轮胎 195/65R15', '195/65R15', '半钢子午线轮胎', 1)
ON CONFLICT (product_code) DO UPDATE SET
product_name = EXCLUDED.product_name,
product_model = EXCLUDED.product_model,
specification = EXCLUDED.specification,
enabled = EXCLUDED.enabled;

INSERT INTO mes_material(material_code, material_name, material_type, specification, unit, shelf_life_days, enabled)
VALUES
('MAT-NR-RSS3', '天然橡胶 RSS3', 'RAW', 'RSS3', 'kg', 365, 1),
('MAT-SBR-1502', '丁苯橡胶 SBR1502', 'RAW', 'SBR1502', 'kg', 365, 1),
('MAT-CB-N330', '炭黑 N330', 'RAW', 'N330', 'kg', 540, 1),
('MAT-STEEL-CORD', '钢丝帘线', 'RAW', '0.30mm', 'kg', 720, 1),
('MAT-CURING-BAG', '硫化胶囊', 'AUX', 'R16/R17通用', 'pcs', 180, 1)
ON CONFLICT (material_code) DO UPDATE SET
material_name = EXCLUDED.material_name,
material_type = EXCLUDED.material_type,
specification = EXCLUDED.specification,
unit = EXCLUDED.unit,
shelf_life_days = EXCLUDED.shelf_life_days,
enabled = EXCLUDED.enabled;

INSERT INTO mes_production_line(line_code, line_name, line_type, daily_capacity, line_status, enabled)
VALUES
('LINE-MIX-01', '炼胶一线', 'MIXING', 800, 'IDLE', 1),
('LINE-BLD-01', '成型一线', 'BUILDING', 600, 'IDLE', 1),
('LINE-CUR-01', '硫化一线', 'CURING', 500, 'IDLE', 1),
('LINE-FIN-01', '终检包装一线', 'FINISHING', 700, 'IDLE', 1)
ON CONFLICT (line_code) DO UPDATE SET
line_name = EXCLUDED.line_name,
line_type = EXCLUDED.line_type,
daily_capacity = EXCLUDED.daily_capacity,
line_status = EXCLUDED.line_status,
enabled = EXCLUDED.enabled;

INSERT INTO mes_warehouse(warehouse_code, warehouse_name, warehouse_type, enabled)
VALUES
('WH-RAW-01', '原材料仓', 'RAW', 1),
('WH-WIP-01', '半成品暂存仓', 'WIP', 1),
('WH-FG-01', '成品仓', 'FINISHED', 1)
ON CONFLICT (warehouse_code) DO UPDATE SET
warehouse_name = EXCLUDED.warehouse_name,
warehouse_type = EXCLUDED.warehouse_type,
enabled = EXCLUDED.enabled;

INSERT INTO mes_warehouse_location(warehouse_id, location_code, location_name, enabled)
SELECT w.warehouse_id, v.location_code, v.location_name, 1
FROM (VALUES
('WH-RAW-01', 'RAW-A01', '原材料 A01'),
('WH-RAW-01', 'RAW-A02', '原材料 A02'),
('WH-WIP-01', 'WIP-B01', '半成品 B01'),
('WH-FG-01', 'FG-C01', '成品 C01')
) AS v(warehouse_code, location_code, location_name)
JOIN mes_warehouse w ON w.warehouse_code = v.warehouse_code
WHERE NOT EXISTS (
    SELECT 1 FROM mes_warehouse_location l WHERE l.location_code = v.location_code
);

INSERT INTO mes_process_route(product_id, process_code, process_name, process_seq, standard_hours, required_equipment_type, enabled)
SELECT p.product_id, v.process_code, v.process_name, v.process_seq, v.standard_hours, v.required_equipment_type, 1
FROM (VALUES
('TYRE-2055516', 'PROC-205-MIX', '炼胶', 1, 1.50, 'MIXER'),
('TYRE-2055516', 'PROC-205-BLD', '成型', 2, 2.00, 'BUILDING_MACHINE'),
('TYRE-2055516', 'PROC-205-CUR', '硫化', 3, 2.50, 'CURING_PRESS'),
('TYRE-2256517', 'PROC-225-MIX', '炼胶', 1, 1.80, 'MIXER'),
('TYRE-2256517', 'PROC-225-BLD', '成型', 2, 2.30, 'BUILDING_MACHINE'),
('TYRE-2256517', 'PROC-225-CUR', '硫化', 3, 2.80, 'CURING_PRESS')
) AS v(product_code, process_code, process_name, process_seq, standard_hours, required_equipment_type)
JOIN mes_product p ON p.product_code = v.product_code
WHERE NOT EXISTS (
    SELECT 1 FROM mes_process_route r WHERE r.process_code = v.process_code
);

INSERT INTO mes_product_bom(product_id, material_id, usage_qty, unit, process_id, enabled)
SELECT p.product_id, m.material_id, v.usage_qty, v.unit, r.process_id, 1
FROM (VALUES
('TYRE-2055516', 'MAT-NR-RSS3', 2.6000, 'kg', 'PROC-205-MIX'),
('TYRE-2055516', 'MAT-SBR-1502', 1.3000, 'kg', 'PROC-205-MIX'),
('TYRE-2055516', 'MAT-CB-N330', 0.8500, 'kg', 'PROC-205-MIX'),
('TYRE-2055516', 'MAT-STEEL-CORD', 1.1000, 'kg', 'PROC-205-BLD'),
('TYRE-2256517', 'MAT-NR-RSS3', 3.1000, 'kg', 'PROC-225-MIX'),
('TYRE-2256517', 'MAT-SBR-1502', 1.5500, 'kg', 'PROC-225-MIX'),
('TYRE-2256517', 'MAT-CB-N330', 1.0500, 'kg', 'PROC-225-MIX'),
('TYRE-2256517', 'MAT-STEEL-CORD', 1.3500, 'kg', 'PROC-225-BLD')
) AS v(product_code, material_code, usage_qty, unit, process_code)
JOIN mes_product p ON p.product_code = v.product_code
JOIN mes_material m ON m.material_code = v.material_code
LEFT JOIN mes_process_route r ON r.process_code = v.process_code
WHERE NOT EXISTS (
    SELECT 1
    FROM mes_product_bom b
    WHERE b.product_id = p.product_id
      AND b.material_id = m.material_id
      AND COALESCE(b.process_id, 0) = COALESCE(r.process_id, 0)
);

INSERT INTO mes_inventory(material_id, warehouse_id, location_id, batch_no, available_qty, reserved_qty, frozen_qty, quality_status, last_check_time)
SELECT m.material_id, w.warehouse_id, l.location_id, v.batch_no, v.available_qty, 0, 0, 'QUALIFIED', CURRENT_TIMESTAMP
FROM (VALUES
('MAT-NR-RSS3', 'WH-RAW-01', 'RAW-A01', 'BATCH-NR-202607', 6000.0000),
('MAT-SBR-1502', 'WH-RAW-01', 'RAW-A01', 'BATCH-SBR-202607', 3800.0000),
('MAT-CB-N330', 'WH-RAW-01', 'RAW-A02', 'BATCH-CB-202607', 3000.0000),
('MAT-STEEL-CORD', 'WH-RAW-01', 'RAW-A02', 'BATCH-SC-202607', 2600.0000),
('MAT-CURING-BAG', 'WH-WIP-01', 'WIP-B01', 'BATCH-BAG-202607', 120.0000)
) AS v(material_code, warehouse_code, location_code, batch_no, available_qty)
JOIN mes_material m ON m.material_code = v.material_code
JOIN mes_warehouse w ON w.warehouse_code = v.warehouse_code
JOIN mes_warehouse_location l ON l.location_code = v.location_code
WHERE NOT EXISTS (
    SELECT 1
    FROM mes_inventory i
    WHERE i.material_id = m.material_id
      AND i.warehouse_id = w.warehouse_id
      AND i.location_id = l.location_id
      AND i.batch_no = v.batch_no
);

INSERT INTO mes_equipment(equipment_code, equipment_name, equipment_type, line_id, equipment_status, last_maintenance_time, enabled)
SELECT v.equipment_code, v.equipment_name, v.equipment_type, l.line_id, 'IDLE', CURRENT_TIMESTAMP - INTERVAL '7 days', 1
FROM (VALUES
('EQ-MIX-01', '密炼机 01', 'MIXER', 'LINE-MIX-01'),
('EQ-BLD-01', '成型机 01', 'BUILDING_MACHINE', 'LINE-BLD-01'),
('EQ-CUR-01', '硫化机 01', 'CURING_PRESS', 'LINE-CUR-01'),
('EQ-FIN-01', '动平衡检测机 01', 'INSPECTION', 'LINE-FIN-01')
) AS v(equipment_code, equipment_name, equipment_type, line_code)
LEFT JOIN mes_production_line l ON l.line_code = v.line_code
ON CONFLICT (equipment_code) DO UPDATE SET
equipment_name = EXCLUDED.equipment_name,
equipment_type = EXCLUDED.equipment_type,
line_id = EXCLUDED.line_id,
equipment_status = EXCLUDED.equipment_status,
last_maintenance_time = EXCLUDED.last_maintenance_time,
enabled = EXCLUDED.enabled;

INSERT INTO mes_customer_order(order_no, customer_name, product_id, product_code, product_model, order_qty, unit, delivery_date, priority_level, order_status, source_system, remark)
SELECT v.order_no, v.customer_name, p.product_id, p.product_code, p.product_model, v.order_qty, '条', v.delivery_date::date, v.priority_level, v.order_status, 'MES_TEST', v.remark
FROM (VALUES
('ORD-TEST-202607-001', '双星演示客户 A', 'TYRE-2055516', 300, '2026-07-25', 1, 'PENDING_PLAN', '端到端测试订单：205/55R16'),
('ORD-TEST-202607-002', '双星演示客户 B', 'TYRE-2256517', 180, '2026-07-28', 2, 'PENDING_PLAN', '端到端测试订单：225/65R17')
) AS v(order_no, customer_name, product_code, order_qty, delivery_date, priority_level, order_status, remark)
JOIN mes_product p ON p.product_code = v.product_code
ON CONFLICT (order_no) DO UPDATE SET
customer_name = EXCLUDED.customer_name,
product_id = EXCLUDED.product_id,
product_code = EXCLUDED.product_code,
product_model = EXCLUDED.product_model,
order_qty = EXCLUDED.order_qty,
delivery_date = EXCLUDED.delivery_date,
priority_level = EXCLUDED.priority_level,
order_status = EXCLUDED.order_status,
source_system = EXCLUDED.source_system,
remark = EXCLUDED.remark,
updated_at = CURRENT_TIMESTAMP;

INSERT INTO mes_dashboard_metric(metric_date, metric_type, metric_name, metric_value, metric_unit, dimension_key)
SELECT CURRENT_DATE, v.metric_type, v.metric_name, v.metric_value, v.metric_unit, v.dimension_key
FROM (VALUES
('ORDER', '待排产订单数', 2, '单', 'PENDING_PLAN'),
('INVENTORY', '原材料库存批次数', 5, '批', 'RAW'),
('LINE', '可用产线数', 4, '条', 'IDLE')
) AS v(metric_type, metric_name, metric_value, metric_unit, dimension_key)
WHERE NOT EXISTS (
    SELECT 1
    FROM mes_dashboard_metric d
    WHERE d.metric_date = CURRENT_DATE
      AND d.metric_type = v.metric_type
      AND d.metric_name = v.metric_name
      AND d.dimension_key = v.dimension_key
);
