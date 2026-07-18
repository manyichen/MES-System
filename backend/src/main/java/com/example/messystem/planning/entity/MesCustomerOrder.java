/*
 * 答辩定位：订单、计划、齐套与工单 模块的 MesCustomerOrder。
 * 分层职责：领域/传输模型：承载数据库字段或接口 JSON。Jackson 通过公开字段、构造器或 record 组件完成序列化与反序列化。
 * 典型调用链：PostgreSQL/JDBC <-> DAO <-> 当前模型 <-> Jackson JSON <-> Vue 页面。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单、计划、齐套与工单 的数据模型；字段名与接口 JSON/数据库列保持可追踪关系，不在模型中实现业务规则。
 */
public class MesCustomerOrder {
    /** 客户订单主键，是计划到生产链路的起点。 */
    public Long orderId;
    /** orderNo 对应的业务单号，便于人工识别和检索。 */
    public String orderNo;
    /** customerName 对应的展示名称。 */
    public String customerName;
    /** 产品主键，关联轮胎规格、BOM 与工艺路线。 */
    public Long productId;
    /** productCode 对应的稳定业务编码。 */
    public String productCode;
    /** productModel 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String productModel;
    /** orderQty 对应的业务数量。 */
    public Integer orderQty;
    /** unit 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String unit;
    /** deliveryDate 业务字段；具体取值由创建/更新用例校验后写入。 */
    public LocalDate deliveryDate;
    /** priorityLevel 业务字段；具体取值由创建/更新用例校验后写入。 */
    public Integer priorityLevel;
    /** orderStatus 对应的业务状态，决定后续可执行动作。 */
    public String orderStatus;
    /** sourceSystem 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String sourceSystem;
    /** 可选业务备注。 */
    public String remark;
    /** 记录创建时间，用于排序、追溯和审计。 */
    public LocalDateTime createdAt;
    /** 记录最后更新时间。 */
    public LocalDateTime updatedAt;
}
