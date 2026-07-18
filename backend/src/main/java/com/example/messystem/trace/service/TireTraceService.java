/*
 * 答辩定位：轮胎标签与公开追溯 模块的 TireTraceService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.trace.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.trace.dao.TireTraceDao;
import com.example.messystem.trace.entity.TireGenerationContext;
import com.example.messystem.trace.entity.TireGenerationRequest;
import com.example.messystem.trace.entity.TireGenerationResult;
import com.example.messystem.trace.entity.TireTraceItem;
import com.example.messystem.trace.entity.TraceFileBundle;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 轮胎标签与公开追溯 的 TireTraceService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class TireTraceService {
    private static final int MAX_BATCH_SIZE = 1000;
    private static final SecureRandom RANDOM = new SecureRandom();
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final TireTraceDao dao;
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final TraceFileService fileService;

    /**
     * 业务用例：执行 TireTraceService 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public TireTraceService() {
        this(new TireTraceDao(), new TraceFileService());
    }

    TireTraceService(TireTraceDao dao, TraceFileService fileService) {
        this.dao = dao;
        this.fileService = fileService;
    }

    /**
     * 将轮胎身份、二维码和产品文档作为一个逻辑批次生成。
     * 数据库事务由 DAO 管理，任一持久化步骤失败时由本服务删除已生成文件。
     */
    public TireGenerationResult generate(TireGenerationRequest request, long createdBy) {
        validate(request);
        List<TraceFileBundle> generatedFiles = new ArrayList<>();
        try {
            return dao.inTransaction(transaction -> {
                TireGenerationContext context = dao.lockGenerationContext(transaction, request.workOrderId(),
                        request.inspectionId(), request.warehouseId(), request.locationId());
                int available = context.qualifiedQuantity() - context.generatedQuantity();
                if (available <= 0) throw new BadRequestException("该工单的合格轮胎已经全部生成二维码");
                if (request.quantity() > available) {
                    throw new BadRequestException("本次生成数量超过剩余合格数量，最多可生成 " + available + " 条");
                }

                List<TireTraceItem> tires = new ArrayList<>();
                for (int index = 1; index <= request.quantity(); index++) {
                    int sequence = context.generatedQuantity() + index;
                    String serialNo = buildSerialNo(context, sequence);
                    String traceCode = "TRACE-" + serialNo;
                    String token = nextToken();
                    String targetUrl = TraceRuntimeConfig.publicBaseUrl(request.publicBaseUrl()) + "/trace-public?token="
                            + URLEncoder.encode(token, StandardCharsets.UTF_8);
                    TireTraceItem inserted = dao.insertTire(transaction, context, serialNo, traceCode, createdBy);
                    TireTraceItem tire = withQrCode(inserted, token, targetUrl);
                    TraceFileBundle files = fileService.generate(tire);
                    generatedFiles.add(files);
                    dao.insertQrCode(transaction, tire.tireId(), token, targetUrl, fileService.relative(files.qrCode()));
                    dao.insertDocument(transaction, tire.tireId(), "LABEL_PNG", "label.png",
                            fileService.relative(files.labelImage()), fileService.sha256(files.labelImage()));
                    dao.insertDocument(transaction, tire.tireId(), "PDF", "product-info.pdf",
                            fileService.relative(files.pdfDocument()), fileService.sha256(files.pdfDocument()));
                    tires.add(tire);
                }
                int remaining = available - request.quantity();
                return new TireGenerationResult(request.quantity(), tires.size(), context.qualifiedQuantity(), remaining, tires);
            });
        } catch (SQLException exception) {
            generatedFiles.forEach(fileService::deleteQuietly);
            throw new IllegalStateException("数据库操作失败：" + exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            generatedFiles.forEach(fileService::deleteQuietly);
            throw exception;
        }
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<TireTraceItem> list() {
        try {
            return dao.findAll();
        } catch (SQLException exception) {
            throw new IllegalStateException("数据库操作失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<TireTraceItem> list(Set<Long> warehouseIds) {
        List<TireTraceItem> rows = list();
        if (warehouseIds == null) return rows;
        return rows.stream().filter(item -> item.warehouseId() != null && warehouseIds.contains(item.warehouseId())).toList();
    }

    /**
     * 业务用例：执行 requireById 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public TireTraceItem requireById(long tireId) {
        try {
            return dao.findById(tireId).orElseThrow(() -> new NotFoundException("轮胎实例不存在"));
        } catch (SQLException exception) {
            throw new IllegalStateException("数据库操作失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 业务用例：执行 requireByToken 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public TireTraceItem requireByToken(String token) {
        if (token == null || token.isBlank()) throw new BadRequestException("二维码访问令牌不能为空");
        try {
            return dao.findByToken(token).orElseThrow(() -> new NotFoundException("轮胎二维码不存在或已经失效"));
        } catch (SQLException exception) {
            throw new IllegalStateException("数据库操作失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 业务用例：执行 publicView 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public Map<String, Object> publicView(String token) {
        TireTraceItem tire = requireByToken(token);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serialNo", tire.serialNo());
        result.put("traceCode", tire.traceCode());
        result.put("productCode", tire.productCode());
        result.put("productName", tire.productName());
        result.put("productModel", tire.productModel());
        result.put("workOrderNo", tire.workOrderNo());
        result.put("inspectionNo", tire.inspectionNo());
        result.put("productionLine", tire.productionLine());
        result.put("batchNo", tire.batchNo());
        result.put("tireStatus", tire.tireStatus());
        result.put("inspectionResult", "合格");
        result.put("qualifiedAt", tire.qualifiedAt());
        result.put("inboundAt", tire.inboundAt());
        result.put("warehouseName", tire.warehouseName());
        result.put("locationName", tire.locationName());
        result.put("documentUrl", "/api/public/tire-traces/" + token + "/document");
        return result;
    }

    /**
     * 业务用例：读取二维码文件。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public Path qrCode(long tireId) {
        try {
            return fileService.resolve(dao.findQrPath(tireId));
        } catch (NotFoundException | IllegalArgumentException exception) {
            return repairFiles(tireId).qrCode();
        } catch (SQLException exception) {
            throw new IllegalStateException("数据库操作失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 业务用例：执行 document 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public Path document(long tireId, String type) {
        try {
            return fileService.resolve(dao.findDocumentPath(tireId, type));
        } catch (NotFoundException | IllegalArgumentException exception) {
            TraceFileBundle files = repairFiles(tireId);
            return "LABEL_PNG".equals(type) ? files.labelImage() : files.pdfDocument();
        } catch (SQLException exception) {
            throw new IllegalStateException("数据库操作失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 业务用例：执行 publicDocument 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public Path publicDocument(String token) {
        return document(requireByToken(token).tireId(), "PDF");
    }

    /**
     * 业务用例：执行 recordPrint 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public void recordPrint(long tireId, long printedBy, String remark) {
        requireById(tireId);
        try {
            dao.recordPrint(tireId, printedBy, remark == null || remark.isBlank() ? "二维码标签打印" : remark.trim());
        } catch (SQLException exception) {
            throw new IllegalStateException("数据库操作失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 业务用例：执行 repairFiles 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private synchronized TraceFileBundle repairFiles(long tireId) {
        TireTraceItem tire = requireById(tireId);
        if (tire.targetUrl() == null || tire.targetUrl().isBlank()) {
            throw new NotFoundException("轮胎二维码元数据不完整，无法恢复追溯文件");
        }
        TraceFileBundle files = fileService.generate(tire);
        try {
            dao.updateGeneratedFiles(
                    tireId,
                    fileService.relative(files.qrCode()),
                    fileService.relative(files.labelImage()),
                    fileService.sha256(files.labelImage()),
                    fileService.relative(files.pdfDocument()),
                    fileService.sha256(files.pdfDocument()));
            return files;
        } catch (SQLException exception) {
            fileService.deleteQuietly(files);
            throw new IllegalStateException("追溯文件恢复记录保存失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 业务用例：校验业务输入与约束。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private void validate(TireGenerationRequest request) {
        if (request == null) throw new BadRequestException("轮胎二维码生成参数不能为空");
        if (request.workOrderId() == null || request.workOrderId() <= 0) throw new BadRequestException("生产工单ID不能为空");
        if (request.inspectionId() == null || request.inspectionId() <= 0) throw new BadRequestException("质检单ID不能为空");
        if (request.warehouseId() == null || request.warehouseId() <= 0) throw new BadRequestException("入库仓库ID不能为空");
        if (request.quantity() == null || request.quantity() <= 0) throw new BadRequestException("轮胎数量必须大于0");
        if (request.quantity() > MAX_BATCH_SIZE) throw new BadRequestException("单次最多生成 " + MAX_BATCH_SIZE + " 条轮胎二维码");
    }

    /**
     * 业务用例：执行 buildSerialNo 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private String buildSerialNo(TireGenerationContext context, int sequence) {
        return "TIRE-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%06d", context.workOrderId())
                + "-" + String.format("%06d", sequence);
    }

    /**
     * 业务用例：执行 nextToken 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private String nextToken() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 业务用例：执行 withQrCode 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private TireTraceItem withQrCode(TireTraceItem tire, String token, String targetUrl) {
        return new TireTraceItem(
                tire.tireId(), tire.serialNo(), tire.traceCode(), tire.workOrderId(), tire.workOrderNo(),
                tire.inspectionId(), tire.inspectionNo(), tire.workReportId(), tire.productId(), tire.productCode(),
                tire.productName(), tire.productModel(), tire.productionLine(), tire.warehouseId(), tire.warehouseName(),
                tire.locationId(), tire.locationName(), tire.batchNo(), tire.tireStatus(), tire.qualifiedAt(),
                tire.inboundAt(), token, targetUrl, 0
        );
    }
}
