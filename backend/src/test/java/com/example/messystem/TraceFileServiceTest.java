/*
 * 答辩定位：MES 应用基础 模块的 TraceFileServiceTest。
 * 分层职责：自动化回归测试：固定关键业务规则、接口契约和架构边界，防止重构时出现静默回归。
 * 典型调用链：Maven Surefire -> JUnit 5 -> 被测类；测试替身用于隔离远程数据库或文件系统。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem;

import com.example.messystem.trace.entity.TireTraceItem;
import com.example.messystem.trace.entity.TraceFileBundle;
import com.example.messystem.trace.service.TraceFileService;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MES 应用基础 的 TraceFileServiceTest，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
class TraceFileServiceTest {
    @TempDir
    Path tempDirectory;

    /**
     * 回归场景：验证 shouldGenerateWechatReadableQrLabelAndPdfWithoutOnlineService 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @Test
    void shouldGenerateWechatReadableQrLabelAndPdfWithoutOnlineService() throws Exception {
        String targetUrl = "https://trace.example.com/trace-public?token=offline-test-token";
        TireTraceItem tire = new TireTraceItem(
                1L, "TIRE-20260712-WO001-000001", "TRACE-TIRE-20260712-WO001-000001",
                10L, "WO-001", 20L, "QI-001", 30L, 40L, "TYRE-2055516",
                "半钢子午线轮胎", "205/55R16", "成型一线", 50L, "成品仓",
                60L, "A-01", "BATCH-001", "IN_STOCK", LocalDateTime.now(),
                LocalDateTime.now(), "offline-test-token", targetUrl, 0
        );
        TraceFileService service = new TraceFileService(tempDirectory);

        TraceFileBundle files = service.generate(tire);

        assertTrue(Files.size(files.qrCode()) > 1000);
        assertTrue(Files.size(files.labelImage()) > 10000);
        assertTrue(Files.size(files.pdfDocument()) > 10000);
        BufferedImage image = ImageIO.read(files.qrCode().toFile());
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        assertEquals(targetUrl, new MultiFormatReader().decode(bitmap).getText());
        try (var document = Loader.loadPDF(files.pdfDocument().toFile())) {
            assertEquals(1, document.getNumberOfPages());
        }
        assertEquals(64, service.sha256(files.pdfDocument()).length());
    }
}
