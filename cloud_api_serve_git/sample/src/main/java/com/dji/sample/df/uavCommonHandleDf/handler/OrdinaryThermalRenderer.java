package com.dji.sample.df.uavCommonHandleDf.handler;

import com.dji.sample.df.temperatureMeasurementDF.modol.TemResultEntity;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.Locale;

/** Local display copy only; a re-encoded JPEG is never a radiometric source. */
@lombok.extern.slf4j.Slf4j
public final class OrdinaryThermalRenderer {
    private OrdinaryThermalRenderer() { }

    public static void annotate(Path original, TemResultEntity temperature) throws IOException {
        if (temperature == null || temperature.getError() != null && !temperature.getError().isBlank()
                || temperature.getMax_tem() == null || !Double.isFinite(temperature.getMax_tem())
                || temperature.getMin_tem() == null || !Double.isFinite(temperature.getMin_tem())
                || temperature.getMin_tem() > temperature.getMax_tem())
            throw new IOException("测温结果缺失、失败或温度值无效，无法标注");
        checkOrientation(original);
        BufferedImage image = ImageIO.read(original.toFile());
        Integer matrixWidth = temperature.getMatrixWidth(), matrixHeight = temperature.getMatrixHeight();
        if (image == null || matrixWidth == null || matrixHeight == null || matrixWidth <= 0 || matrixHeight <= 0)
            throw new IOException("图片无法读取或温度矩阵尺寸无效，跳过标注");
        if ((long) image.getWidth() * matrixHeight != (long) image.getHeight() * matrixWidth)
            throw new IOException("图片与温度矩阵宽高比例不一致，跳过标注：图片=" + image.getWidth() + "x" + image.getHeight()
                    + "，温度矩阵=" + matrixWidth + "x" + matrixHeight);
        Integer matrixX = temperature.getMaxX(), matrixY = temperature.getMaxY();
        if (matrixX == null || matrixY == null || matrixX < 0 || matrixY < 0 || matrixX >= matrixWidth || matrixY >= matrixHeight)
            throw new IOException("最高温坐标缺失或超出温度矩阵范围，跳过标注");
        int x = (int) ((long) matrixX * image.getWidth() / matrixWidth);
        int y = (int) ((long) matrixY * image.getHeight() / matrixHeight);
        log.info("红外最高温坐标映射，图片={}，矩阵尺寸={}x{}，图片尺寸={}x{}，矩阵坐标=({}, {})，标注坐标=({}, {})",
                original.getFileName(), matrixWidth, matrixHeight, image.getWidth(), image.getHeight(), matrixX, matrixY, x, y);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(14, image.getWidth() / 32)));
            FontMetrics metrics = g.getFontMetrics();
            String header = String.format(Locale.ROOT, "Max: %.1f C    Min: %.1f C", temperature.getMax_tem(), temperature.getMin_tem());
            while (metrics.stringWidth(header) > image.getWidth() - 8 && g.getFont().getSize() > 10) {
                g.setFont(g.getFont().deriveFont((float) g.getFont().getSize() - 1)); metrics = g.getFontMetrics();
            }
            g.setColor(new Color(0, 0, 0, 190));
            g.fillRect(0, 0, image.getWidth(), metrics.getHeight() + 8);
            g.setColor(Color.WHITE); g.drawString(header, 4, metrics.getAscent() + 4);
            g.setStroke(new BasicStroke(4)); g.setColor(Color.BLACK);
            g.drawLine(x - 9, y, x + 9, y); g.drawLine(x, y - 9, x, y + 9);
            g.setStroke(new BasicStroke(2)); g.setColor(Color.YELLOW);
            g.drawLine(x - 9, y, x + 9, y); g.drawLine(x, y - 9, x, y + 9);
            String label = String.format(Locale.ROOT, "%.1f C", temperature.getMax_tem());
            int tx = Math.max(2, Math.min(x + 12, image.getWidth() - metrics.stringWidth(label) - 2));
            int ty = Math.max(metrics.getAscent(), Math.min(y - 12, image.getHeight() - metrics.getDescent()));
            g.setColor(Color.BLACK); g.drawString(label, tx + 1, ty + 1);
            g.setColor(Color.YELLOW); g.drawString(label, tx, ty);
        } finally { g.dispose(); }
        Path staging = Files.createTempFile(original.getParent(), ".thermal-", ".jpg");
        try {
            if (!ImageIO.write(image, "jpg", staging.toFile())) throw new IOException("Cannot encode annotated JPEG");
            // No non-atomic fallback: on unsupported filesystems retain the downloaded original.
            Files.move(staging, original, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            log.info("红外结果图绘制完成，图片={}，最高温={}℃，最低温={}℃，最高温坐标=({}, {})，图片尺寸={}x{}",
                    original.getFileName(), temperature.getMax_tem(), temperature.getMin_tem(), x, y, image.getWidth(), image.getHeight());
        } finally { Files.deleteIfExists(staging); }
    }

    private static void checkOrientation(Path file) throws IOException {
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (input.readUnsignedShort() != 0xffd8) throw new IOException("Expected JPEG");
            while (true) {
                if (input.readUnsignedByte() != 0xff) throw new IOException("Invalid JPEG marker");
                int marker;
                do { marker = input.readUnsignedByte(); } while (marker == 0xff);
                if (marker == 0xda || marker == 0xd9) return;
                int length = input.readUnsignedShort() - 2;
                if (length < 0) throw new IOException("Invalid JPEG segment");
                byte[] segment = new byte[length]; input.readFully(segment);
                if (marker != 0xe1 || length < 6 || segment[0] != 'E' || segment[1] != 'x'
                        || segment[2] != 'i' || segment[3] != 'f' || segment[4] != 0 || segment[5] != 0) continue;
                try {
                    ByteBuffer tiff = ByteBuffer.wrap(segment, 6, length - 6).slice();
                    short order = tiff.getShort(0);
                    if (order != 0x4949 && order != 0x4d4d) throw new IOException("Invalid EXIF byte order");
                    tiff.order(order == 0x4949 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
                    if (tiff.getShort(2) != 42) throw new IOException("Invalid EXIF header");
                    int offset = tiff.getInt(4);
                    int count = Short.toUnsignedInt(tiff.getShort(offset));
                    for (int i = 0; i < count; i++) {
                        int entry = Math.addExact(offset + 2, Math.multiplyExact(i, 12));
                        if (Short.toUnsignedInt(tiff.getShort(entry)) == 0x112) {
                            if (tiff.getShort(entry + 2) != 3 || tiff.getInt(entry + 4) != 1 || tiff.getShort(entry + 8) != 1)
                                throw new IOException("Unsupported EXIF orientation");
                        }
                    }
                } catch (IndexOutOfBoundsException | ArithmeticException e) { throw new IOException("Invalid EXIF orientation metadata", e); }
            }
        }
    }
}
