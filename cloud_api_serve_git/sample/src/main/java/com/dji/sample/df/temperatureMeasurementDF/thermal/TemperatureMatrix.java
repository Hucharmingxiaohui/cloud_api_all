package com.dji.sample.df.temperatureMeasurementDF.thermal;

import com.dji.sample.df.temperatureMeasurementDF.modol.TemParamEntity;
import com.dji.sample.df.temperatureMeasurementDF.modol.TemResultEntity;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

final class TemperatureMatrix {
    private final int width, height;
    private final float[] values;
    private final double min, max;

    private TemperatureMatrix(Raster raster) throws IOException {
        width = raster.getWidth(); height = raster.getHeight();
        values = new float[width * height];
        double minimum = Double.POSITIVE_INFINITY, maximum = Double.NEGATIVE_INFINITY;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float value = raster.getSampleFloat(x, y, 0);
                // The tool documents ~-273 C as its no-data floor; never publish it as a measurement.
                if (!Float.isFinite(value) || value <= -273.0f) throw new IOException("TIFF contains invalid/no-data temperatures");
                values[y * width + x] = value;
                minimum = Math.min(minimum, value); maximum = Math.max(maximum, value);
            }
        }
        min = minimum; max = maximum;
    }

    static TemperatureMatrix read(Path file, long maxBytes, long maxPixels) throws IOException {
        if (Files.size(file) > maxBytes) throw new IOException("Temperature TIFF exceeds thermal.cli.max-file-bytes");
        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile())) {
            if (input == null) throw new IOException("Cannot open temperature TIFF");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unsupported temperature TIFF");
            ImageReader reader = readers.next();
            try {
                if (!reader.getFormatName().equalsIgnoreCase("TIFF") && !reader.getFormatName().equalsIgnoreCase("tif"))
                    throw new IOException("Expected float32 Celsius TIFF; reader reports " + reader.getFormatName());
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > maxPixels) throw new IOException("Temperature TIFF exceeds thermal.cli.max-pixels");
                var type = reader.getRawImageType(0);
                if (type == null || type.getSampleModel().getDataType() != DataBuffer.TYPE_FLOAT
                        || type.getSampleModel().getNumBands() != 1 || type.getSampleModel().getSampleSize(0) != 32) {
                    throw new IOException("Expected single-band float32 Celsius TIFF, not integer/view pixels");
                }
                return new TemperatureMatrix(reader.read(0).getRaster());
            } finally { reader.dispose(); }
        }
    }

    TemResultEntity measureWholeImage() {
        TemParamEntity region = new TemParamEntity();
        region.setLeft_top_x(0); region.setLeft_top_y(0);
        region.setRight_bottom_x(width - 1); region.setRight_bottom_y(height - 1);
        return measure(region);
    }

    TemResultEntity measure(TemParamEntity p) {
        if (p == null) throw new IllegalArgumentException("测温参数不能为空");
        TemResultEntity result = new TemResultEntity();
        if (p.getPoint_x() != null || p.getPoint_y() != null) {
            check(p.getPoint_x(), p.getPoint_y());
            result.setPoint_tem(round(at(p.getPoint_x(), p.getPoint_y())));
            result.setPoint_position(position(p.getPoint_x(), p.getPoint_y()));
        } else {
            check(p.getLeft_top_x(), p.getLeft_top_y()); check(p.getRight_bottom_x(), p.getRight_bottom_y());
            int left = p.getLeft_top_x(), top = p.getLeft_top_y(), right = p.getRight_bottom_x(), bottom = p.getRight_bottom_y();
            if (left > right || top > bottom) throw new IllegalArgumentException("区域测温参数设置错误");
            double sum = 0, minimum = Double.POSITIVE_INFINITY, maximum = Double.NEGATIVE_INFINITY;
            for (int y = top; y <= bottom; y++) for (int x = left; x <= right; x++) {
                double value = at(x, y); sum += value;
                minimum = Math.min(minimum, value); maximum = Math.max(maximum, value);
            }
            result.setLeft_top_position(position(left, top)); result.setRight_bottum_position(position(right, bottom));
            result.setMin_tem(round(minimum)); result.setMax_tem(round(maximum));
            result.setAverage_tem(round(sum / ((right - left + 1L) * (bottom - top + 1L))));
        }
        result.setRange("(" + min + "°C," + max + "°C)");
        return result;
    }

    private void check(Integer x, Integer y) {
        if (x == null || y == null || x < 0 || y < 0 || x >= width || y >= height)
            throw new IllegalArgumentException("测温坐标缺失或超出温度矩阵范围 " + width + "x" + height);
    }
    private double at(int x, int y) { return values[y * width + x]; }
    private static double round(double value) { return Math.round(value * 10) / 10.0; }
    private static String position(int x, int y) { return "(" + x + "," + y + ")"; }
}
