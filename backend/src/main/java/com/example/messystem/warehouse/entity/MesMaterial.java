/*
 * 答辩定位：仓储、领料、拣货与机器人物流 模块的 MesMaterial。
 * 分层职责：领域/传输模型：承载数据库字段或接口 JSON。Jackson 通过公开字段、构造器或 record 组件完成序列化与反序列化。
 * 典型调用链：PostgreSQL/JDBC <-> DAO <-> 当前模型 <-> Jackson JSON <-> Vue 页面。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.warehouse.entity;

import java.time.LocalDateTime;

/**
 * 仓储、领料、拣货与机器人物流 的数据模型；字段名与接口 JSON/数据库列保持可追踪关系，不在模型中实现业务规则。
 */
public class MesMaterial {
    /** 物料主键，关联 BOM、库存与领料明细。 */
    public Long materialId;
    /** materialCode 对应的稳定业务编码。 */
    public String materialCode;
    /** materialName 对应的展示名称。 */
    public String materialName;
    /** materialType 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String materialType;
    /** specification 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String specification;
    /** unit 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String unit;
    /** shelfLifeDays 业务字段；具体取值由创建/更新用例校验后写入。 */
    public Integer shelfLifeDays;
    /** 是否启用；停用记录通常保留历史关联但不再参与新业务。 */
    public Integer enabled;
    /** defaultWarehouseType 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String defaultWarehouseType;
    /** defaultWarehouseId 对应的关联记录主键。 */
    public Long defaultWarehouseId;
    /** defaultWarehouseCode 对应的稳定业务编码。 */
    public String defaultWarehouseCode;
    /** defaultWarehouseName 对应的展示名称。 */
    public String defaultWarehouseName;
    /** 记录创建时间，用于排序、追溯和审计。 */
    public LocalDateTime createdAt;
}
