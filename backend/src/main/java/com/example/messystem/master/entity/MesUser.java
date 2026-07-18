/*
 * 答辩定位：主数据与用户 模块的 MesUser。
 * 分层职责：领域/传输模型：承载数据库字段或接口 JSON。Jackson 通过公开字段、构造器或 record 组件完成序列化与反序列化。
 * 典型调用链：PostgreSQL/JDBC <-> DAO <-> 当前模型 <-> Jackson JSON <-> Vue 页面。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.master.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * 主数据与用户 的数据模型；字段名与接口 JSON/数据库列保持可追踪关系，不在模型中实现业务规则。
 */
public class MesUser {
    /** 用户主键，用于关联账号、角色和审计信息。 */
    public Long userId;
    /** 登录账号，认证时作为用户唯一标识之一。 */
    public String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    /** 本次请求携带的明文密码；仅在认证边界短暂使用，不应持久化。 */
    public String password;
    /** realName 对应的展示名称。 */
    public String realName;
    /** roleCode 对应的稳定业务编码。 */
    public String roleCode;
    /** department 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String department;
    /** phone 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String phone;
    /** email 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String email;
    /** avatarUrl 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String avatarUrl;
    /** profileBio 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String profileBio;
    /** employeeNo 对应的业务单号，便于人工识别和检索。 */
    public String employeeNo;
    /** positionName 对应的展示名称。 */
    public String positionName;
    /** 是否启用；停用记录通常保留历史关联但不再参与新业务。 */
    public Integer enabled;
    /** 记录创建时间，用于排序、追溯和审计。 */
    public LocalDateTime createdAt;
    /** 记录最后更新时间。 */
    public LocalDateTime updatedAt;
    /** lastLoginAt 对应的业务时间点。 */
    public LocalDateTime lastLoginAt;
}
