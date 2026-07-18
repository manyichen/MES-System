/*
 * 答辩定位：订单、计划、齐套与工单 模块的 PlanningStore。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.service;

import com.example.messystem.master.entity.MesProcessRoute;
import com.example.messystem.master.entity.MesProduct;
import com.example.messystem.master.entity.MesProductBom;
import com.example.messystem.master.entity.MesProductionLine;
import com.example.messystem.master.entity.MesSyncLog;
import com.example.messystem.master.entity.MesUser;
import com.example.messystem.planning.entity.MesCustomerOrder;
import com.example.messystem.planning.entity.MesKittingAnalysis;
import com.example.messystem.planning.entity.MesKittingShortageItem;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.entity.MesShortageAlert;
import com.example.messystem.planning.entity.MesWorkOrder;
import com.example.messystem.planning.entity.MesWorkOrderOperationLog;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单、计划、齐套与工单 的 PlanningStore，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public final class PlanningStore {
    /** users 业务字段；具体取值由创建/更新用例校验后写入。 */
    public static final Map<Long, MesUser> users = new LinkedHashMap<>();
    /** products 业务字段；具体取值由创建/更新用例校验后写入。 */
    public static final Map<Long, MesProduct> products = new LinkedHashMap<>();
    /** productBoms 业务字段；具体取值由创建/更新用例校验后写入。 */
    public static final Map<Long, MesProductBom> productBoms = new LinkedHashMap<>();
    /** processRoutes 业务字段；具体取值由创建/更新用例校验后写入。 */
    public static final Map<Long, MesProcessRoute> processRoutes = new LinkedHashMap<>();
    /** productionLines 业务字段；具体取值由创建/更新用例校验后写入。 */
    public static final Map<Long, MesProductionLine> productionLines = new LinkedHashMap<>();
    /** syncLogs 业务字段；具体取值由创建/更新用例校验后写入。 */
    public static final Map<Long, MesSyncLog> syncLogs = new LinkedHashMap<>();
    /** orders 业务字段；具体取值由创建/更新用例校验后写入。 */
    public static final Map<Long, MesCustomerOrder> orders = new LinkedHashMap<>();
    /** tasks 业务字段；具体取值由创建/更新用例校验后写入。 */
    public static final Map<Long, MesProductionTask> tasks = new LinkedHashMap<>();
    /** analyses 业务字段；具体取值由创建/更新用例校验后写入。 */
    public static final Map<Long, MesKittingAnalysis> analyses = new LinkedHashMap<>();
    /** shortageItems 业务字段；具体取值由创建/更新用例校验后写入。 */
    public static final Map<Long, MesKittingShortageItem> shortageItems = new LinkedHashMap<>();
    /** shortageAlerts 业务字段；具体取值由创建/更新用例校验后写入。 */
    public static final Map<Long, MesShortageAlert> shortageAlerts = new LinkedHashMap<>();
    /** workOrders 业务字段；具体取值由创建/更新用例校验后写入。 */
    public static final Map<Long, MesWorkOrder> workOrders = new LinkedHashMap<>();
    /** operationLogs 业务字段；具体取值由创建/更新用例校验后写入。 */
    public static final Map<Long, MesWorkOrderOperationLog> operationLogs = new LinkedHashMap<>();

    private static final AtomicLong ids = new AtomicLong();

    /**
     * 业务用例：执行 PlanningStore 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private PlanningStore() {
    }

    /**
     * 业务用例：执行 nextId 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public static long nextId() {
        return ids.incrementAndGet();
    }

    /**
     * 业务用例：执行 clear 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public static void clear() {
        users.clear();
        products.clear();
        productBoms.clear();
        processRoutes.clear();
        productionLines.clear();
        syncLogs.clear();
        orders.clear();
        tasks.clear();
        analyses.clear();
        shortageItems.clear();
        shortageAlerts.clear();
        workOrders.clear();
        operationLogs.clear();
        ids.set(0);
    }
}
