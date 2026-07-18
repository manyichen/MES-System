/*
 * 答辩定位：仓储、领料、拣货与机器人物流 模块的 MesMaterialRequisitionItem。
 * 分层职责：领域/传输模型：承载数据库字段或接口 JSON。Jackson 通过公开字段、构造器或 record 组件完成序列化与反序列化。
 * 典型调用链：PostgreSQL/JDBC <-> DAO <-> 当前模型 <-> Jackson JSON <-> Vue 页面。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.warehouse.entity;

import java.math.BigDecimal;

/**
 * 仓储、领料、拣货与机器人物流 的数据模型；字段名与接口 JSON/数据库列保持可追踪关系，不在模型中实现业务规则。
 */
public class MesMaterialRequisitionItem {
    /** requisitionItemId 对应的关联记录主键。 */
    public Long requisitionItemId;
    /** requisitionId 对应的关联记录主键。 */
    public Long requisitionId;
    /** 物料主键，关联 BOM、库存与领料明细。 */
    public Long materialId;
    /** requiredQty 对应的业务数量。 */
    public BigDecimal requiredQty;
    /** issuedQty 对应的业务数量。 */
    public BigDecimal issuedQty;
    /** unit 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String unit;
    /** 生产批次号，用于质量与产品全链路追溯。 */
    public String batchNo;
    /** itemStatus 对应的业务状态，决定后续可执行动作。 */
    public String itemStatus;
}
