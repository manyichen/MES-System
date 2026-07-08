# 三大模块真实实体 ER 图 Mermaid 代码

说明：实体使用矩形，属性使用椭圆形并连接到实体；实体间连线标出一对一/一对多/多对多。多对多按数据库设计用中间实体实现，并额外用虚线标注概念关系。

## 生产计划与工单管理模块

```mermaid
flowchart LR
  classDef entity fill:#ffffff,stroke:#111827,stroke-width:1.5px,color:#111827;
  classDef attr fill:#ffffff,stroke:#6b7280,stroke-width:1px,color:#111827;

  plan_mes_customer_order["客户订单表<br/>mes_customer_order"]
  class plan_mes_customer_order entity;
  plan_mes_customer_order_order_id(["PK order_id<br/>订单主键"])
  plan_mes_customer_order --- plan_mes_customer_order_order_id
  class plan_mes_customer_order_order_id attr;
  plan_mes_customer_order_order_no(["UK order_no<br/>订单编号"])
  plan_mes_customer_order --- plan_mes_customer_order_order_no
  class plan_mes_customer_order_order_no attr;
  plan_mes_customer_order_customer_name(["customer_name<br/>客户名称"])
  plan_mes_customer_order --- plan_mes_customer_order_customer_name
  class plan_mes_customer_order_customer_name attr;
  plan_mes_customer_order_product_id(["FK product_id<br/>产品主键"])
  plan_mes_customer_order --- plan_mes_customer_order_product_id
  class plan_mes_customer_order_product_id attr;
  plan_mes_customer_order_product_code(["product_code<br/>产品编码快照"])
  plan_mes_customer_order --- plan_mes_customer_order_product_code
  class plan_mes_customer_order_product_code attr;
  plan_mes_customer_order_product_model(["product_model<br/>轮胎型号/规格"])
  plan_mes_customer_order --- plan_mes_customer_order_product_model
  class plan_mes_customer_order_product_model attr;
  plan_mes_customer_order_order_qty(["order_qty<br/>订单数量"])
  plan_mes_customer_order --- plan_mes_customer_order_order_qty
  class plan_mes_customer_order_order_qty attr;
  plan_mes_customer_order_unit(["unit<br/>计量单位"])
  plan_mes_customer_order --- plan_mes_customer_order_unit
  class plan_mes_customer_order_unit attr;
  plan_mes_customer_order_delivery_date(["delivery_date<br/>交付日期"])
  plan_mes_customer_order --- plan_mes_customer_order_delivery_date
  class plan_mes_customer_order_delivery_date attr;
  plan_mes_customer_order_priority_level(["priority_level<br/>优先级，1最高"])
  plan_mes_customer_order --- plan_mes_customer_order_priority_level
  class plan_mes_customer_order_priority_level attr;
  plan_mes_customer_order_order_status(["order_status<br/>订单状态"])
  plan_mes_customer_order --- plan_mes_customer_order_order_status
  class plan_mes_customer_order_order_status attr;
  plan_mes_customer_order_source_system(["source_system<br/>来源系统"])
  plan_mes_customer_order --- plan_mes_customer_order_source_system
  class plan_mes_customer_order_source_system attr;
  plan_mes_customer_order_remark(["remark<br/>备注"])
  plan_mes_customer_order --- plan_mes_customer_order_remark
  class plan_mes_customer_order_remark attr;
  plan_mes_customer_order_created_at(["created_at<br/>创建时间"])
  plan_mes_customer_order --- plan_mes_customer_order_created_at
  class plan_mes_customer_order_created_at attr;
  plan_mes_customer_order_updated_at(["updated_at<br/>更新时间"])
  plan_mes_customer_order --- plan_mes_customer_order_updated_at
  class plan_mes_customer_order_updated_at attr;

  plan_mes_production_task["生产任务表<br/>mes_production_task"]
  class plan_mes_production_task entity;
  plan_mes_production_task_task_id(["PK task_id<br/>任务主键"])
  plan_mes_production_task --- plan_mes_production_task_task_id
  class plan_mes_production_task_task_id attr;
  plan_mes_production_task_task_no(["UK task_no<br/>任务编号"])
  plan_mes_production_task --- plan_mes_production_task_task_no
  class plan_mes_production_task_task_no attr;
  plan_mes_production_task_order_id(["FK order_id<br/>来源订单"])
  plan_mes_production_task --- plan_mes_production_task_order_id
  class plan_mes_production_task_order_id attr;
  plan_mes_production_task_planner_id(["FK planner_id<br/>PMC计划员"])
  plan_mes_production_task --- plan_mes_production_task_planner_id
  class plan_mes_production_task_planner_id attr;
  plan_mes_production_task_plan_qty(["plan_qty<br/>计划数量"])
  plan_mes_production_task --- plan_mes_production_task_plan_qty
  class plan_mes_production_task_plan_qty attr;
  plan_mes_production_task_planned_start_time(["planned_start_time<br/>计划开始时间"])
  plan_mes_production_task --- plan_mes_production_task_planned_start_time
  class plan_mes_production_task_planned_start_time attr;
  plan_mes_production_task_planned_end_time(["planned_end_time<br/>计划完成时间"])
  plan_mes_production_task --- plan_mes_production_task_planned_end_time
  class plan_mes_production_task_planned_end_time attr;
  plan_mes_production_task_target_line_id(["FK target_line_id<br/>目标产线"])
  plan_mes_production_task --- plan_mes_production_task_target_line_id
  class plan_mes_production_task_target_line_id attr;
  plan_mes_production_task_task_status(["task_status<br/>任务状态"])
  plan_mes_production_task --- plan_mes_production_task_task_status
  class plan_mes_production_task_task_status attr;
  plan_mes_production_task_kitting_status(["kitting_status<br/>齐套状态"])
  plan_mes_production_task --- plan_mes_production_task_kitting_status
  class plan_mes_production_task_kitting_status attr;
  plan_mes_production_task_release_time(["release_time<br/>发布时间"])
  plan_mes_production_task --- plan_mes_production_task_release_time
  class plan_mes_production_task_release_time attr;
  plan_mes_production_task_close_time(["close_time<br/>闭环时间"])
  plan_mes_production_task --- plan_mes_production_task_close_time
  class plan_mes_production_task_close_time attr;
  plan_mes_production_task_remark(["remark<br/>备注"])
  plan_mes_production_task --- plan_mes_production_task_remark
  class plan_mes_production_task_remark attr;
  plan_mes_production_task_created_at(["created_at<br/>创建时间"])
  plan_mes_production_task --- plan_mes_production_task_created_at
  class plan_mes_production_task_created_at attr;
  plan_mes_production_task_updated_at(["updated_at<br/>更新时间"])
  plan_mes_production_task --- plan_mes_production_task_updated_at
  class plan_mes_production_task_updated_at attr;

  plan_mes_kitting_analysis["齐套分析表<br/>mes_kitting_analysis"]
  class plan_mes_kitting_analysis entity;
  plan_mes_kitting_analysis_analysis_id(["PK analysis_id<br/>分析主键"])
  plan_mes_kitting_analysis --- plan_mes_kitting_analysis_analysis_id
  class plan_mes_kitting_analysis_analysis_id attr;
  plan_mes_kitting_analysis_analysis_no(["UK analysis_no<br/>分析编号"])
  plan_mes_kitting_analysis --- plan_mes_kitting_analysis_analysis_no
  class plan_mes_kitting_analysis_analysis_no attr;
  plan_mes_kitting_analysis_task_id(["FK task_id<br/>生产任务"])
  plan_mes_kitting_analysis --- plan_mes_kitting_analysis_task_id
  class plan_mes_kitting_analysis_task_id attr;
  plan_mes_kitting_analysis_analysis_scope(["analysis_scope<br/>分析范围"])
  plan_mes_kitting_analysis --- plan_mes_kitting_analysis_analysis_scope
  class plan_mes_kitting_analysis_analysis_scope attr;
  plan_mes_kitting_analysis_result_status(["result_status<br/>分析结果"])
  plan_mes_kitting_analysis --- plan_mes_kitting_analysis_result_status
  class plan_mes_kitting_analysis_result_status attr;
  plan_mes_kitting_analysis_snapshot_time(["snapshot_time<br/>数据快照时间"])
  plan_mes_kitting_analysis --- plan_mes_kitting_analysis_snapshot_time
  class plan_mes_kitting_analysis_snapshot_time attr;
  plan_mes_kitting_analysis_material_ok(["material_ok<br/>物料是否齐套"])
  plan_mes_kitting_analysis --- plan_mes_kitting_analysis_material_ok
  class plan_mes_kitting_analysis_material_ok attr;
  plan_mes_kitting_analysis_line_ok(["line_ok<br/>产线是否可用"])
  plan_mes_kitting_analysis --- plan_mes_kitting_analysis_line_ok
  class plan_mes_kitting_analysis_line_ok attr;
  plan_mes_kitting_analysis_equipment_ok(["equipment_ok<br/>设备是否可用"])
  plan_mes_kitting_analysis --- plan_mes_kitting_analysis_equipment_ok
  class plan_mes_kitting_analysis_equipment_ok attr;
  plan_mes_kitting_analysis_process_ok(["process_ok<br/>工艺是否完整"])
  plan_mes_kitting_analysis --- plan_mes_kitting_analysis_process_ok
  class plan_mes_kitting_analysis_process_ok attr;
  plan_mes_kitting_analysis_created_by(["FK created_by<br/>分析人"])
  plan_mes_kitting_analysis --- plan_mes_kitting_analysis_created_by
  class plan_mes_kitting_analysis_created_by attr;
  plan_mes_kitting_analysis_created_at(["created_at<br/>创建时间"])
  plan_mes_kitting_analysis --- plan_mes_kitting_analysis_created_at
  class plan_mes_kitting_analysis_created_at attr;

  plan_mes_kitting_shortage_item["齐套缺口明细表<br/>mes_kitting_shortage_item"]
  class plan_mes_kitting_shortage_item entity;
  plan_mes_kitting_shortage_item_shortage_item_id(["PK shortage_item_id<br/>缺口明细主键"])
  plan_mes_kitting_shortage_item --- plan_mes_kitting_shortage_item_shortage_item_id
  class plan_mes_kitting_shortage_item_shortage_item_id attr;
  plan_mes_kitting_shortage_item_analysis_id(["FK analysis_id<br/>齐套分析"])
  plan_mes_kitting_shortage_item --- plan_mes_kitting_shortage_item_analysis_id
  class plan_mes_kitting_shortage_item_analysis_id attr;
  plan_mes_kitting_shortage_item_task_id(["FK task_id<br/>生产任务"])
  plan_mes_kitting_shortage_item --- plan_mes_kitting_shortage_item_task_id
  class plan_mes_kitting_shortage_item_task_id attr;
  plan_mes_kitting_shortage_item_shortage_type(["shortage_type<br/>缺口类型 MATERIAL/LINE/EQUIPMENT/PROCESS"])
  plan_mes_kitting_shortage_item --- plan_mes_kitting_shortage_item_shortage_type
  class plan_mes_kitting_shortage_item_shortage_type attr;
  plan_mes_kitting_shortage_item_resource_id(["resource_id<br/>资源主键"])
  plan_mes_kitting_shortage_item --- plan_mes_kitting_shortage_item_resource_id
  class plan_mes_kitting_shortage_item_resource_id attr;
  plan_mes_kitting_shortage_item_resource_code(["resource_code<br/>资源编码"])
  plan_mes_kitting_shortage_item --- plan_mes_kitting_shortage_item_resource_code
  class plan_mes_kitting_shortage_item_resource_code attr;
  plan_mes_kitting_shortage_item_resource_name(["resource_name<br/>资源名称"])
  plan_mes_kitting_shortage_item --- plan_mes_kitting_shortage_item_resource_name
  class plan_mes_kitting_shortage_item_resource_name attr;
  plan_mes_kitting_shortage_item_required_qty(["required_qty<br/>需求数量"])
  plan_mes_kitting_shortage_item --- plan_mes_kitting_shortage_item_required_qty
  class plan_mes_kitting_shortage_item_required_qty attr;
  plan_mes_kitting_shortage_item_available_qty(["available_qty<br/>可用数量"])
  plan_mes_kitting_shortage_item --- plan_mes_kitting_shortage_item_available_qty
  class plan_mes_kitting_shortage_item_available_qty attr;
  plan_mes_kitting_shortage_item_shortage_qty(["shortage_qty<br/>缺口数量"])
  plan_mes_kitting_shortage_item --- plan_mes_kitting_shortage_item_shortage_qty
  class plan_mes_kitting_shortage_item_shortage_qty attr;
  plan_mes_kitting_shortage_item_impact_desc(["impact_desc<br/>影响说明"])
  plan_mes_kitting_shortage_item --- plan_mes_kitting_shortage_item_impact_desc
  class plan_mes_kitting_shortage_item_impact_desc attr;
  plan_mes_kitting_shortage_item_suggestion(["suggestion<br/>处理建议"])
  plan_mes_kitting_shortage_item --- plan_mes_kitting_shortage_item_suggestion
  class plan_mes_kitting_shortage_item_suggestion attr;
  plan_mes_kitting_shortage_item_created_at(["created_at<br/>创建时间"])
  plan_mes_kitting_shortage_item --- plan_mes_kitting_shortage_item_created_at
  class plan_mes_kitting_shortage_item_created_at attr;

  plan_mes_shortage_alert["欠料预警表<br/>mes_shortage_alert"]
  class plan_mes_shortage_alert entity;
  plan_mes_shortage_alert_alert_id(["PK alert_id<br/>预警主键"])
  plan_mes_shortage_alert --- plan_mes_shortage_alert_alert_id
  class plan_mes_shortage_alert_alert_id attr;
  plan_mes_shortage_alert_alert_no(["UK alert_no<br/>预警编号"])
  plan_mes_shortage_alert --- plan_mes_shortage_alert_alert_no
  class plan_mes_shortage_alert_alert_no attr;
  plan_mes_shortage_alert_task_id(["FK task_id<br/>生产任务"])
  plan_mes_shortage_alert --- plan_mes_shortage_alert_task_id
  class plan_mes_shortage_alert_task_id attr;
  plan_mes_shortage_alert_analysis_id(["FK analysis_id<br/>齐套分析"])
  plan_mes_shortage_alert --- plan_mes_shortage_alert_analysis_id
  class plan_mes_shortage_alert_analysis_id attr;
  plan_mes_shortage_alert_alert_type(["alert_type<br/>预警类型"])
  plan_mes_shortage_alert --- plan_mes_shortage_alert_alert_type
  class plan_mes_shortage_alert_alert_type attr;
  plan_mes_shortage_alert_severity(["severity<br/>严重级别"])
  plan_mes_shortage_alert --- plan_mes_shortage_alert_severity
  class plan_mes_shortage_alert_severity attr;
  plan_mes_shortage_alert_alert_status(["alert_status<br/>处理状态"])
  plan_mes_shortage_alert --- plan_mes_shortage_alert_alert_status
  class plan_mes_shortage_alert_alert_status attr;
  plan_mes_shortage_alert_receiver_role(["receiver_role<br/>接收角色"])
  plan_mes_shortage_alert --- plan_mes_shortage_alert_receiver_role
  class plan_mes_shortage_alert_receiver_role attr;
  plan_mes_shortage_alert_alert_content(["alert_content<br/>预警内容"])
  plan_mes_shortage_alert --- plan_mes_shortage_alert_alert_content
  class plan_mes_shortage_alert_alert_content attr;
  plan_mes_shortage_alert_resolved_at(["resolved_at<br/>解决时间"])
  plan_mes_shortage_alert --- plan_mes_shortage_alert_resolved_at
  class plan_mes_shortage_alert_resolved_at attr;
  plan_mes_shortage_alert_created_at(["created_at<br/>创建时间"])
  plan_mes_shortage_alert --- plan_mes_shortage_alert_created_at
  class plan_mes_shortage_alert_created_at attr;

  plan_mes_work_order["生产工单表<br/>mes_work_order"]
  class plan_mes_work_order entity;
  plan_mes_work_order_work_order_id(["PK work_order_id<br/>工单主键"])
  plan_mes_work_order --- plan_mes_work_order_work_order_id
  class plan_mes_work_order_work_order_id attr;
  plan_mes_work_order_work_order_no(["UK work_order_no<br/>工单编号"])
  plan_mes_work_order --- plan_mes_work_order_work_order_no
  class plan_mes_work_order_work_order_no attr;
  plan_mes_work_order_task_id(["FK task_id<br/>生产任务"])
  plan_mes_work_order --- plan_mes_work_order_task_id
  class plan_mes_work_order_task_id attr;
  plan_mes_work_order_line_id(["FK line_id<br/>产线"])
  plan_mes_work_order --- plan_mes_work_order_line_id
  class plan_mes_work_order_line_id attr;
  plan_mes_work_order_process_id(["FK process_id<br/>工序"])
  plan_mes_work_order --- plan_mes_work_order_process_id
  class plan_mes_work_order_process_id attr;
  plan_mes_work_order_planned_qty(["planned_qty<br/>计划数量"])
  plan_mes_work_order --- plan_mes_work_order_planned_qty
  class plan_mes_work_order_planned_qty attr;
  plan_mes_work_order_actual_qty(["actual_qty<br/>实际数量"])
  plan_mes_work_order --- plan_mes_work_order_actual_qty
  class plan_mes_work_order_actual_qty attr;
  plan_mes_work_order_priority_level(["priority_level<br/>优先级"])
  plan_mes_work_order --- plan_mes_work_order_priority_level
  class plan_mes_work_order_priority_level attr;
  plan_mes_work_order_work_order_status(["work_order_status<br/>工单状态"])
  plan_mes_work_order --- plan_mes_work_order_work_order_status
  class plan_mes_work_order_work_order_status attr;
  plan_mes_work_order_dispatch_time(["dispatch_time<br/>派发时间"])
  plan_mes_work_order --- plan_mes_work_order_dispatch_time
  class plan_mes_work_order_dispatch_time attr;
  plan_mes_work_order_receive_time(["receive_time<br/>接收时间"])
  plan_mes_work_order --- plan_mes_work_order_receive_time
  class plan_mes_work_order_receive_time attr;
  plan_mes_work_order_completed_time(["completed_time<br/>完成时间"])
  plan_mes_work_order --- plan_mes_work_order_completed_time
  class plan_mes_work_order_completed_time attr;
  plan_mes_work_order_created_at(["created_at<br/>创建时间"])
  plan_mes_work_order --- plan_mes_work_order_created_at
  class plan_mes_work_order_created_at attr;
  plan_mes_work_order_updated_at(["updated_at<br/>更新时间"])
  plan_mes_work_order --- plan_mes_work_order_updated_at
  class plan_mes_work_order_updated_at attr;

  plan_mes_work_order_operation_log["工单调度操作日志表<br/>mes_work_order_operation_log"]
  class plan_mes_work_order_operation_log entity;
  plan_mes_work_order_operation_log_operation_log_id(["PK operation_log_id<br/>日志主键"])
  plan_mes_work_order_operation_log --- plan_mes_work_order_operation_log_operation_log_id
  class plan_mes_work_order_operation_log_operation_log_id attr;
  plan_mes_work_order_operation_log_work_order_id(["FK work_order_id<br/>工单"])
  plan_mes_work_order_operation_log --- plan_mes_work_order_operation_log_work_order_id
  class plan_mes_work_order_operation_log_work_order_id attr;
  plan_mes_work_order_operation_log_operation_type(["operation_type<br/>操作类型"])
  plan_mes_work_order_operation_log --- plan_mes_work_order_operation_log_operation_type
  class plan_mes_work_order_operation_log_operation_type attr;
  plan_mes_work_order_operation_log_before_status(["before_status<br/>操作前状态"])
  plan_mes_work_order_operation_log --- plan_mes_work_order_operation_log_before_status
  class plan_mes_work_order_operation_log_before_status attr;
  plan_mes_work_order_operation_log_after_status(["after_status<br/>操作后状态"])
  plan_mes_work_order_operation_log --- plan_mes_work_order_operation_log_after_status
  class plan_mes_work_order_operation_log_after_status attr;
  plan_mes_work_order_operation_log_operator_id(["FK operator_id<br/>操作人"])
  plan_mes_work_order_operation_log --- plan_mes_work_order_operation_log_operator_id
  class plan_mes_work_order_operation_log_operator_id attr;
  plan_mes_work_order_operation_log_operation_reason(["operation_reason<br/>操作原因"])
  plan_mes_work_order_operation_log --- plan_mes_work_order_operation_log_operation_reason
  class plan_mes_work_order_operation_log_operation_reason attr;
  plan_mes_work_order_operation_log_operation_time(["operation_time<br/>操作时间"])
  plan_mes_work_order_operation_log --- plan_mes_work_order_operation_log_operation_time
  class plan_mes_work_order_operation_log_operation_time attr;
  plan_mes_work_order_operation_log_undoable(["undoable<br/>是否可撤销"])
  plan_mes_work_order_operation_log --- plan_mes_work_order_operation_log_undoable
  class plan_mes_work_order_operation_log_undoable attr;

  plan_mes_product["产品主数据表<br/>mes_product"]
  class plan_mes_product entity;
  plan_mes_product_product_id(["PK product_id<br/>产品主键"])
  plan_mes_product --- plan_mes_product_product_id
  class plan_mes_product_product_id attr;
  plan_mes_product_product_code(["UK product_code<br/>产品编码"])
  plan_mes_product --- plan_mes_product_product_code
  class plan_mes_product_product_code attr;
  plan_mes_product_product_name(["product_name<br/>产品名称"])
  plan_mes_product --- plan_mes_product_product_name
  class plan_mes_product_product_name attr;
  plan_mes_product_product_model(["product_model<br/>产品型号"])
  plan_mes_product --- plan_mes_product_product_model
  class plan_mes_product_product_model attr;
  plan_mes_product_specification(["specification<br/>规格"])
  plan_mes_product --- plan_mes_product_specification
  class plan_mes_product_specification attr;
  plan_mes_product_enabled(["enabled<br/>是否启用"])
  plan_mes_product --- plan_mes_product_enabled
  class plan_mes_product_enabled attr;

  plan_mes_material["物料主数据表<br/>mes_material"]
  class plan_mes_material entity;
  plan_mes_material_material_id(["PK material_id<br/>物料主键"])
  plan_mes_material --- plan_mes_material_material_id
  class plan_mes_material_material_id attr;
  plan_mes_material_material_code(["UK material_code<br/>物料编码"])
  plan_mes_material --- plan_mes_material_material_code
  class plan_mes_material_material_code attr;
  plan_mes_material_material_name(["material_name<br/>物料名称"])
  plan_mes_material --- plan_mes_material_material_name
  class plan_mes_material_material_name attr;
  plan_mes_material_material_type(["material_type<br/>物料类型"])
  plan_mes_material --- plan_mes_material_material_type
  class plan_mes_material_material_type attr;
  plan_mes_material_specification(["specification<br/>规格"])
  plan_mes_material --- plan_mes_material_specification
  class plan_mes_material_specification attr;
  plan_mes_material_unit(["unit<br/>单位"])
  plan_mes_material --- plan_mes_material_unit
  class plan_mes_material_unit attr;
  plan_mes_material_shelf_life_days(["shelf_life_days<br/>保质期天数"])
  plan_mes_material --- plan_mes_material_shelf_life_days
  class plan_mes_material_shelf_life_days attr;
  plan_mes_material_enabled(["enabled<br/>是否启用"])
  plan_mes_material --- plan_mes_material_enabled
  class plan_mes_material_enabled attr;
  plan_mes_material_created_at(["created_at<br/>创建时间"])
  plan_mes_material --- plan_mes_material_created_at
  class plan_mes_material_created_at attr;

  plan_mes_product_bom["产品BOM表<br/>mes_product_bom"]
  class plan_mes_product_bom entity;
  plan_mes_product_bom_bom_id(["PK bom_id<br/>BOM主键"])
  plan_mes_product_bom --- plan_mes_product_bom_bom_id
  class plan_mes_product_bom_bom_id attr;
  plan_mes_product_bom_product_id(["FK product_id<br/>产品"])
  plan_mes_product_bom --- plan_mes_product_bom_product_id
  class plan_mes_product_bom_product_id attr;
  plan_mes_product_bom_material_id(["FK material_id<br/>物料"])
  plan_mes_product_bom --- plan_mes_product_bom_material_id
  class plan_mes_product_bom_material_id attr;
  plan_mes_product_bom_usage_qty(["usage_qty<br/>单位用量"])
  plan_mes_product_bom --- plan_mes_product_bom_usage_qty
  class plan_mes_product_bom_usage_qty attr;
  plan_mes_product_bom_unit(["unit<br/>单位"])
  plan_mes_product_bom --- plan_mes_product_bom_unit
  class plan_mes_product_bom_unit attr;
  plan_mes_product_bom_process_id(["FK process_id<br/>使用工序"])
  plan_mes_product_bom --- plan_mes_product_bom_process_id
  class plan_mes_product_bom_process_id attr;
  plan_mes_product_bom_enabled(["enabled<br/>是否启用"])
  plan_mes_product_bom --- plan_mes_product_bom_enabled
  class plan_mes_product_bom_enabled attr;

  plan_mes_process_route["工艺路线表<br/>mes_process_route"]
  class plan_mes_process_route entity;
  plan_mes_process_route_process_id(["PK process_id<br/>工序主键"])
  plan_mes_process_route --- plan_mes_process_route_process_id
  class plan_mes_process_route_process_id attr;
  plan_mes_process_route_product_id(["FK product_id<br/>产品"])
  plan_mes_process_route --- plan_mes_process_route_product_id
  class plan_mes_process_route_product_id attr;
  plan_mes_process_route_process_code(["process_code<br/>工序编码"])
  plan_mes_process_route --- plan_mes_process_route_process_code
  class plan_mes_process_route_process_code attr;
  plan_mes_process_route_process_name(["process_name<br/>工序名称"])
  plan_mes_process_route --- plan_mes_process_route_process_name
  class plan_mes_process_route_process_name attr;
  plan_mes_process_route_process_seq(["process_seq<br/>工序顺序"])
  plan_mes_process_route --- plan_mes_process_route_process_seq
  class plan_mes_process_route_process_seq attr;
  plan_mes_process_route_standard_hours(["standard_hours<br/>标准工时"])
  plan_mes_process_route --- plan_mes_process_route_standard_hours
  class plan_mes_process_route_standard_hours attr;
  plan_mes_process_route_required_equipment_type(["required_equipment_type<br/>所需设备类型"])
  plan_mes_process_route --- plan_mes_process_route_required_equipment_type
  class plan_mes_process_route_required_equipment_type attr;
  plan_mes_process_route_enabled(["enabled<br/>是否启用"])
  plan_mes_process_route --- plan_mes_process_route_enabled
  class plan_mes_process_route_enabled attr;

  plan_mes_production_line["生产产线表<br/>mes_production_line"]
  class plan_mes_production_line entity;
  plan_mes_production_line_line_id(["PK line_id<br/>产线主键"])
  plan_mes_production_line --- plan_mes_production_line_line_id
  class plan_mes_production_line_line_id attr;
  plan_mes_production_line_line_code(["UK line_code<br/>产线编码"])
  plan_mes_production_line --- plan_mes_production_line_line_code
  class plan_mes_production_line_line_code attr;
  plan_mes_production_line_line_name(["line_name<br/>产线名称"])
  plan_mes_production_line --- plan_mes_production_line_line_name
  class plan_mes_production_line_line_name attr;
  plan_mes_production_line_line_type(["line_type<br/>产线类型"])
  plan_mes_production_line --- plan_mes_production_line_line_type
  class plan_mes_production_line_line_type attr;
  plan_mes_production_line_daily_capacity(["daily_capacity<br/>日产能"])
  plan_mes_production_line --- plan_mes_production_line_daily_capacity
  class plan_mes_production_line_daily_capacity attr;
  plan_mes_production_line_line_status(["line_status<br/>产线状态"])
  plan_mes_production_line --- plan_mes_production_line_line_status
  class plan_mes_production_line_line_status attr;
  plan_mes_production_line_enabled(["enabled<br/>是否启用"])
  plan_mes_production_line --- plan_mes_production_line_enabled
  class plan_mes_production_line_enabled attr;

  plan_mes_user["用户表<br/>mes_user"]
  class plan_mes_user entity;
  plan_mes_user_user_id(["PK user_id<br/>用户主键"])
  plan_mes_user --- plan_mes_user_user_id
  class plan_mes_user_user_id attr;
  plan_mes_user_username(["UK username<br/>登录名"])
  plan_mes_user --- plan_mes_user_username
  class plan_mes_user_username attr;
  plan_mes_user_real_name(["real_name<br/>姓名"])
  plan_mes_user --- plan_mes_user_real_name
  class plan_mes_user_real_name attr;
  plan_mes_user_role_code(["role_code<br/>角色编码"])
  plan_mes_user --- plan_mes_user_role_code
  class plan_mes_user_role_code attr;
  plan_mes_user_department(["department<br/>部门"])
  plan_mes_user --- plan_mes_user_department
  class plan_mes_user_department attr;
  plan_mes_user_phone(["phone<br/>电话"])
  plan_mes_user --- plan_mes_user_phone
  class plan_mes_user_phone attr;
  plan_mes_user_enabled(["enabled<br/>是否启用"])
  plan_mes_user --- plan_mes_user_enabled
  class plan_mes_user_enabled attr;
  plan_mes_user_created_at(["created_at<br/>创建时间"])
  plan_mes_user --- plan_mes_user_created_at
  class plan_mes_user_created_at attr;

  %% Entity relationships: parent entity --> child entity
  plan_mes_product -->|"一对多 - 产品主键"| plan_mes_customer_order
  plan_mes_customer_order -->|"一对多 - 来源订单"| plan_mes_production_task
  plan_mes_user -->|"一对多 - PMC计划员"| plan_mes_production_task
  plan_mes_production_line -->|"一对多 - 目标产线"| plan_mes_production_task
  plan_mes_production_task -->|"一对多 - 生产任务"| plan_mes_kitting_analysis
  plan_mes_user -->|"一对多 - 分析人"| plan_mes_kitting_analysis
  plan_mes_kitting_analysis -->|"一对多 - 齐套分析"| plan_mes_kitting_shortage_item
  plan_mes_production_task -->|"一对多 - 生产任务"| plan_mes_kitting_shortage_item
  plan_mes_production_task -->|"一对多 - 生产任务"| plan_mes_shortage_alert
  plan_mes_kitting_analysis -->|"一对多 - 齐套分析"| plan_mes_shortage_alert
  plan_mes_production_task -->|"一对多 - 生产任务"| plan_mes_work_order
  plan_mes_production_line -->|"一对多 - 产线"| plan_mes_work_order
  plan_mes_process_route -->|"一对多 - 工序"| plan_mes_work_order
  plan_mes_work_order -->|"一对多 - 工单"| plan_mes_work_order_operation_log
  plan_mes_user -->|"一对多 - 操作人"| plan_mes_work_order_operation_log
  plan_mes_product -->|"一对多 - 产品"| plan_mes_product_bom
  plan_mes_material -->|"一对多 - 物料"| plan_mes_product_bom
  plan_mes_process_route -->|"一对多 - 使用工序"| plan_mes_product_bom
  plan_mes_product -->|"一对多 - 产品"| plan_mes_process_route

  %% Conceptual M:N / 1:1 business relationships
  plan_mes_product -.->|"多对多 - 通过产品BOM"| plan_mes_material
```

## 生产执行与仓储物流模块

```mermaid
flowchart LR
  classDef entity fill:#ffffff,stroke:#111827,stroke-width:1.5px,color:#111827;
  classDef attr fill:#ffffff,stroke:#6b7280,stroke-width:1px,color:#111827;

  exec_mes_material_requisition["领料任务表<br/>mes_material_requisition"]
  class exec_mes_material_requisition entity;
  exec_mes_material_requisition_requisition_id(["PK requisition_id<br/>领料任务主键"])
  exec_mes_material_requisition --- exec_mes_material_requisition_requisition_id
  class exec_mes_material_requisition_requisition_id attr;
  exec_mes_material_requisition_requisition_no(["UK requisition_no<br/>领料单号"])
  exec_mes_material_requisition --- exec_mes_material_requisition_requisition_no
  class exec_mes_material_requisition_requisition_no attr;
  exec_mes_material_requisition_work_order_id(["FK work_order_id<br/>生产工单"])
  exec_mes_material_requisition --- exec_mes_material_requisition_work_order_id
  class exec_mes_material_requisition_work_order_id attr;
  exec_mes_material_requisition_requested_by(["FK requested_by<br/>申请人"])
  exec_mes_material_requisition --- exec_mes_material_requisition_requested_by
  class exec_mes_material_requisition_requested_by attr;
  exec_mes_material_requisition_request_status(["request_status<br/>领料状态"])
  exec_mes_material_requisition --- exec_mes_material_requisition_request_status
  class exec_mes_material_requisition_request_status attr;
  exec_mes_material_requisition_request_time(["request_time<br/>申请时间"])
  exec_mes_material_requisition --- exec_mes_material_requisition_request_time
  class exec_mes_material_requisition_request_time attr;
  exec_mes_material_requisition_approved_by(["FK approved_by<br/>审批人"])
  exec_mes_material_requisition --- exec_mes_material_requisition_approved_by
  class exec_mes_material_requisition_approved_by attr;
  exec_mes_material_requisition_approved_time(["approved_time<br/>审批时间"])
  exec_mes_material_requisition --- exec_mes_material_requisition_approved_time
  class exec_mes_material_requisition_approved_time attr;
  exec_mes_material_requisition_remark(["remark<br/>备注"])
  exec_mes_material_requisition --- exec_mes_material_requisition_remark
  class exec_mes_material_requisition_remark attr;

  exec_mes_material_requisition_item["领料任务明细表<br/>mes_material_requisition_item"]
  class exec_mes_material_requisition_item entity;
  exec_mes_material_requisition_item_requisition_item_id(["PK requisition_item_id<br/>明细主键"])
  exec_mes_material_requisition_item --- exec_mes_material_requisition_item_requisition_item_id
  class exec_mes_material_requisition_item_requisition_item_id attr;
  exec_mes_material_requisition_item_requisition_id(["FK requisition_id<br/>领料任务"])
  exec_mes_material_requisition_item --- exec_mes_material_requisition_item_requisition_id
  class exec_mes_material_requisition_item_requisition_id attr;
  exec_mes_material_requisition_item_material_id(["FK material_id<br/>物料"])
  exec_mes_material_requisition_item --- exec_mes_material_requisition_item_material_id
  class exec_mes_material_requisition_item_material_id attr;
  exec_mes_material_requisition_item_required_qty(["required_qty<br/>需求数量"])
  exec_mes_material_requisition_item --- exec_mes_material_requisition_item_required_qty
  class exec_mes_material_requisition_item_required_qty attr;
  exec_mes_material_requisition_item_issued_qty(["issued_qty<br/>已发数量"])
  exec_mes_material_requisition_item --- exec_mes_material_requisition_item_issued_qty
  class exec_mes_material_requisition_item_issued_qty attr;
  exec_mes_material_requisition_item_unit(["unit<br/>单位"])
  exec_mes_material_requisition_item --- exec_mes_material_requisition_item_unit
  class exec_mes_material_requisition_item_unit attr;
  exec_mes_material_requisition_item_batch_no(["batch_no<br/>指定批次"])
  exec_mes_material_requisition_item --- exec_mes_material_requisition_item_batch_no
  class exec_mes_material_requisition_item_batch_no attr;
  exec_mes_material_requisition_item_item_status(["item_status<br/>明细状态"])
  exec_mes_material_requisition_item --- exec_mes_material_requisition_item_item_status
  class exec_mes_material_requisition_item_item_status attr;

  exec_mes_material["物料主数据表<br/>mes_material"]
  class exec_mes_material entity;
  exec_mes_material_material_id(["PK material_id<br/>物料主键"])
  exec_mes_material --- exec_mes_material_material_id
  class exec_mes_material_material_id attr;
  exec_mes_material_material_code(["UK material_code<br/>物料编码"])
  exec_mes_material --- exec_mes_material_material_code
  class exec_mes_material_material_code attr;
  exec_mes_material_material_name(["material_name<br/>物料名称"])
  exec_mes_material --- exec_mes_material_material_name
  class exec_mes_material_material_name attr;
  exec_mes_material_material_type(["material_type<br/>物料类型"])
  exec_mes_material --- exec_mes_material_material_type
  class exec_mes_material_material_type attr;
  exec_mes_material_specification(["specification<br/>规格"])
  exec_mes_material --- exec_mes_material_specification
  class exec_mes_material_specification attr;
  exec_mes_material_unit(["unit<br/>单位"])
  exec_mes_material --- exec_mes_material_unit
  class exec_mes_material_unit attr;
  exec_mes_material_shelf_life_days(["shelf_life_days<br/>保质期天数"])
  exec_mes_material --- exec_mes_material_shelf_life_days
  class exec_mes_material_shelf_life_days attr;
  exec_mes_material_enabled(["enabled<br/>是否启用"])
  exec_mes_material --- exec_mes_material_enabled
  class exec_mes_material_enabled attr;
  exec_mes_material_created_at(["created_at<br/>创建时间"])
  exec_mes_material --- exec_mes_material_created_at
  class exec_mes_material_created_at attr;

  exec_mes_inventory["库存表<br/>mes_inventory"]
  class exec_mes_inventory entity;
  exec_mes_inventory_inventory_id(["PK inventory_id<br/>库存主键"])
  exec_mes_inventory --- exec_mes_inventory_inventory_id
  class exec_mes_inventory_inventory_id attr;
  exec_mes_inventory_material_id(["FK material_id<br/>物料"])
  exec_mes_inventory --- exec_mes_inventory_material_id
  class exec_mes_inventory_material_id attr;
  exec_mes_inventory_warehouse_id(["FK warehouse_id<br/>仓库"])
  exec_mes_inventory --- exec_mes_inventory_warehouse_id
  class exec_mes_inventory_warehouse_id attr;
  exec_mes_inventory_location_id(["FK location_id<br/>库位"])
  exec_mes_inventory --- exec_mes_inventory_location_id
  class exec_mes_inventory_location_id attr;
  exec_mes_inventory_batch_no(["batch_no<br/>批次号"])
  exec_mes_inventory --- exec_mes_inventory_batch_no
  class exec_mes_inventory_batch_no attr;
  exec_mes_inventory_available_qty(["available_qty<br/>可用数量"])
  exec_mes_inventory --- exec_mes_inventory_available_qty
  class exec_mes_inventory_available_qty attr;
  exec_mes_inventory_reserved_qty(["reserved_qty<br/>占用数量"])
  exec_mes_inventory --- exec_mes_inventory_reserved_qty
  class exec_mes_inventory_reserved_qty attr;
  exec_mes_inventory_frozen_qty(["frozen_qty<br/>冻结数量"])
  exec_mes_inventory --- exec_mes_inventory_frozen_qty
  class exec_mes_inventory_frozen_qty attr;
  exec_mes_inventory_quality_status(["quality_status<br/>质量状态"])
  exec_mes_inventory --- exec_mes_inventory_quality_status
  class exec_mes_inventory_quality_status attr;
  exec_mes_inventory_last_check_time(["last_check_time<br/>最近核对时间"])
  exec_mes_inventory --- exec_mes_inventory_last_check_time
  class exec_mes_inventory_last_check_time attr;

  exec_mes_warehouse["仓库表<br/>mes_warehouse"]
  class exec_mes_warehouse entity;
  exec_mes_warehouse_warehouse_id(["PK warehouse_id<br/>仓库主键"])
  exec_mes_warehouse --- exec_mes_warehouse_warehouse_id
  class exec_mes_warehouse_warehouse_id attr;
  exec_mes_warehouse_warehouse_code(["UK warehouse_code<br/>仓库编码"])
  exec_mes_warehouse --- exec_mes_warehouse_warehouse_code
  class exec_mes_warehouse_warehouse_code attr;
  exec_mes_warehouse_warehouse_name(["warehouse_name<br/>仓库名称"])
  exec_mes_warehouse --- exec_mes_warehouse_warehouse_name
  class exec_mes_warehouse_warehouse_name attr;
  exec_mes_warehouse_warehouse_type(["warehouse_type<br/>仓库类型"])
  exec_mes_warehouse --- exec_mes_warehouse_warehouse_type
  class exec_mes_warehouse_warehouse_type attr;
  exec_mes_warehouse_enabled(["enabled<br/>是否启用"])
  exec_mes_warehouse --- exec_mes_warehouse_enabled
  class exec_mes_warehouse_enabled attr;

  exec_mes_warehouse_location["库位表<br/>mes_warehouse_location"]
  class exec_mes_warehouse_location entity;
  exec_mes_warehouse_location_location_id(["PK location_id<br/>库位主键"])
  exec_mes_warehouse_location --- exec_mes_warehouse_location_location_id
  class exec_mes_warehouse_location_location_id attr;
  exec_mes_warehouse_location_warehouse_id(["FK warehouse_id<br/>仓库"])
  exec_mes_warehouse_location --- exec_mes_warehouse_location_warehouse_id
  class exec_mes_warehouse_location_warehouse_id attr;
  exec_mes_warehouse_location_location_code(["UK location_code<br/>库位编码"])
  exec_mes_warehouse_location --- exec_mes_warehouse_location_location_code
  class exec_mes_warehouse_location_location_code attr;
  exec_mes_warehouse_location_location_name(["location_name<br/>库位名称"])
  exec_mes_warehouse_location --- exec_mes_warehouse_location_location_name
  class exec_mes_warehouse_location_location_name attr;
  exec_mes_warehouse_location_enabled(["enabled<br/>是否启用"])
  exec_mes_warehouse_location --- exec_mes_warehouse_location_enabled
  class exec_mes_warehouse_location_enabled attr;

  exec_mes_inventory_transaction["库存流水表<br/>mes_inventory_transaction"]
  class exec_mes_inventory_transaction entity;
  exec_mes_inventory_transaction_transaction_id(["PK transaction_id<br/>流水主键"])
  exec_mes_inventory_transaction --- exec_mes_inventory_transaction_transaction_id
  class exec_mes_inventory_transaction_transaction_id attr;
  exec_mes_inventory_transaction_transaction_no(["UK transaction_no<br/>流水编号"])
  exec_mes_inventory_transaction --- exec_mes_inventory_transaction_transaction_no
  class exec_mes_inventory_transaction_transaction_no attr;
  exec_mes_inventory_transaction_material_id(["FK material_id<br/>物料"])
  exec_mes_inventory_transaction --- exec_mes_inventory_transaction_material_id
  class exec_mes_inventory_transaction_material_id attr;
  exec_mes_inventory_transaction_inventory_id(["FK inventory_id<br/>库存"])
  exec_mes_inventory_transaction --- exec_mes_inventory_transaction_inventory_id
  class exec_mes_inventory_transaction_inventory_id attr;
  exec_mes_inventory_transaction_transaction_type(["transaction_type<br/>变动类型"])
  exec_mes_inventory_transaction --- exec_mes_inventory_transaction_transaction_type
  class exec_mes_inventory_transaction_transaction_type attr;
  exec_mes_inventory_transaction_qty(["qty<br/>变动数量"])
  exec_mes_inventory_transaction --- exec_mes_inventory_transaction_qty
  class exec_mes_inventory_transaction_qty attr;
  exec_mes_inventory_transaction_source_doc_type(["source_doc_type<br/>来源单据类型"])
  exec_mes_inventory_transaction --- exec_mes_inventory_transaction_source_doc_type
  class exec_mes_inventory_transaction_source_doc_type attr;
  exec_mes_inventory_transaction_source_doc_id(["source_doc_id<br/>来源单据ID"])
  exec_mes_inventory_transaction --- exec_mes_inventory_transaction_source_doc_id
  class exec_mes_inventory_transaction_source_doc_id attr;
  exec_mes_inventory_transaction_operator_id(["FK operator_id<br/>操作人"])
  exec_mes_inventory_transaction --- exec_mes_inventory_transaction_operator_id
  class exec_mes_inventory_transaction_operator_id attr;
  exec_mes_inventory_transaction_created_at(["created_at<br/>创建时间"])
  exec_mes_inventory_transaction --- exec_mes_inventory_transaction_created_at
  class exec_mes_inventory_transaction_created_at attr;

  exec_mes_picking_task["备货拣货任务表<br/>mes_picking_task"]
  class exec_mes_picking_task entity;
  exec_mes_picking_task_picking_task_id(["PK picking_task_id<br/>拣货任务主键"])
  exec_mes_picking_task --- exec_mes_picking_task_picking_task_id
  class exec_mes_picking_task_picking_task_id attr;
  exec_mes_picking_task_picking_task_no(["UK picking_task_no<br/>拣货任务号"])
  exec_mes_picking_task --- exec_mes_picking_task_picking_task_no
  class exec_mes_picking_task_picking_task_no attr;
  exec_mes_picking_task_requisition_id(["FK requisition_id<br/>领料任务"])
  exec_mes_picking_task --- exec_mes_picking_task_requisition_id
  class exec_mes_picking_task_requisition_id attr;
  exec_mes_picking_task_warehouse_id(["FK warehouse_id<br/>仓库"])
  exec_mes_picking_task --- exec_mes_picking_task_warehouse_id
  class exec_mes_picking_task_warehouse_id attr;
  exec_mes_picking_task_task_status(["task_status<br/>任务状态"])
  exec_mes_picking_task --- exec_mes_picking_task_task_status
  class exec_mes_picking_task_task_status attr;
  exec_mes_picking_task_assigned_to(["FK assigned_to<br/>拣货人"])
  exec_mes_picking_task --- exec_mes_picking_task_assigned_to
  class exec_mes_picking_task_assigned_to attr;
  exec_mes_picking_task_start_time(["start_time<br/>开始时间"])
  exec_mes_picking_task --- exec_mes_picking_task_start_time
  class exec_mes_picking_task_start_time attr;
  exec_mes_picking_task_finish_time(["finish_time<br/>完成时间"])
  exec_mes_picking_task --- exec_mes_picking_task_finish_time
  class exec_mes_picking_task_finish_time attr;

  exec_mes_robot["运输机器人表<br/>mes_robot"]
  class exec_mes_robot entity;
  exec_mes_robot_robot_id(["PK robot_id<br/>机器人主键"])
  exec_mes_robot --- exec_mes_robot_robot_id
  class exec_mes_robot_robot_id attr;
  exec_mes_robot_robot_code(["UK robot_code<br/>机器人编码"])
  exec_mes_robot --- exec_mes_robot_robot_code
  class exec_mes_robot_robot_code attr;
  exec_mes_robot_robot_name(["robot_name<br/>机器人名称"])
  exec_mes_robot --- exec_mes_robot_robot_name
  class exec_mes_robot_robot_name attr;
  exec_mes_robot_robot_status(["robot_status<br/>机器人状态"])
  exec_mes_robot --- exec_mes_robot_robot_status
  class exec_mes_robot_robot_status attr;
  exec_mes_robot_battery_level(["battery_level<br/>电量百分比"])
  exec_mes_robot --- exec_mes_robot_battery_level
  class exec_mes_robot_battery_level attr;
  exec_mes_robot_current_location(["current_location<br/>当前位置"])
  exec_mes_robot --- exec_mes_robot_current_location
  class exec_mes_robot_current_location attr;
  exec_mes_robot_enabled(["enabled<br/>是否启用"])
  exec_mes_robot --- exec_mes_robot_enabled
  class exec_mes_robot_enabled attr;

  exec_mes_robot_delivery_task["机器人配送任务表<br/>mes_robot_delivery_task"]
  class exec_mes_robot_delivery_task entity;
  exec_mes_robot_delivery_task_delivery_task_id(["PK delivery_task_id<br/>配送任务主键"])
  exec_mes_robot_delivery_task --- exec_mes_robot_delivery_task_delivery_task_id
  class exec_mes_robot_delivery_task_delivery_task_id attr;
  exec_mes_robot_delivery_task_delivery_task_no(["UK delivery_task_no<br/>配送任务号"])
  exec_mes_robot_delivery_task --- exec_mes_robot_delivery_task_delivery_task_no
  class exec_mes_robot_delivery_task_delivery_task_no attr;
  exec_mes_robot_delivery_task_picking_task_id(["FK picking_task_id<br/>拣货任务"])
  exec_mes_robot_delivery_task --- exec_mes_robot_delivery_task_picking_task_id
  class exec_mes_robot_delivery_task_picking_task_id attr;
  exec_mes_robot_delivery_task_robot_id(["FK robot_id<br/>机器人"])
  exec_mes_robot_delivery_task --- exec_mes_robot_delivery_task_robot_id
  class exec_mes_robot_delivery_task_robot_id attr;
  exec_mes_robot_delivery_task_from_location_id(["FK from_location_id<br/>起点库位"])
  exec_mes_robot_delivery_task --- exec_mes_robot_delivery_task_from_location_id
  class exec_mes_robot_delivery_task_from_location_id attr;
  exec_mes_robot_delivery_task_to_line_id(["FK to_line_id<br/>目标产线"])
  exec_mes_robot_delivery_task --- exec_mes_robot_delivery_task_to_line_id
  class exec_mes_robot_delivery_task_to_line_id attr;
  exec_mes_robot_delivery_task_delivery_status(["delivery_status<br/>配送状态"])
  exec_mes_robot_delivery_task --- exec_mes_robot_delivery_task_delivery_status
  class exec_mes_robot_delivery_task_delivery_status attr;
  exec_mes_robot_delivery_task_load_time(["load_time<br/>装载时间"])
  exec_mes_robot_delivery_task --- exec_mes_robot_delivery_task_load_time
  class exec_mes_robot_delivery_task_load_time attr;
  exec_mes_robot_delivery_task_handover_time(["handover_time<br/>交接时间"])
  exec_mes_robot_delivery_task --- exec_mes_robot_delivery_task_handover_time
  class exec_mes_robot_delivery_task_handover_time attr;

  exec_mes_work_report["生产报工表<br/>mes_work_report"]
  class exec_mes_work_report entity;
  exec_mes_work_report_report_id(["PK report_id<br/>报工主键"])
  exec_mes_work_report --- exec_mes_work_report_report_id
  class exec_mes_work_report_report_id attr;
  exec_mes_work_report_report_no(["UK report_no<br/>报工编号"])
  exec_mes_work_report --- exec_mes_work_report_report_no
  class exec_mes_work_report_report_no attr;
  exec_mes_work_report_work_order_id(["FK work_order_id<br/>工单"])
  exec_mes_work_report --- exec_mes_work_report_work_order_id
  class exec_mes_work_report_work_order_id attr;
  exec_mes_work_report_operator_id(["FK operator_id<br/>操作工"])
  exec_mes_work_report --- exec_mes_work_report_operator_id
  class exec_mes_work_report_operator_id attr;
  exec_mes_work_report_report_qty(["report_qty<br/>报工数量"])
  exec_mes_work_report --- exec_mes_work_report_report_qty
  class exec_mes_work_report_report_qty attr;
  exec_mes_work_report_qualified_qty(["qualified_qty<br/>合格数量"])
  exec_mes_work_report --- exec_mes_work_report_qualified_qty
  class exec_mes_work_report_qualified_qty attr;
  exec_mes_work_report_defect_qty(["defect_qty<br/>不合格数量"])
  exec_mes_work_report --- exec_mes_work_report_defect_qty
  class exec_mes_work_report_defect_qty attr;
  exec_mes_work_report_work_hours(["work_hours<br/>工时"])
  exec_mes_work_report --- exec_mes_work_report_work_hours
  class exec_mes_work_report_work_hours attr;
  exec_mes_work_report_report_time(["report_time<br/>报工时间"])
  exec_mes_work_report --- exec_mes_work_report_report_time
  class exec_mes_work_report_report_time attr;
  exec_mes_work_report_report_status(["report_status<br/>报工状态"])
  exec_mes_work_report --- exec_mes_work_report_report_status
  class exec_mes_work_report_report_status attr;

  exec_mes_piecework_wage["计件工资表<br/>mes_piecework_wage"]
  class exec_mes_piecework_wage entity;
  exec_mes_piecework_wage_wage_id(["PK wage_id<br/>计件工资主键"])
  exec_mes_piecework_wage --- exec_mes_piecework_wage_wage_id
  class exec_mes_piecework_wage_wage_id attr;
  exec_mes_piecework_wage_report_id(["FK report_id<br/>报工"])
  exec_mes_piecework_wage --- exec_mes_piecework_wage_report_id
  class exec_mes_piecework_wage_report_id attr;
  exec_mes_piecework_wage_operator_id(["FK operator_id<br/>操作工"])
  exec_mes_piecework_wage --- exec_mes_piecework_wage_operator_id
  class exec_mes_piecework_wage_operator_id attr;
  exec_mes_piecework_wage_piece_rate(["piece_rate<br/>单件费率"])
  exec_mes_piecework_wage --- exec_mes_piecework_wage_piece_rate
  class exec_mes_piecework_wage_piece_rate attr;
  exec_mes_piecework_wage_qualified_qty(["qualified_qty<br/>合格数量"])
  exec_mes_piecework_wage --- exec_mes_piecework_wage_qualified_qty
  class exec_mes_piecework_wage_qualified_qty attr;
  exec_mes_piecework_wage_wage_amount(["wage_amount<br/>工资金额"])
  exec_mes_piecework_wage --- exec_mes_piecework_wage_wage_amount
  class exec_mes_piecework_wage_wage_amount attr;
  exec_mes_piecework_wage_settlement_status(["settlement_status<br/>结算状态"])
  exec_mes_piecework_wage --- exec_mes_piecework_wage_settlement_status
  class exec_mes_piecework_wage_settlement_status attr;
  exec_mes_piecework_wage_created_at(["created_at<br/>创建时间"])
  exec_mes_piecework_wage --- exec_mes_piecework_wage_created_at
  class exec_mes_piecework_wage_created_at attr;

  exec_mes_work_order["生产工单表<br/>mes_work_order"]
  class exec_mes_work_order entity;
  exec_mes_work_order_work_order_id(["PK work_order_id<br/>工单主键"])
  exec_mes_work_order --- exec_mes_work_order_work_order_id
  class exec_mes_work_order_work_order_id attr;
  exec_mes_work_order_work_order_no(["UK work_order_no<br/>工单编号"])
  exec_mes_work_order --- exec_mes_work_order_work_order_no
  class exec_mes_work_order_work_order_no attr;
  exec_mes_work_order_task_id(["FK task_id<br/>生产任务"])
  exec_mes_work_order --- exec_mes_work_order_task_id
  class exec_mes_work_order_task_id attr;
  exec_mes_work_order_line_id(["FK line_id<br/>产线"])
  exec_mes_work_order --- exec_mes_work_order_line_id
  class exec_mes_work_order_line_id attr;
  exec_mes_work_order_process_id(["FK process_id<br/>工序"])
  exec_mes_work_order --- exec_mes_work_order_process_id
  class exec_mes_work_order_process_id attr;
  exec_mes_work_order_planned_qty(["planned_qty<br/>计划数量"])
  exec_mes_work_order --- exec_mes_work_order_planned_qty
  class exec_mes_work_order_planned_qty attr;
  exec_mes_work_order_actual_qty(["actual_qty<br/>实际数量"])
  exec_mes_work_order --- exec_mes_work_order_actual_qty
  class exec_mes_work_order_actual_qty attr;
  exec_mes_work_order_priority_level(["priority_level<br/>优先级"])
  exec_mes_work_order --- exec_mes_work_order_priority_level
  class exec_mes_work_order_priority_level attr;
  exec_mes_work_order_work_order_status(["work_order_status<br/>工单状态"])
  exec_mes_work_order --- exec_mes_work_order_work_order_status
  class exec_mes_work_order_work_order_status attr;
  exec_mes_work_order_dispatch_time(["dispatch_time<br/>派发时间"])
  exec_mes_work_order --- exec_mes_work_order_dispatch_time
  class exec_mes_work_order_dispatch_time attr;
  exec_mes_work_order_receive_time(["receive_time<br/>接收时间"])
  exec_mes_work_order --- exec_mes_work_order_receive_time
  class exec_mes_work_order_receive_time attr;
  exec_mes_work_order_completed_time(["completed_time<br/>完成时间"])
  exec_mes_work_order --- exec_mes_work_order_completed_time
  class exec_mes_work_order_completed_time attr;
  exec_mes_work_order_created_at(["created_at<br/>创建时间"])
  exec_mes_work_order --- exec_mes_work_order_created_at
  class exec_mes_work_order_created_at attr;
  exec_mes_work_order_updated_at(["updated_at<br/>更新时间"])
  exec_mes_work_order --- exec_mes_work_order_updated_at
  class exec_mes_work_order_updated_at attr;

  exec_mes_production_line["生产产线表<br/>mes_production_line"]
  class exec_mes_production_line entity;
  exec_mes_production_line_line_id(["PK line_id<br/>产线主键"])
  exec_mes_production_line --- exec_mes_production_line_line_id
  class exec_mes_production_line_line_id attr;
  exec_mes_production_line_line_code(["UK line_code<br/>产线编码"])
  exec_mes_production_line --- exec_mes_production_line_line_code
  class exec_mes_production_line_line_code attr;
  exec_mes_production_line_line_name(["line_name<br/>产线名称"])
  exec_mes_production_line --- exec_mes_production_line_line_name
  class exec_mes_production_line_line_name attr;
  exec_mes_production_line_line_type(["line_type<br/>产线类型"])
  exec_mes_production_line --- exec_mes_production_line_line_type
  class exec_mes_production_line_line_type attr;
  exec_mes_production_line_daily_capacity(["daily_capacity<br/>日产能"])
  exec_mes_production_line --- exec_mes_production_line_daily_capacity
  class exec_mes_production_line_daily_capacity attr;
  exec_mes_production_line_line_status(["line_status<br/>产线状态"])
  exec_mes_production_line --- exec_mes_production_line_line_status
  class exec_mes_production_line_line_status attr;
  exec_mes_production_line_enabled(["enabled<br/>是否启用"])
  exec_mes_production_line --- exec_mes_production_line_enabled
  class exec_mes_production_line_enabled attr;

  exec_mes_user["用户表<br/>mes_user"]
  class exec_mes_user entity;
  exec_mes_user_user_id(["PK user_id<br/>用户主键"])
  exec_mes_user --- exec_mes_user_user_id
  class exec_mes_user_user_id attr;
  exec_mes_user_username(["UK username<br/>登录名"])
  exec_mes_user --- exec_mes_user_username
  class exec_mes_user_username attr;
  exec_mes_user_real_name(["real_name<br/>姓名"])
  exec_mes_user --- exec_mes_user_real_name
  class exec_mes_user_real_name attr;
  exec_mes_user_role_code(["role_code<br/>角色编码"])
  exec_mes_user --- exec_mes_user_role_code
  class exec_mes_user_role_code attr;
  exec_mes_user_department(["department<br/>部门"])
  exec_mes_user --- exec_mes_user_department
  class exec_mes_user_department attr;
  exec_mes_user_phone(["phone<br/>电话"])
  exec_mes_user --- exec_mes_user_phone
  class exec_mes_user_phone attr;
  exec_mes_user_enabled(["enabled<br/>是否启用"])
  exec_mes_user --- exec_mes_user_enabled
  class exec_mes_user_enabled attr;
  exec_mes_user_created_at(["created_at<br/>创建时间"])
  exec_mes_user --- exec_mes_user_created_at
  class exec_mes_user_created_at attr;

  %% Entity relationships: parent entity --> child entity
  exec_mes_work_order -->|"一对多 - 生产工单"| exec_mes_material_requisition
  exec_mes_user -->|"一对多 - 申请人"| exec_mes_material_requisition
  exec_mes_user -->|"一对多 - 审批人"| exec_mes_material_requisition
  exec_mes_material_requisition -->|"一对多 - 领料任务"| exec_mes_material_requisition_item
  exec_mes_material -->|"一对多 - 物料"| exec_mes_material_requisition_item
  exec_mes_material -->|"一对多 - 物料"| exec_mes_inventory
  exec_mes_warehouse -->|"一对多 - 仓库"| exec_mes_inventory
  exec_mes_warehouse_location -->|"一对多 - 库位"| exec_mes_inventory
  exec_mes_warehouse -->|"一对多 - 仓库"| exec_mes_warehouse_location
  exec_mes_material -->|"一对多 - 物料"| exec_mes_inventory_transaction
  exec_mes_inventory -->|"一对多 - 库存"| exec_mes_inventory_transaction
  exec_mes_user -->|"一对多 - 操作人"| exec_mes_inventory_transaction
  exec_mes_material_requisition -->|"一对多 - 领料任务"| exec_mes_picking_task
  exec_mes_warehouse -->|"一对多 - 仓库"| exec_mes_picking_task
  exec_mes_user -->|"一对多 - 拣货人"| exec_mes_picking_task
  exec_mes_picking_task -->|"一对多 - 拣货任务"| exec_mes_robot_delivery_task
  exec_mes_robot -->|"一对多 - 机器人"| exec_mes_robot_delivery_task
  exec_mes_warehouse_location -->|"一对多 - 起点库位"| exec_mes_robot_delivery_task
  exec_mes_production_line -->|"一对多 - 目标产线"| exec_mes_robot_delivery_task
  exec_mes_work_order -->|"一对多 - 工单"| exec_mes_work_report
  exec_mes_user -->|"一对多 - 操作工"| exec_mes_work_report
  exec_mes_work_report -->|"一对多 - 报工"| exec_mes_piecework_wage
  exec_mes_user -->|"一对多 - 操作工"| exec_mes_piecework_wage
  exec_mes_production_line -->|"一对多 - 产线"| exec_mes_work_order

  %% Conceptual M:N / 1:1 business relationships
  exec_mes_material_requisition -.->|"多对多 - 通过领料任务明细"| exec_mes_material
  exec_mes_material -.->|"多对多 - 通过库存批次"| exec_mes_warehouse
  exec_mes_work_report -.->|"一对一 - 报工生成计件工资"| exec_mes_piecework_wage
```

## 质量设备与基础管理模块

```mermaid
flowchart LR
  classDef entity fill:#ffffff,stroke:#111827,stroke-width:1.5px,color:#111827;
  classDef attr fill:#ffffff,stroke:#6b7280,stroke-width:1px,color:#111827;

  qual_mes_quality_inspection["质量抽检单表<br/>mes_quality_inspection"]
  class qual_mes_quality_inspection entity;
  qual_mes_quality_inspection_inspection_id(["PK inspection_id<br/>抽检单主键"])
  qual_mes_quality_inspection --- qual_mes_quality_inspection_inspection_id
  class qual_mes_quality_inspection_inspection_id attr;
  qual_mes_quality_inspection_inspection_no(["UK inspection_no<br/>抽检单号"])
  qual_mes_quality_inspection --- qual_mes_quality_inspection_inspection_no
  class qual_mes_quality_inspection_inspection_no attr;
  qual_mes_quality_inspection_work_order_id(["FK work_order_id<br/>工单"])
  qual_mes_quality_inspection --- qual_mes_quality_inspection_work_order_id
  class qual_mes_quality_inspection_work_order_id attr;
  qual_mes_quality_inspection_sample_qty(["sample_qty<br/>抽检数量"])
  qual_mes_quality_inspection --- qual_mes_quality_inspection_sample_qty
  class qual_mes_quality_inspection_sample_qty attr;
  qual_mes_quality_inspection_inspection_status(["inspection_status<br/>抽检状态"])
  qual_mes_quality_inspection --- qual_mes_quality_inspection_inspection_status
  class qual_mes_quality_inspection_inspection_status attr;
  qual_mes_quality_inspection_inspector_id(["FK inspector_id<br/>质检员"])
  qual_mes_quality_inspection --- qual_mes_quality_inspection_inspector_id
  class qual_mes_quality_inspection_inspector_id attr;
  qual_mes_quality_inspection_inspection_time(["inspection_time<br/>检验时间"])
  qual_mes_quality_inspection --- qual_mes_quality_inspection_inspection_time
  class qual_mes_quality_inspection_inspection_time attr;
  qual_mes_quality_inspection_judgement_result(["judgement_result<br/>判定结果"])
  qual_mes_quality_inspection --- qual_mes_quality_inspection_judgement_result
  class qual_mes_quality_inspection_judgement_result attr;

  qual_mes_quality_inspection_item["质量检验项目表<br/>mes_quality_inspection_item"]
  class qual_mes_quality_inspection_item entity;
  qual_mes_quality_inspection_item_inspection_item_id(["PK inspection_item_id<br/>检验项目主键"])
  qual_mes_quality_inspection_item --- qual_mes_quality_inspection_item_inspection_item_id
  class qual_mes_quality_inspection_item_inspection_item_id attr;
  qual_mes_quality_inspection_item_inspection_id(["FK inspection_id<br/>抽检单"])
  qual_mes_quality_inspection_item --- qual_mes_quality_inspection_item_inspection_id
  class qual_mes_quality_inspection_item_inspection_id attr;
  qual_mes_quality_inspection_item_item_code(["item_code<br/>项目编码"])
  qual_mes_quality_inspection_item --- qual_mes_quality_inspection_item_item_code
  class qual_mes_quality_inspection_item_item_code attr;
  qual_mes_quality_inspection_item_item_name(["item_name<br/>项目名称"])
  qual_mes_quality_inspection_item --- qual_mes_quality_inspection_item_item_name
  class qual_mes_quality_inspection_item_item_name attr;
  qual_mes_quality_inspection_item_standard_value(["standard_value<br/>标准值"])
  qual_mes_quality_inspection_item --- qual_mes_quality_inspection_item_standard_value
  class qual_mes_quality_inspection_item_standard_value attr;
  qual_mes_quality_inspection_item_actual_value(["actual_value<br/>实测值"])
  qual_mes_quality_inspection_item --- qual_mes_quality_inspection_item_actual_value
  class qual_mes_quality_inspection_item_actual_value attr;
  qual_mes_quality_inspection_item_item_result(["item_result<br/>项目判定"])
  qual_mes_quality_inspection_item --- qual_mes_quality_inspection_item_item_result
  class qual_mes_quality_inspection_item_item_result attr;
  qual_mes_quality_inspection_item_remark(["remark<br/>备注"])
  qual_mes_quality_inspection_item --- qual_mes_quality_inspection_item_remark
  class qual_mes_quality_inspection_item_remark attr;

  qual_mes_rework_order["返工单表<br/>mes_rework_order"]
  class qual_mes_rework_order entity;
  qual_mes_rework_order_rework_order_id(["PK rework_order_id<br/>返工单主键"])
  qual_mes_rework_order --- qual_mes_rework_order_rework_order_id
  class qual_mes_rework_order_rework_order_id attr;
  qual_mes_rework_order_rework_order_no(["UK rework_order_no<br/>返工单号"])
  qual_mes_rework_order --- qual_mes_rework_order_rework_order_no
  class qual_mes_rework_order_rework_order_no attr;
  qual_mes_rework_order_source_work_order_id(["FK source_work_order_id<br/>来源工单"])
  qual_mes_rework_order --- qual_mes_rework_order_source_work_order_id
  class qual_mes_rework_order_source_work_order_id attr;
  qual_mes_rework_order_inspection_id(["FK inspection_id<br/>来源质检单"])
  qual_mes_rework_order --- qual_mes_rework_order_inspection_id
  class qual_mes_rework_order_inspection_id attr;
  qual_mes_rework_order_rework_reason(["rework_reason<br/>返工原因"])
  qual_mes_rework_order --- qual_mes_rework_order_rework_reason
  class qual_mes_rework_order_rework_reason attr;
  qual_mes_rework_order_rework_status(["rework_status<br/>返工状态"])
  qual_mes_rework_order --- qual_mes_rework_order_rework_status
  class qual_mes_rework_order_rework_status attr;
  qual_mes_rework_order_assigned_line_id(["FK assigned_line_id<br/>返工产线"])
  qual_mes_rework_order --- qual_mes_rework_order_assigned_line_id
  class qual_mes_rework_order_assigned_line_id attr;
  qual_mes_rework_order_created_at(["created_at<br/>创建时间"])
  qual_mes_rework_order --- qual_mes_rework_order_created_at
  class qual_mes_rework_order_created_at attr;
  qual_mes_rework_order_closed_at(["closed_at<br/>关闭时间"])
  qual_mes_rework_order --- qual_mes_rework_order_closed_at
  class qual_mes_rework_order_closed_at attr;

  qual_mes_quality_trace["质量追溯表<br/>mes_quality_trace"]
  class qual_mes_quality_trace entity;
  qual_mes_quality_trace_trace_id(["PK trace_id<br/>追溯主键"])
  qual_mes_quality_trace --- qual_mes_quality_trace_trace_id
  class qual_mes_quality_trace_trace_id attr;
  qual_mes_quality_trace_trace_no(["UK trace_no<br/>追溯编号"])
  qual_mes_quality_trace --- qual_mes_quality_trace_trace_no
  class qual_mes_quality_trace_trace_no attr;
  qual_mes_quality_trace_order_id(["FK order_id<br/>订单"])
  qual_mes_quality_trace --- qual_mes_quality_trace_order_id
  class qual_mes_quality_trace_order_id attr;
  qual_mes_quality_trace_task_id(["FK task_id<br/>任务"])
  qual_mes_quality_trace --- qual_mes_quality_trace_task_id
  class qual_mes_quality_trace_task_id attr;
  qual_mes_quality_trace_work_order_id(["FK work_order_id<br/>工单"])
  qual_mes_quality_trace --- qual_mes_quality_trace_work_order_id
  class qual_mes_quality_trace_work_order_id attr;
  qual_mes_quality_trace_batch_no(["batch_no<br/>批次号"])
  qual_mes_quality_trace --- qual_mes_quality_trace_batch_no
  class qual_mes_quality_trace_batch_no attr;
  qual_mes_quality_trace_inspection_id(["FK inspection_id<br/>质检单"])
  qual_mes_quality_trace --- qual_mes_quality_trace_inspection_id
  class qual_mes_quality_trace_inspection_id attr;
  qual_mes_quality_trace_rework_order_id(["FK rework_order_id<br/>返工单"])
  qual_mes_quality_trace --- qual_mes_quality_trace_rework_order_id
  class qual_mes_quality_trace_rework_order_id attr;
  qual_mes_quality_trace_trace_status(["trace_status<br/>追溯状态"])
  qual_mes_quality_trace --- qual_mes_quality_trace_trace_status
  class qual_mes_quality_trace_trace_status attr;
  qual_mes_quality_trace_created_at(["created_at<br/>创建时间"])
  qual_mes_quality_trace --- qual_mes_quality_trace_created_at
  class qual_mes_quality_trace_created_at attr;

  qual_mes_equipment["设备表<br/>mes_equipment"]
  class qual_mes_equipment entity;
  qual_mes_equipment_equipment_id(["PK equipment_id<br/>设备主键"])
  qual_mes_equipment --- qual_mes_equipment_equipment_id
  class qual_mes_equipment_equipment_id attr;
  qual_mes_equipment_equipment_code(["UK equipment_code<br/>设备编码"])
  qual_mes_equipment --- qual_mes_equipment_equipment_code
  class qual_mes_equipment_equipment_code attr;
  qual_mes_equipment_equipment_name(["equipment_name<br/>设备名称"])
  qual_mes_equipment --- qual_mes_equipment_equipment_name
  class qual_mes_equipment_equipment_name attr;
  qual_mes_equipment_equipment_type(["equipment_type<br/>设备类型"])
  qual_mes_equipment --- qual_mes_equipment_equipment_type
  class qual_mes_equipment_equipment_type attr;
  qual_mes_equipment_line_id(["FK line_id<br/>所属产线"])
  qual_mes_equipment --- qual_mes_equipment_line_id
  class qual_mes_equipment_line_id attr;
  qual_mes_equipment_equipment_status(["equipment_status<br/>设备状态"])
  qual_mes_equipment --- qual_mes_equipment_equipment_status
  class qual_mes_equipment_equipment_status attr;
  qual_mes_equipment_last_maintenance_time(["last_maintenance_time<br/>最近维护时间"])
  qual_mes_equipment --- qual_mes_equipment_last_maintenance_time
  class qual_mes_equipment_last_maintenance_time attr;
  qual_mes_equipment_enabled(["enabled<br/>是否启用"])
  qual_mes_equipment --- qual_mes_equipment_enabled
  class qual_mes_equipment_enabled attr;

  qual_mes_equipment_repair_report["设备故障报修表<br/>mes_equipment_repair_report"]
  class qual_mes_equipment_repair_report entity;
  qual_mes_equipment_repair_report_repair_report_id(["PK repair_report_id<br/>报修主键"])
  qual_mes_equipment_repair_report --- qual_mes_equipment_repair_report_repair_report_id
  class qual_mes_equipment_repair_report_repair_report_id attr;
  qual_mes_equipment_repair_report_repair_report_no(["UK repair_report_no<br/>报修单号"])
  qual_mes_equipment_repair_report --- qual_mes_equipment_repair_report_repair_report_no
  class qual_mes_equipment_repair_report_repair_report_no attr;
  qual_mes_equipment_repair_report_equipment_id(["FK equipment_id<br/>设备"])
  qual_mes_equipment_repair_report --- qual_mes_equipment_repair_report_equipment_id
  class qual_mes_equipment_repair_report_equipment_id attr;
  qual_mes_equipment_repair_report_work_order_id(["FK work_order_id<br/>关联工单"])
  qual_mes_equipment_repair_report --- qual_mes_equipment_repair_report_work_order_id
  class qual_mes_equipment_repair_report_work_order_id attr;
  qual_mes_equipment_repair_report_fault_level(["fault_level<br/>故障等级"])
  qual_mes_equipment_repair_report --- qual_mes_equipment_repair_report_fault_level
  class qual_mes_equipment_repair_report_fault_level attr;
  qual_mes_equipment_repair_report_fault_desc(["fault_desc<br/>故障描述"])
  qual_mes_equipment_repair_report --- qual_mes_equipment_repair_report_fault_desc
  class qual_mes_equipment_repair_report_fault_desc attr;
  qual_mes_equipment_repair_report_reporter_id(["FK reporter_id<br/>报修人"])
  qual_mes_equipment_repair_report --- qual_mes_equipment_repair_report_reporter_id
  class qual_mes_equipment_repair_report_reporter_id attr;
  qual_mes_equipment_repair_report_report_time(["report_time<br/>报修时间"])
  qual_mes_equipment_repair_report --- qual_mes_equipment_repair_report_report_time
  class qual_mes_equipment_repair_report_report_time attr;
  qual_mes_equipment_repair_report_repair_status(["repair_status<br/>报修状态"])
  qual_mes_equipment_repair_report --- qual_mes_equipment_repair_report_repair_status
  class qual_mes_equipment_repair_report_repair_status attr;

  qual_mes_maintenance_order["维修工单表<br/>mes_maintenance_order"]
  class qual_mes_maintenance_order entity;
  qual_mes_maintenance_order_maintenance_order_id(["PK maintenance_order_id<br/>维修工单主键"])
  qual_mes_maintenance_order --- qual_mes_maintenance_order_maintenance_order_id
  class qual_mes_maintenance_order_maintenance_order_id attr;
  qual_mes_maintenance_order_maintenance_order_no(["UK maintenance_order_no<br/>维修工单号"])
  qual_mes_maintenance_order --- qual_mes_maintenance_order_maintenance_order_no
  class qual_mes_maintenance_order_maintenance_order_no attr;
  qual_mes_maintenance_order_repair_report_id(["FK repair_report_id<br/>报修单"])
  qual_mes_maintenance_order --- qual_mes_maintenance_order_repair_report_id
  class qual_mes_maintenance_order_repair_report_id attr;
  qual_mes_maintenance_order_equipment_id(["FK equipment_id<br/>设备"])
  qual_mes_maintenance_order --- qual_mes_maintenance_order_equipment_id
  class qual_mes_maintenance_order_equipment_id attr;
  qual_mes_maintenance_order_maintainer_id(["FK maintainer_id<br/>维修人"])
  qual_mes_maintenance_order --- qual_mes_maintenance_order_maintainer_id
  class qual_mes_maintenance_order_maintainer_id attr;
  qual_mes_maintenance_order_maintenance_status(["maintenance_status<br/>维修状态"])
  qual_mes_maintenance_order --- qual_mes_maintenance_order_maintenance_status
  class qual_mes_maintenance_order_maintenance_status attr;
  qual_mes_maintenance_order_dispatch_time(["dispatch_time<br/>派发时间"])
  qual_mes_maintenance_order --- qual_mes_maintenance_order_dispatch_time
  class qual_mes_maintenance_order_dispatch_time attr;
  qual_mes_maintenance_order_finish_time(["finish_time<br/>完成时间"])
  qual_mes_maintenance_order --- qual_mes_maintenance_order_finish_time
  class qual_mes_maintenance_order_finish_time attr;
  qual_mes_maintenance_order_result_desc(["result_desc<br/>维修结果"])
  qual_mes_maintenance_order --- qual_mes_maintenance_order_result_desc
  class qual_mes_maintenance_order_result_desc attr;

  qual_mes_maintenance_plan["设备维护计划表<br/>mes_maintenance_plan"]
  class qual_mes_maintenance_plan entity;
  qual_mes_maintenance_plan_maintenance_plan_id(["PK maintenance_plan_id<br/>维护计划主键"])
  qual_mes_maintenance_plan --- qual_mes_maintenance_plan_maintenance_plan_id
  class qual_mes_maintenance_plan_maintenance_plan_id attr;
  qual_mes_maintenance_plan_equipment_id(["FK equipment_id<br/>设备"])
  qual_mes_maintenance_plan --- qual_mes_maintenance_plan_equipment_id
  class qual_mes_maintenance_plan_equipment_id attr;
  qual_mes_maintenance_plan_plan_cycle(["plan_cycle<br/>维护周期"])
  qual_mes_maintenance_plan --- qual_mes_maintenance_plan_plan_cycle
  class qual_mes_maintenance_plan_plan_cycle attr;
  qual_mes_maintenance_plan_next_plan_time(["next_plan_time<br/>下次计划时间"])
  qual_mes_maintenance_plan --- qual_mes_maintenance_plan_next_plan_time
  class qual_mes_maintenance_plan_next_plan_time attr;
  qual_mes_maintenance_plan_plan_status(["plan_status<br/>计划状态"])
  qual_mes_maintenance_plan --- qual_mes_maintenance_plan_plan_status
  class qual_mes_maintenance_plan_plan_status attr;
  qual_mes_maintenance_plan_created_at(["created_at<br/>创建时间"])
  qual_mes_maintenance_plan --- qual_mes_maintenance_plan_created_at
  class qual_mes_maintenance_plan_created_at attr;

  qual_mes_product_trace["产品追溯表<br/>mes_product_trace"]
  class qual_mes_product_trace entity;
  qual_mes_product_trace_product_trace_id(["PK product_trace_id<br/>产品追溯主键"])
  qual_mes_product_trace --- qual_mes_product_trace_product_trace_id
  class qual_mes_product_trace_product_trace_id attr;
  qual_mes_product_trace_trace_code(["UK trace_code<br/>追溯码"])
  qual_mes_product_trace --- qual_mes_product_trace_trace_code
  class qual_mes_product_trace_trace_code attr;
  qual_mes_product_trace_order_id(["FK order_id<br/>订单"])
  qual_mes_product_trace --- qual_mes_product_trace_order_id
  class qual_mes_product_trace_order_id attr;
  qual_mes_product_trace_task_id(["FK task_id<br/>任务"])
  qual_mes_product_trace --- qual_mes_product_trace_task_id
  class qual_mes_product_trace_task_id attr;
  qual_mes_product_trace_work_order_id(["FK work_order_id<br/>工单"])
  qual_mes_product_trace --- qual_mes_product_trace_work_order_id
  class qual_mes_product_trace_work_order_id attr;
  qual_mes_product_trace_product_id(["FK product_id<br/>产品"])
  qual_mes_product_trace --- qual_mes_product_trace_product_id
  class qual_mes_product_trace_product_id attr;
  qual_mes_product_trace_batch_no(["batch_no<br/>生产批次"])
  qual_mes_product_trace --- qual_mes_product_trace_batch_no
  class qual_mes_product_trace_batch_no attr;
  qual_mes_product_trace_trace_status(["trace_status<br/>追溯状态"])
  qual_mes_product_trace --- qual_mes_product_trace_trace_status
  class qual_mes_product_trace_trace_status attr;
  qual_mes_product_trace_created_at(["created_at<br/>创建时间"])
  qual_mes_product_trace --- qual_mes_product_trace_created_at
  class qual_mes_product_trace_created_at attr;

  qual_mes_dashboard_metric["综合生产看板指标表<br/>mes_dashboard_metric"]
  class qual_mes_dashboard_metric entity;
  qual_mes_dashboard_metric_metric_id(["PK metric_id<br/>指标主键"])
  qual_mes_dashboard_metric --- qual_mes_dashboard_metric_metric_id
  class qual_mes_dashboard_metric_metric_id attr;
  qual_mes_dashboard_metric_metric_date(["metric_date<br/>指标日期"])
  qual_mes_dashboard_metric --- qual_mes_dashboard_metric_metric_date
  class qual_mes_dashboard_metric_metric_date attr;
  qual_mes_dashboard_metric_metric_type(["metric_type<br/>指标类型"])
  qual_mes_dashboard_metric --- qual_mes_dashboard_metric_metric_type
  class qual_mes_dashboard_metric_metric_type attr;
  qual_mes_dashboard_metric_metric_name(["metric_name<br/>指标名称"])
  qual_mes_dashboard_metric --- qual_mes_dashboard_metric_metric_name
  class qual_mes_dashboard_metric_metric_name attr;
  qual_mes_dashboard_metric_metric_value(["metric_value<br/>指标值"])
  qual_mes_dashboard_metric --- qual_mes_dashboard_metric_metric_value
  class qual_mes_dashboard_metric_metric_value attr;
  qual_mes_dashboard_metric_metric_unit(["metric_unit<br/>单位"])
  qual_mes_dashboard_metric --- qual_mes_dashboard_metric_metric_unit
  class qual_mes_dashboard_metric_metric_unit attr;
  qual_mes_dashboard_metric_dimension_key(["dimension_key<br/>维度键"])
  qual_mes_dashboard_metric --- qual_mes_dashboard_metric_dimension_key
  class qual_mes_dashboard_metric_dimension_key attr;
  qual_mes_dashboard_metric_generated_at(["generated_at<br/>生成时间"])
  qual_mes_dashboard_metric --- qual_mes_dashboard_metric_generated_at
  class qual_mes_dashboard_metric_generated_at attr;

  qual_mes_management_feedback["管理决策反馈表<br/>mes_management_feedback"]
  class qual_mes_management_feedback entity;
  qual_mes_management_feedback_feedback_id(["PK feedback_id<br/>反馈主键"])
  qual_mes_management_feedback --- qual_mes_management_feedback_feedback_id
  class qual_mes_management_feedback_feedback_id attr;
  qual_mes_management_feedback_feedback_no(["UK feedback_no<br/>反馈编号"])
  qual_mes_management_feedback --- qual_mes_management_feedback_feedback_no
  class qual_mes_management_feedback_feedback_no attr;
  qual_mes_management_feedback_feedback_type(["feedback_type<br/>反馈类型"])
  qual_mes_management_feedback --- qual_mes_management_feedback_feedback_type
  class qual_mes_management_feedback_feedback_type attr;
  qual_mes_management_feedback_related_doc_type(["related_doc_type<br/>关联单据类型"])
  qual_mes_management_feedback --- qual_mes_management_feedback_related_doc_type
  class qual_mes_management_feedback_related_doc_type attr;
  qual_mes_management_feedback_related_doc_id(["related_doc_id<br/>关联单据ID"])
  qual_mes_management_feedback --- qual_mes_management_feedback_related_doc_id
  class qual_mes_management_feedback_related_doc_id attr;
  qual_mes_management_feedback_feedback_content(["feedback_content<br/>反馈内容"])
  qual_mes_management_feedback --- qual_mes_management_feedback_feedback_content
  class qual_mes_management_feedback_feedback_content attr;
  qual_mes_management_feedback_decision_action(["decision_action<br/>决策动作"])
  qual_mes_management_feedback --- qual_mes_management_feedback_decision_action
  class qual_mes_management_feedback_decision_action attr;
  qual_mes_management_feedback_feedback_status(["feedback_status<br/>处理状态"])
  qual_mes_management_feedback --- qual_mes_management_feedback_feedback_status
  class qual_mes_management_feedback_feedback_status attr;
  qual_mes_management_feedback_created_by(["FK created_by<br/>创建人"])
  qual_mes_management_feedback --- qual_mes_management_feedback_created_by
  class qual_mes_management_feedback_created_by attr;
  qual_mes_management_feedback_created_at(["created_at<br/>创建时间"])
  qual_mes_management_feedback --- qual_mes_management_feedback_created_at
  class qual_mes_management_feedback_created_at attr;

  qual_mes_product["产品主数据表<br/>mes_product"]
  class qual_mes_product entity;
  qual_mes_product_product_id(["PK product_id<br/>产品主键"])
  qual_mes_product --- qual_mes_product_product_id
  class qual_mes_product_product_id attr;
  qual_mes_product_product_code(["UK product_code<br/>产品编码"])
  qual_mes_product --- qual_mes_product_product_code
  class qual_mes_product_product_code attr;
  qual_mes_product_product_name(["product_name<br/>产品名称"])
  qual_mes_product --- qual_mes_product_product_name
  class qual_mes_product_product_name attr;
  qual_mes_product_product_model(["product_model<br/>产品型号"])
  qual_mes_product --- qual_mes_product_product_model
  class qual_mes_product_product_model attr;
  qual_mes_product_specification(["specification<br/>规格"])
  qual_mes_product --- qual_mes_product_specification
  class qual_mes_product_specification attr;
  qual_mes_product_enabled(["enabled<br/>是否启用"])
  qual_mes_product --- qual_mes_product_enabled
  class qual_mes_product_enabled attr;

  qual_mes_material["物料主数据表<br/>mes_material"]
  class qual_mes_material entity;
  qual_mes_material_material_id(["PK material_id<br/>物料主键"])
  qual_mes_material --- qual_mes_material_material_id
  class qual_mes_material_material_id attr;
  qual_mes_material_material_code(["UK material_code<br/>物料编码"])
  qual_mes_material --- qual_mes_material_material_code
  class qual_mes_material_material_code attr;
  qual_mes_material_material_name(["material_name<br/>物料名称"])
  qual_mes_material --- qual_mes_material_material_name
  class qual_mes_material_material_name attr;
  qual_mes_material_material_type(["material_type<br/>物料类型"])
  qual_mes_material --- qual_mes_material_material_type
  class qual_mes_material_material_type attr;
  qual_mes_material_specification(["specification<br/>规格"])
  qual_mes_material --- qual_mes_material_specification
  class qual_mes_material_specification attr;
  qual_mes_material_unit(["unit<br/>单位"])
  qual_mes_material --- qual_mes_material_unit
  class qual_mes_material_unit attr;
  qual_mes_material_shelf_life_days(["shelf_life_days<br/>保质期天数"])
  qual_mes_material --- qual_mes_material_shelf_life_days
  class qual_mes_material_shelf_life_days attr;
  qual_mes_material_enabled(["enabled<br/>是否启用"])
  qual_mes_material --- qual_mes_material_enabled
  class qual_mes_material_enabled attr;
  qual_mes_material_created_at(["created_at<br/>创建时间"])
  qual_mes_material --- qual_mes_material_created_at
  class qual_mes_material_created_at attr;

  qual_mes_product_bom["产品BOM表<br/>mes_product_bom"]
  class qual_mes_product_bom entity;
  qual_mes_product_bom_bom_id(["PK bom_id<br/>BOM主键"])
  qual_mes_product_bom --- qual_mes_product_bom_bom_id
  class qual_mes_product_bom_bom_id attr;
  qual_mes_product_bom_product_id(["FK product_id<br/>产品"])
  qual_mes_product_bom --- qual_mes_product_bom_product_id
  class qual_mes_product_bom_product_id attr;
  qual_mes_product_bom_material_id(["FK material_id<br/>物料"])
  qual_mes_product_bom --- qual_mes_product_bom_material_id
  class qual_mes_product_bom_material_id attr;
  qual_mes_product_bom_usage_qty(["usage_qty<br/>单位用量"])
  qual_mes_product_bom --- qual_mes_product_bom_usage_qty
  class qual_mes_product_bom_usage_qty attr;
  qual_mes_product_bom_unit(["unit<br/>单位"])
  qual_mes_product_bom --- qual_mes_product_bom_unit
  class qual_mes_product_bom_unit attr;
  qual_mes_product_bom_process_id(["FK process_id<br/>使用工序"])
  qual_mes_product_bom --- qual_mes_product_bom_process_id
  class qual_mes_product_bom_process_id attr;
  qual_mes_product_bom_enabled(["enabled<br/>是否启用"])
  qual_mes_product_bom --- qual_mes_product_bom_enabled
  class qual_mes_product_bom_enabled attr;

  qual_mes_process_route["工艺路线表<br/>mes_process_route"]
  class qual_mes_process_route entity;
  qual_mes_process_route_process_id(["PK process_id<br/>工序主键"])
  qual_mes_process_route --- qual_mes_process_route_process_id
  class qual_mes_process_route_process_id attr;
  qual_mes_process_route_product_id(["FK product_id<br/>产品"])
  qual_mes_process_route --- qual_mes_process_route_product_id
  class qual_mes_process_route_product_id attr;
  qual_mes_process_route_process_code(["process_code<br/>工序编码"])
  qual_mes_process_route --- qual_mes_process_route_process_code
  class qual_mes_process_route_process_code attr;
  qual_mes_process_route_process_name(["process_name<br/>工序名称"])
  qual_mes_process_route --- qual_mes_process_route_process_name
  class qual_mes_process_route_process_name attr;
  qual_mes_process_route_process_seq(["process_seq<br/>工序顺序"])
  qual_mes_process_route --- qual_mes_process_route_process_seq
  class qual_mes_process_route_process_seq attr;
  qual_mes_process_route_standard_hours(["standard_hours<br/>标准工时"])
  qual_mes_process_route --- qual_mes_process_route_standard_hours
  class qual_mes_process_route_standard_hours attr;
  qual_mes_process_route_required_equipment_type(["required_equipment_type<br/>所需设备类型"])
  qual_mes_process_route --- qual_mes_process_route_required_equipment_type
  class qual_mes_process_route_required_equipment_type attr;
  qual_mes_process_route_enabled(["enabled<br/>是否启用"])
  qual_mes_process_route --- qual_mes_process_route_enabled
  class qual_mes_process_route_enabled attr;

  qual_mes_production_line["生产产线表<br/>mes_production_line"]
  class qual_mes_production_line entity;
  qual_mes_production_line_line_id(["PK line_id<br/>产线主键"])
  qual_mes_production_line --- qual_mes_production_line_line_id
  class qual_mes_production_line_line_id attr;
  qual_mes_production_line_line_code(["UK line_code<br/>产线编码"])
  qual_mes_production_line --- qual_mes_production_line_line_code
  class qual_mes_production_line_line_code attr;
  qual_mes_production_line_line_name(["line_name<br/>产线名称"])
  qual_mes_production_line --- qual_mes_production_line_line_name
  class qual_mes_production_line_line_name attr;
  qual_mes_production_line_line_type(["line_type<br/>产线类型"])
  qual_mes_production_line --- qual_mes_production_line_line_type
  class qual_mes_production_line_line_type attr;
  qual_mes_production_line_daily_capacity(["daily_capacity<br/>日产能"])
  qual_mes_production_line --- qual_mes_production_line_daily_capacity
  class qual_mes_production_line_daily_capacity attr;
  qual_mes_production_line_line_status(["line_status<br/>产线状态"])
  qual_mes_production_line --- qual_mes_production_line_line_status
  class qual_mes_production_line_line_status attr;
  qual_mes_production_line_enabled(["enabled<br/>是否启用"])
  qual_mes_production_line --- qual_mes_production_line_enabled
  class qual_mes_production_line_enabled attr;

  qual_mes_sync_log["数据同步日志表<br/>mes_sync_log"]
  class qual_mes_sync_log entity;
  qual_mes_sync_log_sync_log_id(["PK sync_log_id<br/>同步日志主键"])
  qual_mes_sync_log --- qual_mes_sync_log_sync_log_id
  class qual_mes_sync_log_sync_log_id attr;
  qual_mes_sync_log_source_system(["source_system<br/>来源系统"])
  qual_mes_sync_log --- qual_mes_sync_log_source_system
  class qual_mes_sync_log_source_system attr;
  qual_mes_sync_log_sync_object(["sync_object<br/>同步对象"])
  qual_mes_sync_log --- qual_mes_sync_log_sync_object
  class qual_mes_sync_log_sync_object attr;
  qual_mes_sync_log_business_key(["business_key<br/>业务键"])
  qual_mes_sync_log --- qual_mes_sync_log_business_key
  class qual_mes_sync_log_business_key attr;
  qual_mes_sync_log_sync_status(["sync_status<br/>同步状态"])
  qual_mes_sync_log --- qual_mes_sync_log_sync_status
  class qual_mes_sync_log_sync_status attr;
  qual_mes_sync_log_error_message(["error_message<br/>错误信息"])
  qual_mes_sync_log --- qual_mes_sync_log_error_message
  class qual_mes_sync_log_error_message attr;
  qual_mes_sync_log_sync_time(["sync_time<br/>同步时间"])
  qual_mes_sync_log --- qual_mes_sync_log_sync_time
  class qual_mes_sync_log_sync_time attr;

  qual_mes_user["用户表<br/>mes_user"]
  class qual_mes_user entity;
  qual_mes_user_user_id(["PK user_id<br/>用户主键"])
  qual_mes_user --- qual_mes_user_user_id
  class qual_mes_user_user_id attr;
  qual_mes_user_username(["UK username<br/>登录名"])
  qual_mes_user --- qual_mes_user_username
  class qual_mes_user_username attr;
  qual_mes_user_real_name(["real_name<br/>姓名"])
  qual_mes_user --- qual_mes_user_real_name
  class qual_mes_user_real_name attr;
  qual_mes_user_role_code(["role_code<br/>角色编码"])
  qual_mes_user --- qual_mes_user_role_code
  class qual_mes_user_role_code attr;
  qual_mes_user_department(["department<br/>部门"])
  qual_mes_user --- qual_mes_user_department
  class qual_mes_user_department attr;
  qual_mes_user_phone(["phone<br/>电话"])
  qual_mes_user --- qual_mes_user_phone
  class qual_mes_user_phone attr;
  qual_mes_user_enabled(["enabled<br/>是否启用"])
  qual_mes_user --- qual_mes_user_enabled
  class qual_mes_user_enabled attr;
  qual_mes_user_created_at(["created_at<br/>创建时间"])
  qual_mes_user --- qual_mes_user_created_at
  class qual_mes_user_created_at attr;

  qual_mes_customer_order["客户订单表<br/>mes_customer_order"]
  class qual_mes_customer_order entity;
  qual_mes_customer_order_order_id(["PK order_id<br/>订单主键"])
  qual_mes_customer_order --- qual_mes_customer_order_order_id
  class qual_mes_customer_order_order_id attr;
  qual_mes_customer_order_order_no(["UK order_no<br/>订单编号"])
  qual_mes_customer_order --- qual_mes_customer_order_order_no
  class qual_mes_customer_order_order_no attr;
  qual_mes_customer_order_customer_name(["customer_name<br/>客户名称"])
  qual_mes_customer_order --- qual_mes_customer_order_customer_name
  class qual_mes_customer_order_customer_name attr;
  qual_mes_customer_order_product_id(["FK product_id<br/>产品主键"])
  qual_mes_customer_order --- qual_mes_customer_order_product_id
  class qual_mes_customer_order_product_id attr;
  qual_mes_customer_order_product_code(["product_code<br/>产品编码快照"])
  qual_mes_customer_order --- qual_mes_customer_order_product_code
  class qual_mes_customer_order_product_code attr;
  qual_mes_customer_order_product_model(["product_model<br/>轮胎型号/规格"])
  qual_mes_customer_order --- qual_mes_customer_order_product_model
  class qual_mes_customer_order_product_model attr;
  qual_mes_customer_order_order_qty(["order_qty<br/>订单数量"])
  qual_mes_customer_order --- qual_mes_customer_order_order_qty
  class qual_mes_customer_order_order_qty attr;
  qual_mes_customer_order_unit(["unit<br/>计量单位"])
  qual_mes_customer_order --- qual_mes_customer_order_unit
  class qual_mes_customer_order_unit attr;
  qual_mes_customer_order_delivery_date(["delivery_date<br/>交付日期"])
  qual_mes_customer_order --- qual_mes_customer_order_delivery_date
  class qual_mes_customer_order_delivery_date attr;
  qual_mes_customer_order_priority_level(["priority_level<br/>优先级，1最高"])
  qual_mes_customer_order --- qual_mes_customer_order_priority_level
  class qual_mes_customer_order_priority_level attr;
  qual_mes_customer_order_order_status(["order_status<br/>订单状态"])
  qual_mes_customer_order --- qual_mes_customer_order_order_status
  class qual_mes_customer_order_order_status attr;
  qual_mes_customer_order_source_system(["source_system<br/>来源系统"])
  qual_mes_customer_order --- qual_mes_customer_order_source_system
  class qual_mes_customer_order_source_system attr;
  qual_mes_customer_order_remark(["remark<br/>备注"])
  qual_mes_customer_order --- qual_mes_customer_order_remark
  class qual_mes_customer_order_remark attr;
  qual_mes_customer_order_created_at(["created_at<br/>创建时间"])
  qual_mes_customer_order --- qual_mes_customer_order_created_at
  class qual_mes_customer_order_created_at attr;
  qual_mes_customer_order_updated_at(["updated_at<br/>更新时间"])
  qual_mes_customer_order --- qual_mes_customer_order_updated_at
  class qual_mes_customer_order_updated_at attr;

  qual_mes_production_task["生产任务表<br/>mes_production_task"]
  class qual_mes_production_task entity;
  qual_mes_production_task_task_id(["PK task_id<br/>任务主键"])
  qual_mes_production_task --- qual_mes_production_task_task_id
  class qual_mes_production_task_task_id attr;
  qual_mes_production_task_task_no(["UK task_no<br/>任务编号"])
  qual_mes_production_task --- qual_mes_production_task_task_no
  class qual_mes_production_task_task_no attr;
  qual_mes_production_task_order_id(["FK order_id<br/>来源订单"])
  qual_mes_production_task --- qual_mes_production_task_order_id
  class qual_mes_production_task_order_id attr;
  qual_mes_production_task_planner_id(["FK planner_id<br/>PMC计划员"])
  qual_mes_production_task --- qual_mes_production_task_planner_id
  class qual_mes_production_task_planner_id attr;
  qual_mes_production_task_plan_qty(["plan_qty<br/>计划数量"])
  qual_mes_production_task --- qual_mes_production_task_plan_qty
  class qual_mes_production_task_plan_qty attr;
  qual_mes_production_task_planned_start_time(["planned_start_time<br/>计划开始时间"])
  qual_mes_production_task --- qual_mes_production_task_planned_start_time
  class qual_mes_production_task_planned_start_time attr;
  qual_mes_production_task_planned_end_time(["planned_end_time<br/>计划完成时间"])
  qual_mes_production_task --- qual_mes_production_task_planned_end_time
  class qual_mes_production_task_planned_end_time attr;
  qual_mes_production_task_target_line_id(["FK target_line_id<br/>目标产线"])
  qual_mes_production_task --- qual_mes_production_task_target_line_id
  class qual_mes_production_task_target_line_id attr;
  qual_mes_production_task_task_status(["task_status<br/>任务状态"])
  qual_mes_production_task --- qual_mes_production_task_task_status
  class qual_mes_production_task_task_status attr;
  qual_mes_production_task_kitting_status(["kitting_status<br/>齐套状态"])
  qual_mes_production_task --- qual_mes_production_task_kitting_status
  class qual_mes_production_task_kitting_status attr;
  qual_mes_production_task_release_time(["release_time<br/>发布时间"])
  qual_mes_production_task --- qual_mes_production_task_release_time
  class qual_mes_production_task_release_time attr;
  qual_mes_production_task_close_time(["close_time<br/>闭环时间"])
  qual_mes_production_task --- qual_mes_production_task_close_time
  class qual_mes_production_task_close_time attr;
  qual_mes_production_task_remark(["remark<br/>备注"])
  qual_mes_production_task --- qual_mes_production_task_remark
  class qual_mes_production_task_remark attr;
  qual_mes_production_task_created_at(["created_at<br/>创建时间"])
  qual_mes_production_task --- qual_mes_production_task_created_at
  class qual_mes_production_task_created_at attr;
  qual_mes_production_task_updated_at(["updated_at<br/>更新时间"])
  qual_mes_production_task --- qual_mes_production_task_updated_at
  class qual_mes_production_task_updated_at attr;

  qual_mes_work_order["生产工单表<br/>mes_work_order"]
  class qual_mes_work_order entity;
  qual_mes_work_order_work_order_id(["PK work_order_id<br/>工单主键"])
  qual_mes_work_order --- qual_mes_work_order_work_order_id
  class qual_mes_work_order_work_order_id attr;
  qual_mes_work_order_work_order_no(["UK work_order_no<br/>工单编号"])
  qual_mes_work_order --- qual_mes_work_order_work_order_no
  class qual_mes_work_order_work_order_no attr;
  qual_mes_work_order_task_id(["FK task_id<br/>生产任务"])
  qual_mes_work_order --- qual_mes_work_order_task_id
  class qual_mes_work_order_task_id attr;
  qual_mes_work_order_line_id(["FK line_id<br/>产线"])
  qual_mes_work_order --- qual_mes_work_order_line_id
  class qual_mes_work_order_line_id attr;
  qual_mes_work_order_process_id(["FK process_id<br/>工序"])
  qual_mes_work_order --- qual_mes_work_order_process_id
  class qual_mes_work_order_process_id attr;
  qual_mes_work_order_planned_qty(["planned_qty<br/>计划数量"])
  qual_mes_work_order --- qual_mes_work_order_planned_qty
  class qual_mes_work_order_planned_qty attr;
  qual_mes_work_order_actual_qty(["actual_qty<br/>实际数量"])
  qual_mes_work_order --- qual_mes_work_order_actual_qty
  class qual_mes_work_order_actual_qty attr;
  qual_mes_work_order_priority_level(["priority_level<br/>优先级"])
  qual_mes_work_order --- qual_mes_work_order_priority_level
  class qual_mes_work_order_priority_level attr;
  qual_mes_work_order_work_order_status(["work_order_status<br/>工单状态"])
  qual_mes_work_order --- qual_mes_work_order_work_order_status
  class qual_mes_work_order_work_order_status attr;
  qual_mes_work_order_dispatch_time(["dispatch_time<br/>派发时间"])
  qual_mes_work_order --- qual_mes_work_order_dispatch_time
  class qual_mes_work_order_dispatch_time attr;
  qual_mes_work_order_receive_time(["receive_time<br/>接收时间"])
  qual_mes_work_order --- qual_mes_work_order_receive_time
  class qual_mes_work_order_receive_time attr;
  qual_mes_work_order_completed_time(["completed_time<br/>完成时间"])
  qual_mes_work_order --- qual_mes_work_order_completed_time
  class qual_mes_work_order_completed_time attr;
  qual_mes_work_order_created_at(["created_at<br/>创建时间"])
  qual_mes_work_order --- qual_mes_work_order_created_at
  class qual_mes_work_order_created_at attr;
  qual_mes_work_order_updated_at(["updated_at<br/>更新时间"])
  qual_mes_work_order --- qual_mes_work_order_updated_at
  class qual_mes_work_order_updated_at attr;

  %% Entity relationships: parent entity --> child entity
  qual_mes_work_order -->|"一对多 - 工单"| qual_mes_quality_inspection
  qual_mes_user -->|"一对多 - 质检员"| qual_mes_quality_inspection
  qual_mes_quality_inspection -->|"一对多 - 抽检单"| qual_mes_quality_inspection_item
  qual_mes_work_order -->|"一对多 - 来源工单"| qual_mes_rework_order
  qual_mes_quality_inspection -->|"一对多 - 来源质检单"| qual_mes_rework_order
  qual_mes_production_line -->|"一对多 - 返工产线"| qual_mes_rework_order
  qual_mes_customer_order -->|"一对多 - 订单"| qual_mes_quality_trace
  qual_mes_production_task -->|"一对多 - 任务"| qual_mes_quality_trace
  qual_mes_work_order -->|"一对多 - 工单"| qual_mes_quality_trace
  qual_mes_quality_inspection -->|"一对多 - 质检单"| qual_mes_quality_trace
  qual_mes_rework_order -->|"一对多 - 返工单"| qual_mes_quality_trace
  qual_mes_production_line -->|"一对多 - 所属产线"| qual_mes_equipment
  qual_mes_equipment -->|"一对多 - 设备"| qual_mes_equipment_repair_report
  qual_mes_work_order -->|"一对多 - 关联工单"| qual_mes_equipment_repair_report
  qual_mes_user -->|"一对多 - 报修人"| qual_mes_equipment_repair_report
  qual_mes_equipment_repair_report -->|"一对多 - 报修单"| qual_mes_maintenance_order
  qual_mes_equipment -->|"一对多 - 设备"| qual_mes_maintenance_order
  qual_mes_user -->|"一对多 - 维修人"| qual_mes_maintenance_order
  qual_mes_equipment -->|"一对多 - 设备"| qual_mes_maintenance_plan
  qual_mes_customer_order -->|"一对多 - 订单"| qual_mes_product_trace
  qual_mes_production_task -->|"一对多 - 任务"| qual_mes_product_trace
  qual_mes_work_order -->|"一对多 - 工单"| qual_mes_product_trace
  qual_mes_product -->|"一对多 - 产品"| qual_mes_product_trace
  qual_mes_user -->|"一对多 - 创建人"| qual_mes_management_feedback
  qual_mes_product -->|"一对多 - 产品"| qual_mes_product_bom
  qual_mes_material -->|"一对多 - 物料"| qual_mes_product_bom
  qual_mes_process_route -->|"一对多 - 使用工序"| qual_mes_product_bom
  qual_mes_product -->|"一对多 - 产品"| qual_mes_process_route
  qual_mes_product -->|"一对多 - 产品主键"| qual_mes_customer_order
  qual_mes_customer_order -->|"一对多 - 来源订单"| qual_mes_production_task
  qual_mes_user -->|"一对多 - PMC计划员"| qual_mes_production_task
  qual_mes_production_line -->|"一对多 - 目标产线"| qual_mes_production_task
  qual_mes_production_task -->|"一对多 - 生产任务"| qual_mes_work_order
  qual_mes_production_line -->|"一对多 - 产线"| qual_mes_work_order
  qual_mes_process_route -->|"一对多 - 工序"| qual_mes_work_order

  %% Conceptual M:N / 1:1 business relationships
  qual_mes_product -.->|"多对多 - 通过产品BOM"| qual_mes_material
  qual_mes_equipment_repair_report -.->|"一对一 - 报修生成维修工单"| qual_mes_maintenance_order
```


