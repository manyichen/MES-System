/*
 * 答辩定位：订单、计划、齐套与工单 模块的 MesKittingShortageItem。
 * 分层职责：领域/传输模型：承载数据库字段或接口 JSON。Jackson 通过公开字段、构造器或 record 组件完成序列化与反序列化。
 * 典型调用链：PostgreSQL/JDBC <-> DAO <-> 当前模型 <-> Jackson JSON <-> Vue 页面。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.entity;

import java.math.BigDecimal;

/**
 * 订单、计划、齐套与工单 的数据模型；字段名与接口 JSON/数据库列保持可追踪关系，不在模型中实现业务规则。
 */
public class MesKittingShortageItem {
    /** shortageItemId 对应的关联记录主键。 */
    public Long shortageItemId;
    /** analysisId 对应的关联记录主键。 */
    public Long analysisId;
    /** 生产任务主键，连接客户订单与生产工单。 */
    public Long taskId;
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
    /** itemStatus 对应的业务状态，决定后续可执行动作。 */
    public String itemStatus;
}
