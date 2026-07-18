/*
 * 答辩定位：订单、计划、齐套与工单 模块的 MesWorkOrder。
 * 分层职责：领域/传输模型：承载数据库字段或接口 JSON。Jackson 通过公开字段、构造器或 record 组件完成序列化与反序列化。
 * 典型调用链：PostgreSQL/JDBC <-> DAO <-> 当前模型 <-> Jackson JSON <-> Vue 页面。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.entity;

import java.time.LocalDateTime;

/**
 * 订单、计划、齐套与工单 的数据模型；字段名与接口 JSON/数据库列保持可追踪关系，不在模型中实现业务规则。
 */
public class MesWorkOrder {
    /** 生产工单主键，连接计划、报工、质检与追溯。 */
    public Long workOrderId;
    /** workOrderNo 对应的业务单号，便于人工识别和检索。 */
    public String workOrderNo;
    /** 生产任务主键，连接客户订单与生产工单。 */
    public Long taskId;
    /** 产品主键，关联轮胎规格、BOM 与工艺路线。 */
    public Long productId;
    /** 生产线主键，用于排产和数据范围隔离。 */
    public Long lineId;
    /** processId 对应的关联记录主键。 */
    public Long processId;
    /** plannedQty 对应的业务数量。 */
    public Integer plannedQty;
    /** actualQty 对应的业务数量。 */
    public Integer actualQty;
    /** priorityLevel 业务字段；具体取值由创建/更新用例校验后写入。 */
    public Integer priorityLevel;
    /** workOrderStatus 对应的业务状态，决定后续可执行动作。 */
    public String workOrderStatus;
    /** 生产批次号，用于质量与产品全链路追溯。 */
    public String batchNo;
    /** assignedTo 业务字段；具体取值由创建/更新用例校验后写入。 */
    public Long assignedTo;
    /** acceptedBy 业务字段；具体取值由创建/更新用例校验后写入。 */
    public Long acceptedBy;
    /** dispatchTime 对应的业务时间点。 */
    public LocalDateTime dispatchTime;
    /** receiveTime 对应的业务时间点。 */
    public LocalDateTime receiveTime;
    /** completedTime 对应的业务时间点。 */
    public LocalDateTime completedTime;
    /** 记录创建时间，用于排序、追溯和审计。 */
    public LocalDateTime createdAt;
    /** 记录最后更新时间。 */
    public LocalDateTime updatedAt;
}
