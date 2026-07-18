/*
 * 答辩定位：订单、计划、齐套与工单 模块的 MesKittingAnalysis。
 * 分层职责：领域/传输模型：承载数据库字段或接口 JSON。Jackson 通过公开字段、构造器或 record 组件完成序列化与反序列化。
 * 典型调用链：PostgreSQL/JDBC <-> DAO <-> 当前模型 <-> Jackson JSON <-> Vue 页面。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单、计划、齐套与工单 的数据模型；字段名与接口 JSON/数据库列保持可追踪关系，不在模型中实现业务规则。
 */
public class MesKittingAnalysis {
    /** analysisId 对应的关联记录主键。 */
    public Long analysisId;
    /** analysisNo 对应的业务单号，便于人工识别和检索。 */
    public String analysisNo;
    /** 生产任务主键，连接客户订单与生产工单。 */
    public Long taskId;
    /** 产品主键，关联轮胎规格、BOM 与工艺路线。 */
    public Long productId;
    /** planQty 对应的业务数量。 */
    public Integer planQty;
    /** kittingStatus 对应的业务状态，决定后续可执行动作。 */
    public String kittingStatus;
    /** analysisResult 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String analysisResult;
    /** analysisTime 对应的业务时间点。 */
    public LocalDateTime analysisTime;
    /** shortageItems 业务字段；具体取值由创建/更新用例校验后写入。 */
    public List<MesKittingShortageItem> shortageItems = new ArrayList<>();
}
