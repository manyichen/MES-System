/*
 * 答辩定位：仓储、领料、拣货与机器人物流 模块的 MesMaterialRequisition。
 * 分层职责：领域/传输模型：承载数据库字段或接口 JSON。Jackson 通过公开字段、构造器或 record 组件完成序列化与反序列化。
 * 典型调用链：PostgreSQL/JDBC <-> DAO <-> 当前模型 <-> Jackson JSON <-> Vue 页面。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.warehouse.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 仓储、领料、拣货与机器人物流 的数据模型；字段名与接口 JSON/数据库列保持可追踪关系，不在模型中实现业务规则。
 */
public class MesMaterialRequisition {
    /** requisitionId 对应的关联记录主键。 */
    public Long requisitionId;
    /** requisitionNo 对应的业务单号，便于人工识别和检索。 */
    public String requisitionNo;
    /** 生产工单主键，连接计划、报工、质检与追溯。 */
    public Long workOrderId;
    /** 仓库主键，用于库存归属和数据范围隔离。 */
    public Long warehouseId;
    /** requestedBy 业务字段；具体取值由创建/更新用例校验后写入。 */
    public Long requestedBy;
    /** requestStatus 对应的业务状态，决定后续可执行动作。 */
    public String requestStatus;
    /** requestTime 对应的业务时间点。 */
    public LocalDateTime requestTime;
    /** approvedBy 业务字段；具体取值由创建/更新用例校验后写入。 */
    public Long approvedBy;
    /** approvedTime 对应的业务时间点。 */
    public LocalDateTime approvedTime;
    /** 可选业务备注。 */
    public String remark;
    /** pickingTaskId 对应的关联记录主键。 */
    public Long pickingTaskId;
    /** pickingTaskStatus 对应的业务状态，决定后续可执行动作。 */
    public String pickingTaskStatus;
    /** deliveryTaskId 对应的关联记录主键。 */
    public Long deliveryTaskId;
    /** deliveryStatus 对应的业务状态，决定后续可执行动作。 */
    public String deliveryStatus;
    /** items 业务字段；具体取值由创建/更新用例校验后写入。 */
    public List<MesMaterialRequisitionItem> items = new ArrayList<>();
}
