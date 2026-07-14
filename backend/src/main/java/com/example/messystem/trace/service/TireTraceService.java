package com.example.messystem.trace.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
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
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TireTraceService {
    private static final int MAX_BATCH_SIZE = 1000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final TireTraceDao dao;
    private final TraceFileService fileService;

    public TireTraceService() {
        this(new TireTraceDao(), new TraceFileService());
    }

    TireTraceService(TireTraceDao dao, TraceFileService fileService) {
        this.dao = dao;
        this.fileService = fileService;
    }

    public TireGenerationResult generate(TireGenerationRequest request, long createdBy) {
        validate(request);
        List<TraceFileBundle> generatedFiles = new ArrayList<>();
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                TireGenerationContext context = dao.lockGenerationContext(connection, request.workOrderId(),
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
                    String targetUrl = TraceRuntimeConfig.publicBaseUrl(request.publicBaseUrl()) + "/trace-public.html?token="
                            + URLEncoder.encode(token, StandardCharsets.UTF_8);
                    TireTraceItem inserted = dao.insertTire(connection, context, serialNo, traceCode, createdBy);
                    TireTraceItem tire = withQrCode(inserted, token, targetUrl);
                    TraceFileBundle files = fileService.generate(tire);
                    generatedFiles.add(files);
                    dao.insertQrCode(connection, tire.tireId(), token, targetUrl, fileService.relative(files.qrCode()));
                    dao.insertDocument(connection, tire.tireId(), "LABEL_PNG", "label.png",
                            fileService.relative(files.labelImage()), fileService.sha256(files.labelImage()));
                    dao.insertDocument(connection, tire.tireId(), "PDF", "product-info.pdf",
                            fileService.relative(files.pdfDocument()), fileService.sha256(files.pdfDocument()));
                    tires.add(tire);
                }
                connection.commit();
                int remaining = available - request.quantity();
                return new TireGenerationResult(request.quantity(), tires.size(), context.qualifiedQuantity(), remaining, tires);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                generatedFiles.forEach(fileService::deleteQuietly);
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("数据库操作失败：" + exception.getMessage(), exception);
        }
    }

    public List<TireTraceItem> list() {
        try {
            return dao.findAll();
        } catch (SQLException exception) {
            throw new IllegalStateException("数据库操作失败：" + exception.getMessage(), exception);
        }
    }

    public List<TireTraceItem> list(Set<Long> warehouseIds) {
        List<TireTraceItem> rows = list();
        if (warehouseIds == null) return rows;
        return rows.stream().filter(item -> item.warehouseId() != null && warehouseIds.contains(item.warehouseId())).toList();
    }

    public TireTraceItem requireById(long tireId) {
        try {
            return dao.findById(tireId).orElseThrow(() -> new NotFoundException("轮胎实例不存在"));
        } catch (SQLException exception) {
            throw new IllegalStateException("数据库操作失败：" + exception.getMessage(), exception);
        }
    }

    public TireTraceItem requireByToken(String token) {
        if (token == null || token.isBlank()) throw new BadRequestException("二维码访问令牌不能为空");
        try {
            return dao.findByToken(token).orElseThrow(() -> new NotFoundException("轮胎二维码不存在或已经失效"));
        } catch (SQLException exception) {
            throw new IllegalStateException("数据库操作失败：" + exception.getMessage(), exception);
        }
    }

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

    public Path qrCode(long tireId) {
        try {
            return fileService.resolve(dao.findQrPath(tireId));
        } catch (SQLException exception) {
            throw new IllegalStateException("数据库操作失败：" + exception.getMessage(), exception);
        }
    }

    public Path document(long tireId, String type) {
        try {
            return fileService.resolve(dao.findDocumentPath(tireId, type));
        } catch (SQLException exception) {
            throw new IllegalStateException("数据库操作失败：" + exception.getMessage(), exception);
        }
    }

    public Path publicDocument(String token) {
        return document(requireByToken(token).tireId(), "PDF");
    }

    public void recordPrint(long tireId, long printedBy, String remark) {
        requireById(tireId);
        try {
            dao.recordPrint(tireId, printedBy, remark == null || remark.isBlank() ? "二维码标签打印" : remark.trim());
        } catch (SQLException exception) {
            throw new IllegalStateException("数据库操作失败：" + exception.getMessage(), exception);
        }
    }

    private void validate(TireGenerationRequest request) {
        if (request == null) throw new BadRequestException("轮胎二维码生成参数不能为空");
        if (request.workOrderId() == null || request.workOrderId() <= 0) throw new BadRequestException("生产工单ID不能为空");
        if (request.inspectionId() == null || request.inspectionId() <= 0) throw new BadRequestException("质检单ID不能为空");
        if (request.warehouseId() == null || request.warehouseId() <= 0) throw new BadRequestException("入库仓库ID不能为空");
        if (request.quantity() == null || request.quantity() <= 0) throw new BadRequestException("轮胎数量必须大于0");
        if (request.quantity() > MAX_BATCH_SIZE) throw new BadRequestException("单次最多生成 " + MAX_BATCH_SIZE + " 条轮胎二维码");
    }

    private String buildSerialNo(TireGenerationContext context, int sequence) {
        return "TIRE-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%06d", context.workOrderId())
                + "-" + String.format("%06d", sequence);
    }

    private String nextToken() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

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
