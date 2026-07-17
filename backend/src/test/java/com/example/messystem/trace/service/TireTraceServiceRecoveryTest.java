package com.example.messystem.trace.service;

import com.example.messystem.trace.dao.TireTraceDao;
import com.example.messystem.trace.entity.TireTraceItem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TireTraceServiceRecoveryTest {
    @TempDir
    Path tempDirectory;

    @Test
    void shouldRebuildMissingTraceFilesWithoutChangingQrTarget() throws Exception {
        String targetUrl = "https://trace.example.com/trace-public?token=historical-token";
        TireTraceItem tire = new TireTraceItem(
                19L, "TIRE-20260707-000364-000019", "TRACE-TIRE-20260707-000364-000019",
                364L, "WO-364", 20L, "QI-020", 30L, 40L, "TYRE-2055516",
                "半钢子午线轮胎", "205/55R16", "成型一线", 50L, "成品仓",
                60L, "A-01", "BATCH-364", "IN_STOCK", LocalDateTime.now(),
                LocalDateTime.now(), "historical-token", targetUrl, 0);
        RecordingDao dao = new RecordingDao(tire);
        TireTraceService service = new TireTraceService(dao, new TraceFileService(tempDirectory));

        Path qrCode = service.qrCode(tire.tireId());

        assertTrue(Files.isRegularFile(qrCode));
        assertTrue(Files.size(qrCode) > 1000);
        assertEquals("TIRE-20260707-000364-000019/qrcode.png", dao.qrPath);
        assertEquals("TIRE-20260707-000364-000019/label.png", dao.labelPath);
        assertEquals("TIRE-20260707-000364-000019/product-info.pdf", dao.pdfPath);
        assertEquals(64, dao.labelHash.length());
        assertEquals(64, dao.pdfHash.length());
    }

    private static final class RecordingDao extends TireTraceDao {
        private final TireTraceItem tire;
        private String qrPath;
        private String labelPath;
        private String labelHash;
        private String pdfPath;
        private String pdfHash;

        private RecordingDao(TireTraceItem tire) {
            this.tire = tire;
        }

        @Override
        public Optional<TireTraceItem> findById(long tireId) {
            return tire.tireId() == tireId ? Optional.of(tire) : Optional.empty();
        }

        @Override
        public String findQrPath(long tireId) {
            return "missing/qrcode.png";
        }

        @Override
        public void updateGeneratedFiles(long tireId, String qrPath, String labelPath, String labelHash,
                String pdfPath, String pdfHash) throws SQLException {
            this.qrPath = qrPath;
            this.labelPath = labelPath;
            this.labelHash = labelHash;
            this.pdfPath = pdfPath;
            this.pdfHash = pdfHash;
        }
    }
}
