/*
 * 答辩定位：质检、质量追溯与返工 模块的 ReworkOrderService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.quality.service;

import com.example.messystem.quality.dao.ReworkOrderDao;
import com.example.messystem.quality.entity.MesReworkOrder;
import com.example.messystem.common.BadRequestException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 质检、质量追溯与返工 的 ReworkOrderService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class ReworkOrderService {

    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final ReworkOrderDao dao = new ReworkOrderDao();

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public long createReworkOrder(MesReworkOrder order) throws SQLException {
        return dao.insert(order);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public Optional<MesReworkOrder> getReworkOrder(long id) throws SQLException {
        return dao.findById(id);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesReworkOrder> listReworkOrders() throws SQLException {
        return dao.findAll();
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesReworkOrder> listReworkOrdersByInspection(long inspectionId) throws SQLException {
        return dao.findByInspectionId(inspectionId);
    }

    /**
     * 业务用例：更新业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public boolean updateStatus(long id, String status) throws SQLException {
        return dao.updateStatus(id, status);
    }

    /**
     * 业务用例：派发业务任务。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public boolean dispatch(long id) throws SQLException {
        if (!dao.updateStatus(id, "DISPATCHED", "PLANNED")) {
            throw new BadRequestException("只有已由 PMC 排产的返工单才能派发");
        }
        return true;
    }

    /**
     * 业务用例：完成业务任务。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public boolean finish(long id) throws SQLException {
        if (!dao.updateStatus(id, "FINISHED", "DISPATCHED")) {
            throw new BadRequestException("只有已派发的返工单才能完成");
        }
        return true;
    }
}
