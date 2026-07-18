/*
 * 答辩定位：主数据与用户 模块的 MesProductBom。
 * 分层职责：领域/传输模型：承载数据库字段或接口 JSON。Jackson 通过公开字段、构造器或 record 组件完成序列化与反序列化。
 * 典型调用链：PostgreSQL/JDBC <-> DAO <-> 当前模型 <-> Jackson JSON <-> Vue 页面。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.master.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 主数据与用户 的数据模型；字段名与接口 JSON/数据库列保持可追踪关系，不在模型中实现业务规则。
 */
public class MesProductBom {
    /** bomId 对应的关联记录主键。 */
    public Long bomId;
    /** 产品主键，关联轮胎规格、BOM 与工艺路线。 */
    public Long productId;
    /** productCode 对应的稳定业务编码。 */
    public String productCode;
    /** productName 对应的展示名称。 */
    public String productName;
    /** 物料主键，关联 BOM、库存与领料明细。 */
    public Long materialId;
    /** materialCode 对应的稳定业务编码。 */
    public String materialCode;
    /** materialName 对应的展示名称。 */
    public String materialName;
    /** qtyPerUnit 业务字段；具体取值由创建/更新用例校验后写入。 */
    public BigDecimal qtyPerUnit;
    /** unit 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String unit;
    /** 是否启用；停用记录通常保留历史关联但不再参与新业务。 */
    public Integer enabled;
    /** 记录创建时间，用于排序、追溯和审计。 */
    public LocalDateTime createdAt;
}
