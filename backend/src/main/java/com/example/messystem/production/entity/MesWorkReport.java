/*
 * 答辩定位：生产报工与计件工资 模块的 MesWorkReport。
 * 分层职责：领域/传输模型：承载数据库字段或接口 JSON。Jackson 通过公开字段、构造器或 record 组件完成序列化与反序列化。
 * 典型调用链：PostgreSQL/JDBC <-> DAO <-> 当前模型 <-> Jackson JSON <-> Vue 页面。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.production.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产报工与计件工资 的数据模型；字段名与接口 JSON/数据库列保持可追踪关系，不在模型中实现业务规则。
 */
public class MesWorkReport {
    /** reportId 对应的关联记录主键。 */
    public Long reportId;
    /** reportNo 对应的业务单号，便于人工识别和检索。 */
    public String reportNo;
    /** 生产工单主键，连接计划、报工、质检与追溯。 */
    public Long workOrderId;
    /** 生产批次号，用于质量与产品全链路追溯。 */
    public String batchNo;
    /** operatorId 对应的关联记录主键。 */
    public Long operatorId;
    /** reportQty 对应的业务数量。 */
    public Integer reportQty;
    /** qualifiedQty 对应的业务数量。 */
    public Integer qualifiedQty;
    /** defectQty 对应的业务数量。 */
    public Integer defectQty;
    /** workHours 业务字段；具体取值由创建/更新用例校验后写入。 */
    public BigDecimal workHours;
    /** reportTime 对应的业务时间点。 */
    public LocalDateTime reportTime;
    /** reportStatus 对应的业务状态，决定后续可执行动作。 */
    public String reportStatus;
    /** 可选业务备注。 */
    public String remark;
    /** rejectReason 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String rejectReason;
}
