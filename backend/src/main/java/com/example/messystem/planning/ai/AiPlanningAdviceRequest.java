/*
 * 答辩定位：订单、计划、齐套与工单 模块的 AiPlanningAdviceRequest。
 * 分层职责：公共支撑代码：提供多个业务模块共享的响应、异常、编码或工具能力。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.ai;

import java.util.List;

/**
 * 订单、计划、齐套与工单 的 AiPlanningAdviceRequest，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class AiPlanningAdviceRequest {
    /** taskIds 对应的关联主键集合。 */
    public List<Long> taskIds;
    /** horizonDays 业务字段；具体取值由创建/更新用例校验后写入。 */
    public Integer horizonDays;
    /** objective 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String objective;
}
