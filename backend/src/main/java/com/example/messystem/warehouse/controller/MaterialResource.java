/*
 * 答辩定位：仓储、领料、拣货与机器人物流 模块的 MaterialResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.warehouse.controller;

import com.example.messystem.common.ResourceSupport;
import com.example.messystem.warehouse.entity.MesMaterial;
import com.example.messystem.warehouse.service.WarehouseService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** 承载 /materials 物料接口契约的 JAX-RS 控制器。 */
@Path("/materials")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaterialResource {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final WarehouseService service = new WarehouseService();

    /**
     * 接口：GET /api/materials。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public Response list() {
        return ResourceSupport.ok(service.listMaterials());
    }

    /**
     * 接口：GET /api/materials/{id}。
     * 用例：查询单条记录或详情；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getMaterial(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：POST /api/materials。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    public Response create(MesMaterial material) {
        try {
            return ResourceSupport.created("物料已创建", service.createMaterial(material));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：PUT /api/materials/{id}。
     * 用例：更新业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") long id, MesMaterial material) {
        try {
            return ResourceSupport.action("物料已更新", service.updateMaterial(id, material));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：DELETE /api/materials/{id}。
     * 用例：删除业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        try {
            service.deleteMaterial(id);
            return ResourceSupport.action("物料已删除", null);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
