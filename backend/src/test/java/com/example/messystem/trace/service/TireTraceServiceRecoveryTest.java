/*
 * 答辩定位：轮胎标签与公开追溯 模块的 TireTraceServiceRecoveryTest。
 * 分层职责：自动化回归测试：固定关键业务规则、接口契约和架构边界，防止重构时出现静默回归。
 * 典型调用链：Maven Surefire -> JUnit 5 -> 被测类；测试替身用于隔离远程数据库或文件系统。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
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

/**
 * 轮胎标签与公开追溯 的 TireTraceServiceRecoveryTest，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
class TireTraceServiceRecoveryTest {
    @TempDir
    Path tempDirectory;

    /**
     * 回归场景：验证 shouldRebuildMissingTraceFilesWithoutChangingQrTarget 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
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

        /**
         * 回归场景：验证 RecordingDao 所描述的行为。
         * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
         */
        private RecordingDao(TireTraceItem tire) {
            this.tire = tire;
        }

        /**
         * 回归场景：验证 findById 所描述的行为。
         * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
         */
        @Override
        public Optional<TireTraceItem> findById(long tireId) {
            return tire.tireId() == tireId ? Optional.of(tire) : Optional.empty();
        }

        /**
         * 回归场景：验证 findQrPath 所描述的行为。
         * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
         */
        @Override
        public String findQrPath(long tireId) {
            return "missing/qrcode.png";
        }

        /**
         * 回归场景：验证 updateGeneratedFiles 所描述的行为。
         * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
         */
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
