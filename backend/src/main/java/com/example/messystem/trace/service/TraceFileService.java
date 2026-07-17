package com.example.messystem.trace.service;

import com.example.messystem.trace.entity.TireTraceItem;
import com.example.messystem.trace.entity.TraceFileBundle;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class TraceFileService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Path storageRoot;

    public TraceFileService() {
        this(TraceRuntimeConfig.storageRoot());
    }

    public TraceFileService(Path storageRoot) {
        this.storageRoot = storageRoot.toAbsolutePath().normalize();
    }

    public TraceFileBundle generate(TireTraceItem tire) {
        Path directory = storageRoot.resolve(safe(tire.serialNo())).normalize();
        if (!directory.startsWith(storageRoot)) throw new IllegalStateException("追溯文件目录不合法");
        try {
            Files.createDirectories(directory);
            Path qrPath = directory.resolve("qrcode.png");
            Path labelPath = directory.resolve("label.png");
            Path pdfPath = directory.resolve("product-info.pdf");
            BufferedImage qrCode = generateQrCode(tire.targetUrl());
            ImageIO.write(qrCode, "PNG", qrPath.toFile());
            BufferedImage label = generateLabel(tire, qrCode);
            ImageIO.write(label, "PNG", labelPath.toFile());
            generatePdf(label, pdfPath);
            return new TraceFileBundle(qrPath, labelPath, pdfPath);
        } catch (Exception exception) {
            throw new IllegalStateException("轮胎二维码文件生成失败", exception);
        }
    }

    public Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("追溯文件路径为空");
        }
        Path result = storageRoot.resolve(relativePath).normalize();
        if (!result.startsWith(storageRoot) || !Files.isRegularFile(result)) {
            throw new IllegalArgumentException("追溯文件不存在");
        }
        return result;
    }

    public String relative(Path file) {
        return storageRoot.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    public String sha256(Path file) {
        try (InputStream input = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) digest.update(buffer, 0, count);
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("追溯文件校验失败", exception);
        }
    }

    public void deleteQuietly(TraceFileBundle bundle) {
        if (bundle == null) return;
        try { Files.deleteIfExists(bundle.pdfDocument()); } catch (IOException ignored) { }
        try { Files.deleteIfExists(bundle.labelImage()); } catch (IOException ignored) { }
        try { Files.deleteIfExists(bundle.qrCode()); } catch (IOException ignored) { }
    }

    private BufferedImage generateQrCode(String content) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 800, 800, Map.of(
                EncodeHintType.CHARACTER_SET, "UTF-8",
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN, 2
        ));
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private BufferedImage generateLabel(TireTraceItem tire, BufferedImage qrCode) {
        BufferedImage image = new BufferedImage(1200, 1700, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(12, 38, 75));
        graphics.fillRect(0, 0, image.getWidth(), 190);
        graphics.setColor(Color.WHITE);
        graphics.setFont(font(Font.BOLD, 54));
        drawCentered(graphics, "双星轮胎 · 单胎质量追溯", 105);
        graphics.setFont(font(Font.PLAIN, 25));
        drawCentered(graphics, "每条轮胎唯一编码 · 微信扫码查询", 155);

        graphics.setColor(new Color(25, 38, 60));
        graphics.setFont(font(Font.BOLD, 48));
        drawCentered(graphics, tire.productName() == null ? "轮胎产品" : tire.productName(), 270);
        graphics.setFont(font(Font.PLAIN, 30));
        drawCentered(graphics, value(tire.productModel(), "规格型号未填写"), 320);

        graphics.drawImage(qrCode, 200, 360, 800, 800, null);
        graphics.setColor(new Color(20, 91, 190));
        graphics.setFont(font(Font.BOLD, 34));
        drawCentered(graphics, tire.serialNo(), 1215);

        graphics.setColor(new Color(38, 50, 70));
        graphics.setFont(font(Font.PLAIN, 27));
        int y = 1280;
        y = drawLine(graphics, "追溯码", tire.traceCode(), y);
        y = drawLine(graphics, "生产工单", value(tire.workOrderNo(), String.valueOf(tire.workOrderId())), y);
        y = drawLine(graphics, "生产批次", value(tire.batchNo(), "—"), y);
        y = drawLine(graphics, "质检单", value(tire.inspectionNo(), String.valueOf(tire.inspectionId())), y);
        y = drawLine(graphics, "质检结果", "合格", y);
        y = drawLine(graphics, "入库仓库", value(tire.warehouseName(), "—"), y);
        drawLine(graphics, "入库时间", tire.inboundAt() == null ? "—" : TIME_FORMAT.format(tire.inboundAt()), y);
        graphics.dispose();
        return image;
    }

    private void generatePdf(BufferedImage label, Path output) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDImageXObject image = LosslessFactory.createFromImage(document, label);
            float margin = 24;
            float width = page.getMediaBox().getWidth() - margin * 2;
            float height = width * label.getHeight() / label.getWidth();
            if (height > page.getMediaBox().getHeight() - margin * 2) {
                height = page.getMediaBox().getHeight() - margin * 2;
                width = height * label.getWidth() / label.getHeight();
            }
            float x = (page.getMediaBox().getWidth() - width) / 2;
            float y = (page.getMediaBox().getHeight() - height) / 2;
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(image, x, y, width, height);
            }
            document.save(output.toFile());
        }
    }

    private int drawLine(Graphics2D graphics, String label, String value, int y) {
        graphics.setColor(new Color(105, 117, 135));
        graphics.drawString(label + "：", 120, y);
        graphics.setColor(new Color(28, 40, 58));
        graphics.drawString(value, 320, y);
        return y + 58;
    }

    private void drawCentered(Graphics2D graphics, String text, int y) {
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(text, (1200 - metrics.stringWidth(text)) / 2, y);
    }

    private Font font(int style, int size) {
        return new Font("Microsoft YaHei", style, size);
    }

    private String safe(String value) {
        return value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
