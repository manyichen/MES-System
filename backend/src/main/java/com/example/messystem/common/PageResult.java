/*
 * 答辩定位：公共基础设施 模块的 PageResult。
 * 分层职责：公共支撑代码：提供多个业务模块共享的响应、异常、编码或工具能力。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.common;

import java.util.List;

/**
 * 公共基础设施 的 PageResult，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class PageResult<T> {
    /** items 业务字段；具体取值由创建/更新用例校验后写入。 */
    public List<T> items;
    /** page 业务字段；具体取值由创建/更新用例校验后写入。 */
    public int page;
    /** pageSize 业务字段；具体取值由创建/更新用例校验后写入。 */
    public int pageSize;
    /** total 业务字段；具体取值由创建/更新用例校验后写入。 */
    public long total;

    /**
     * 公共能力：执行 PageResult 对应的业务步骤。
     * 由 PageResult 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public PageResult() {
    }

    /**
     * 公共能力：执行 PageResult 对应的业务步骤。
     * 由 PageResult 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public PageResult(List<T> items, int page, int pageSize, long total) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }
}
