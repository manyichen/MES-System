/*
 * 答辩定位：主数据与用户 模块的 MasterDataResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.master.controller;

import com.example.messystem.common.ResourceSupport;
import com.example.messystem.master.entity.MesProcessRoute;
import com.example.messystem.master.entity.MesProduct;
import com.example.messystem.master.entity.MesProductBom;
import com.example.messystem.master.entity.MesProductionLine;
import com.example.messystem.master.service.MasterDataService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** 承载产品、产线和工艺等主数据接口契约的 JAX-RS 控制器。 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MasterDataResource {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final MasterDataService service = new MasterDataService();

    /**
     * 接口：GET /api/products。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/products")
    public Response listProducts() {
        return ResourceSupport.ok(service.listProducts());
    }

    /**
     * 接口：POST /api/products。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/products")
    public Response createProduct(MesProduct product) {
        try {
            return ResourceSupport.created("product created", service.createProduct(product));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：PUT /api/products/{id}。
     * 用例：更新业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @PUT
    @Path("/products/{id}")
    public Response updateProduct(@PathParam("id") long productId, MesProduct product) {
        try {
            return ResourceSupport.action("product updated", service.updateProduct(productId, product));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：DELETE /api/products/{id}。
     * 用例：停用业务对象；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @DELETE
    @Path("/products/{id}")
    public Response disableProduct(@PathParam("id") long productId) {
        try {
            return ResourceSupport.action("product disabled", service.disableProduct(productId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：GET /api/products/{id}/bom。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/products/{id}/bom")
    public Response listBom(@PathParam("id") long productId) {
        try {
            return ResourceSupport.ok(service.listBom(productId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：POST /api/products/{id}/bom。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/products/{id}/bom")
    public Response createBom(@PathParam("id") long productId, MesProductBom bom) {
        try {
            return ResourceSupport.created("bom item created", service.createBom(productId, bom));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：GET /api/product-boms。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/product-boms")
    public Response listAllBom() {
        return ResourceSupport.ok(service.listAllBom());
    }

    /**
     * 接口：POST /api/product-boms。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/product-boms")
    public Response createBom(MesProductBom bom) {
        try {
            return ResourceSupport.created("bom item created", service.createBom(bom.productId, bom));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：PUT /api/product-boms/{id}。
     * 用例：更新业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @PUT
    @Path("/product-boms/{id}")
    public Response updateBom(@PathParam("id") long bomId, MesProductBom bom) {
        try {
            return ResourceSupport.action("bom item updated", service.updateBom(bomId, bom));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：DELETE /api/product-boms/{id}。
     * 用例：删除业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @DELETE
    @Path("/product-boms/{id}")
    public Response deleteBom(@PathParam("id") long bomId) {
        try {
            service.deleteBom(bomId);
            return ResourceSupport.action("bom item deleted", null);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：GET /api/process-routes。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/process-routes")
    public Response listProcessRoutes() {
        return ResourceSupport.ok(service.listProcessRoutes());
    }

    /**
     * 接口：POST /api/process-routes。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/process-routes")
    public Response createProcessRoute(MesProcessRoute route) {
        try {
            return ResourceSupport.created("process route created", service.createProcessRoute(route));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：PUT /api/process-routes/{id}。
     * 用例：更新业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @PUT
    @Path("/process-routes/{id}")
    public Response updateProcessRoute(@PathParam("id") long id, MesProcessRoute route) {
        try {
            return ResourceSupport.action("process route updated", service.updateProcessRoute(id, route));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：DELETE /api/process-routes/{id}。
     * 用例：删除业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @DELETE
    @Path("/process-routes/{id}")
    public Response deleteProcessRoute(@PathParam("id") long id) {
        try {
            service.deleteProcessRoute(id);
            return ResourceSupport.action("process route deleted", null);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：GET /api/production-lines。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/production-lines")
    public Response listProductionLines() {
        return ResourceSupport.ok(service.listProductionLines());
    }

    /**
     * 接口：POST /api/production-lines。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/production-lines")
    public Response createProductionLine(MesProductionLine line) {
        try {
            return ResourceSupport.created("production line created", service.createProductionLine(line));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：PUT /api/production-lines/{id}。
     * 用例：更新业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @PUT
    @Path("/production-lines/{id}")
    public Response updateProductionLine(@PathParam("id") long lineId, MesProductionLine line) {
        try {
            return ResourceSupport.action("production line updated", service.updateProductionLine(lineId, line));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：DELETE /api/production-lines/{id}。
     * 用例：停用业务对象；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @DELETE
    @Path("/production-lines/{id}")
    public Response disableProductionLine(@PathParam("id") long lineId) {
        try {
            return ResourceSupport.action("production line disabled", service.disableProductionLine(lineId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：GET /api/sync-logs。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/sync-logs")
    public Response listSyncLogs() {
        return ResourceSupport.ok(service.listSyncLogs());
    }
}
