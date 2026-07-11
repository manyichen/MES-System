-- Demo seed data for virtual warehouse delivery robots.
-- Safe to run repeatedly. Robots are virtual scheduling objects, not login users.

WITH warehouse_refs AS (
    SELECT
        COALESCE(
            (SELECT warehouse_id FROM mes_warehouse WHERE warehouse_code = 'WH-RAW-01' LIMIT 1),
            (SELECT warehouse_id FROM mes_warehouse ORDER BY warehouse_id LIMIT 1)
        ) AS raw_warehouse_id,
        COALESCE(
            (SELECT warehouse_id FROM mes_warehouse WHERE warehouse_code = 'WH-WIP-01' LIMIT 1),
            (SELECT warehouse_id FROM mes_warehouse ORDER BY warehouse_id LIMIT 1)
        ) AS wip_warehouse_id,
        COALESCE(
            (SELECT warehouse_id FROM mes_warehouse WHERE warehouse_code = 'WH-FG-01' LIMIT 1),
            (SELECT warehouse_id FROM mes_warehouse ORDER BY warehouse_id LIMIT 1)
        ) AS fg_warehouse_id
)
INSERT INTO mes_robot (
    robot_code,
    robot_name,
    warehouse_id,
    robot_status,
    battery_level,
    current_location,
    enabled
)
SELECT
    robot.robot_code,
    robot.robot_name,
    robot.warehouse_id,
    robot.robot_status,
    robot.battery_level,
    robot.current_location,
    robot.enabled
FROM warehouse_refs w
CROSS JOIN LATERAL (
    VALUES
        ('ROB-RAW-01', '智能配送机器人一号', w.raw_warehouse_id, 'IDLE', 88.00, '原材料仓待命区', 1),
        ('ROB-RAW-02', '智能配送机器人二号', w.raw_warehouse_id, 'IDLE', 76.00, '原材料仓充电区', 1),
        ('ROB-WIP-01', '半成品转运机器人', w.wip_warehouse_id, 'IDLE', 91.00, '半成品暂存仓', 1),
        ('ROB-FG-01', '成品入库机器人', w.fg_warehouse_id, 'CHARGING', 42.00, '成品仓充电区', 1),
        ('ROB-DEMO-01', '演示配送机器人', w.raw_warehouse_id, 'IDLE', 100.00, '演示待命点', 1)
) AS robot(
    robot_code,
    robot_name,
    warehouse_id,
    robot_status,
    battery_level,
    current_location,
    enabled
)
ON CONFLICT (robot_code) DO UPDATE SET
    robot_name = EXCLUDED.robot_name,
    warehouse_id = EXCLUDED.warehouse_id,
    robot_status = EXCLUDED.robot_status,
    battery_level = EXCLUDED.battery_level,
    current_location = EXCLUDED.current_location,
    enabled = EXCLUDED.enabled;
