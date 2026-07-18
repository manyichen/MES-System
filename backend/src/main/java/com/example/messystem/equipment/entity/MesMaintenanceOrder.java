/*
 * 答辩定位：设备与维修保养 模块的 MesMaintenanceOrder。
 * 分层职责：领域/传输模型：承载数据库字段或接口 JSON。Jackson 通过公开字段、构造器或 record 组件完成序列化与反序列化。
 * 典型调用链：PostgreSQL/JDBC <-> DAO <-> 当前模型 <-> Jackson JSON <-> Vue 页面。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.equipment.entity;

import java.time.LocalDateTime;

/**
 * 设备与维修保养 的数据模型；字段名与接口 JSON/数据库列保持可追踪关系，不在模型中实现业务规则。
 */
/**
 * 公共能力：执行 MesMaintenanceOrder 对应的业务步骤。
 * 由 MesMaintenanceOrder 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
 */
public record MesMaintenanceOrder(
        Long maintenanceOrderId,
        String maintenanceOrderNo,
        Long repairReportId,
        Long equipmentId,
        Long maintainerId,
        String maintenanceStatus,
        LocalDateTime dispatchTime,
        LocalDateTime finishTime,
        String resultDesc
) {
}
