/*
 * 答辩定位：订单、计划、齐套与工单 模块的 MesShortageAlert。
 * 分层职责：领域/传输模型：承载数据库字段或接口 JSON。Jackson 通过公开字段、构造器或 record 组件完成序列化与反序列化。
 * 典型调用链：PostgreSQL/JDBC <-> DAO <-> 当前模型 <-> Jackson JSON <-> Vue 页面。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单、计划、齐套与工单 的数据模型；字段名与接口 JSON/数据库列保持可追踪关系，不在模型中实现业务规则。
 */
public class MesShortageAlert {
    /** alertId 对应的关联记录主键。 */
    public Long alertId;
    /** alertNo 对应的业务单号，便于人工识别和检索。 */
    public String alertNo;
    /** 生产任务主键，连接客户订单与生产工单。 */
    public Long taskId;
    /** analysisId 对应的关联记录主键。 */
    public Long analysisId;
    /** 物料主键，关联 BOM、库存与领料明细。 */
    public Long materialId;
    /** materialCode 对应的稳定业务编码。 */
    public String materialCode;
    /** materialName 对应的展示名称。 */
    public String materialName;
    /** requiredQty 对应的业务数量。 */
    public BigDecimal requiredQty;
    /** availableQty 对应的业务数量。 */
    public BigDecimal availableQty;
    /** shortageQty 对应的业务数量。 */
    public BigDecimal shortageQty;
    /** alertLevel 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String alertLevel;
    /** alertStatus 对应的业务状态，决定后续可执行动作。 */
    public String alertStatus;
    /** acceptedBy 业务字段；具体取值由创建/更新用例校验后写入。 */
    public Long acceptedBy;
    /** acceptedAt 对应的业务时间点。 */
    public LocalDateTime acceptedAt;
    /** resolvedAt 对应的业务时间点。 */
    public LocalDateTime resolvedAt;
    /** 记录创建时间，用于排序、追溯和审计。 */
    public LocalDateTime createdAt;
}
