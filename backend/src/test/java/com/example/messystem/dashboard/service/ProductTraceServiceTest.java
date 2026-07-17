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

class ProductTraceServiceTest {

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

    @Test
    void shouldReturnNotFoundForUnknownTrace() {
        ProductTraceService service = new ProductTraceService(new RecordingDao(new TraceContext(1L, "BATCH")));

        assertThrows(NotFoundException.class, () -> service.getProductTraceChain("TRACE-UNKNOWN"));
    }

    private static final class RecordingDao extends ProductTraceDao {
        private final TraceContext context;
        private MesProductTrace inserted;

        private RecordingDao(TraceContext context) {
            this.context = context;
        }

        @Override
        public Optional<TraceContext> findContext(long orderId, long taskId, long workOrderId) {
            return Optional.ofNullable(context);
        }

        @Override
        public long insert(MesProductTrace trace) {
            inserted = trace;
            return 901L;
        }

        @Override
        public Optional<MesProductTrace> findByTraceCode(String traceCode) {
            return Optional.empty();
        }

        @Override
        public Map<String, Object> findTraceChain(MesProductTrace trace) throws SQLException {
            return Map.of("trace", trace);
        }
    }
}
