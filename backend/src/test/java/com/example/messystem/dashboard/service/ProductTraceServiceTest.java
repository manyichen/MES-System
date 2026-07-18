/*
 * 答辩定位：驾驶舱、反馈与产品追溯 模块的 ProductTraceServiceTest。
 * 分层职责：自动化回归测试：固定关键业务规则、接口契约和架构边界，防止重构时出现静默回归。
 * 典型调用链：Maven Surefire -> JUnit 5 -> 被测类；测试替身用于隔离远程数据库或文件系统。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.dashboard.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.dashboard.dao.ProductTraceDao;
import com.example.messystem.dashboard.dao.ProductTraceDao.TraceContext;
import com.example.messystem.dashboard.entity.MesProductTrace;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 驾驶舱、反馈与产品追溯 的 ProductTraceServiceTest，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
class ProductTraceServiceTest {

    /**
     * 回归场景：验证 shouldDeriveProductAndDefaultsFromValidatedWorkOrderChain 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @Test
    void shouldDeriveProductAndDefaultsFromValidatedWorkOrderChain() throws Exception {
        RecordingDao dao = new RecordingDao(new TraceContext(88L, "BATCH-WO-88"));
        ProductTraceService service = new ProductTraceService(dao);
        MesProductTrace request = new MesProductTrace(
                null, " ", 11L, 22L, 33L, null, "", null, null);

        long id = service.createProductTrace(request);

        assertEquals(901L, id);
        assertEquals(88L, dao.inserted.productId());
        assertEquals("BATCH-WO-88", dao.inserted.batchNo());
        assertEquals("NORMAL", dao.inserted.traceStatus());
        assertTrue(dao.inserted.traceCode().startsWith("TRACE-"));
        assertNotNull(dao.inserted.createdAt());
    }

    /**
     * 回归场景：验证 shouldRejectMismatchedProductAndBrokenOrderChain 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @Test
    void shouldRejectMismatchedProductAndBrokenOrderChain() {
        RecordingDao productMismatch = new RecordingDao(new TraceContext(88L, "BATCH-88"));
        ProductTraceService mismatchService = new ProductTraceService(productMismatch);
        MesProductTrace mismatch = new MesProductTrace(
                null, "TRACE-88", 11L, 22L, 33L, 99L, "BATCH-88", "NORMAL", null);

        assertThrows(BadRequestException.class, () -> mismatchService.createProductTrace(mismatch));

        RecordingDao missingChain = new RecordingDao(null);
        ProductTraceService missingService = new ProductTraceService(missingChain);
        MesProductTrace missing = new MesProductTrace(
                null, "TRACE-MISSING", 11L, 22L, 33L, null, "BATCH", "NORMAL", null);
        assertThrows(BadRequestException.class, () -> missingService.createProductTrace(missing));
    }

    /**
     * 回归场景：验证 shouldReturnNotFoundForUnknownTrace 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @Test
    void shouldReturnNotFoundForUnknownTrace() {
        ProductTraceService service = new ProductTraceService(new RecordingDao(new TraceContext(1L, "BATCH")));

        assertThrows(NotFoundException.class, () -> service.getProductTraceChain("TRACE-UNKNOWN"));
    }

    private static final class RecordingDao extends ProductTraceDao {
        private final TraceContext context;
        private MesProductTrace inserted;

        /**
         * 回归场景：验证 RecordingDao 所描述的行为。
         * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
         */
        private RecordingDao(TraceContext context) {
            this.context = context;
        }

        /**
         * 回归场景：验证 findContext 所描述的行为。
         * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
         */
        @Override
        public Optional<TraceContext> findContext(long orderId, long taskId, long workOrderId) {
            return Optional.ofNullable(context);
        }

        /**
         * 回归场景：验证 insert 所描述的行为。
         * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
         */
        @Override
        public long insert(MesProductTrace trace) {
            inserted = trace;
            return 901L;
        }

        /**
         * 回归场景：验证 findByTraceCode 所描述的行为。
         * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
         */
        @Override
        public Optional<MesProductTrace> findByTraceCode(String traceCode) {
            return Optional.empty();
        }

        /**
         * 回归场景：验证 findTraceChain 所描述的行为。
         * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
         */
        @Override
        public Map<String, Object> findTraceChain(MesProductTrace trace) throws SQLException {
            return Map.of("trace", trace);
        }
    }
}
