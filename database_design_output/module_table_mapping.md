# 双星轮胎制造 MES 数据库模块与表映射

本文件用于后续代码开发时按业务模块拆分包、接口和数据访问对象。数据库设计书中的“DB一览表”按以下 15 个模块组织，覆盖系统模块图中的 22 个流程节点。

| 序号 | 数据库模块 | 对应系统模块图流程节点 | 数据表 |
|---:|---|---|---|
| 1 | 订单查看 | 流程节点1：订单查看 | mes_customer_order |
| 2 | 生产任务管理 | 流程节点2：生产任务管理 | mes_production_task |
| 3 | 齐套分析与欠料预警 | 流程节点3-4：齐套分析、欠料预警 | mes_kitting_analysis<br>mes_kitting_shortage_item<br>mes_shortage_alert |
| 4 | 生产工单创建接收派发 | 流程节点5-8：创建生产工单、接收生产工单、派发制造工单、接收制造工单 | mes_work_order<br>mes_work_order_operation_log |
| 5 | 领料任务管理 | 流程节点9：创建领料任务 | mes_material_requisition<br>mes_material_requisition_item |
| 6 | 仓库接收与库存核对 | 流程节点10：接收领料信息；支撑库存核对 | mes_material<br>mes_inventory<br>mes_warehouse<br>mes_warehouse_location<br>mes_inventory_transaction |
| 7 | 备货与拣货 | 流程节点11：备货与拣货 | mes_picking_task |
| 8 | 运输机器人配送 | 流程节点12：运输机器人配送 | mes_robot<br>mes_robot_delivery_task |
| 9 | 生产报工与计件 | 流程节点13：生产报工与计件 | mes_work_report<br>mes_piecework_wage |
| 10 | 质量抽检与结果判定 | 流程节点14-15：质量抽检、结果判定 | mes_quality_inspection<br>mes_quality_inspection_item |
| 11 | 返工与质量追溯 | 流程节点16：返工与质量追溯 | mes_rework_order<br>mes_quality_trace |
| 12 | 设备故障报修 | 流程节点17：设备故障报修 | mes_equipment<br>mes_equipment_repair_report |
| 13 | 维修工单与设备维护 | 流程节点18：维修工单与设备维护 | mes_maintenance_order<br>mes_maintenance_plan |
| 14 | 数据同步与主数据存储 | 流程节点19：数据同步与存储；支撑产品、BOM、工艺、产线、用户等主数据 | mes_product<br>mes_product_bom<br>mes_process_route<br>mes_production_line<br>mes_sync_log<br>mes_user |
| 15 | 产品追溯看板决策反馈 | 流程节点20-22：产品追溯、综合生产看板、管理决策反馈 | mes_product_trace<br>mes_dashboard_metric<br>mes_management_feedback |
