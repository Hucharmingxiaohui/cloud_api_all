package com.dji.sample.df.wind.controller;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

/**
 * 红外热成像图片温度提取器
 * 支持多种红外相机格式的温度数据提取
 */
public class AA {

    // FLIR相机常量
    private static final byte[] FLIR_MARKER = "FLIR".getBytes();
    private static final byte[] FLIR_SYSTEMS_MARKER = "FLIR Systems".getBytes();
    private static final int FLIR_RAW_DATA_OFFSET = 0x400; // 常见偏移量

    // Seek Thermal相机常量
    private static final byte[] SEEK_MARKER = "seek".getBytes();

    // 测试点位置（中心点和四个角）
    private static final int[][] TEST_POINTS = {
            {0, 0},                   // 左上角
            {0, -1},                  // 右上角
            {-1, 0},                  // 左下角
            {-1, -1},                 // 右下角
            {0, 0}                    // 中心（会在运行时计算）
    };

    public static void main(String[] args) {


        String imagePath = "C:\\Users\\90828\\Desktop\\fsdownload\\无人机热成像.jpg";
        String outputPath = args.length > 1 ? args[1] : null;

        try {
            System.out.println("正在分析: " + imagePath);
            TemperatureData result = extractTemperatureData(imagePath, outputPath);

            if (result != null && result.isSuccess()) {
                printTemperatureReport(result);

                if (outputPath != null) {
                    saveTemperatureData(result, outputPath);
                }
            } else {
                System.err.println("无法从图片中提取温度数据");
                System.err.println("可能原因:");
                System.err.println("  1. 不是红外热成像图片");
                System.err.println("  2. 温度数据已被移除");
                System.err.println("  3. 不支持的相机格式");
            }

        } catch (Exception e) {
            System.err.println("处理过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 提取温度数据
     */
    public static TemperatureData extractTemperatureData(String imagePath, String outputPath) throws IOException {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new FileNotFoundException("文件不存在: " + imagePath);
        }

        byte[] fileData = Files.readAllBytes(imageFile.toPath());
        TemperatureData tempData = new TemperatureData(imageFile.getName());

        // 1. 尝试识别相机类型
        CameraType cameraType = identifyCameraType(fileData);
        tempData.setCameraType(cameraType);
        System.out.println("检测到相机类型: " + cameraType);

        // 2. 尝试不同方法提取温度数据
        boolean success = false;

        switch (cameraType) {
            case FLIR:
                success = extractFlirTemperatureData(fileData, tempData);
                break;
            case SEEK_THERMAL:
                success = extractSeekTemperatureData(fileData, tempData);
                break;
            case STANDARD_JPEG_WITH_THERMAL:
                success = extractStandardThermalData(fileData, tempData);
                break;
            case UNKNOWN:
                // 尝试所有方法
                success = tryAllExtractionMethods(fileData, tempData);
                break;
            default:
                success = tryAllExtractionMethods(fileData, tempData);
                break;
        }

        tempData.setSuccess(success);
        return tempData;
    }

    /**
     * 识别相机类型
     */
    private static CameraType identifyCameraType(byte[] fileData) {
        String hexString = bytesToHex(fileData, 0, Math.min(1000, fileData.length));

        // 检查FLIR标记
        if (containsBytes(fileData, FLIR_MARKER) || containsBytes(fileData, FLIR_SYSTEMS_MARKER)) {
            return CameraType.FLIR;
        }

        // 检查Seek Thermal标记
        if (containsBytes(fileData, SEEK_MARKER)) {
            return CameraType.SEEK_THERMAL;
        }

        // 检查Testo标记
        if (hexString.contains("544553544f") || hexString.contains("544553544f")) { // TESTO的hex
            return CameraType.TESTO;
        }

        // 检查标准JPEG但可能包含热数据
        if (isJpegFile(fileData)) {
            // 检查是否有APP1段（可能包含热数据）
            if (findJpegSegment(fileData, 0xFFE1) != -1) {
                return CameraType.STANDARD_JPEG_WITH_THERMAL;
            }
        }

        return CameraType.UNKNOWN;
    }

    /**
     * 提取FLIR相机温度数据
     */
    private static boolean extractFlirTemperatureData(byte[] fileData, TemperatureData tempData) {
        try {
            // FLIR JPEG通常包含热数据在APP1段
            int app1Offset = findJpegSegment(fileData, 0xFFE1);
            if (app1Offset != -1) {
                System.out.println("找到APP1段，偏移: " + app1Offset);

                // 尝试解析FLIR APP1数据
                return parseFlirApp1Data(fileData, app1Offset, tempData);
            }

            // 如果找不到APP1，尝试其他方法
            return extractFlirRawData(fileData, tempData);

        } catch (Exception e) {
            System.err.println("FLIR数据提取失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 解析FLIR APP1数据
     */
    private static boolean parseFlirApp1Data(byte[] fileData, int app1Offset, TemperatureData tempData) {
        try {
            // 跳过标记(2字节)和长度(2字节)
            int dataStart = app1Offset + 4;

            // 读取APP1段长度
            int segmentLength = ((fileData[app1Offset + 2] & 0xFF) << 8) | (fileData[app1Offset + 3] & 0xFF);
            int dataEnd = app1Offset + segmentLength;

            System.out.println("APP1段长度: " + segmentLength + " 字节");

            // 在APP1段中搜索热数据
            byte[] app1Data = Arrays.copyOfRange(fileData, dataStart, dataEnd);

            // 搜索热数据标记（常见模式）
            int thermalDataOffset = -1;
            int thermalDataLength = 0;

            // 常见的热数据标记
            byte[][] thermalMarkers = {
                    "raw thermal image".getBytes(),
                    "thermal data".getBytes(),
                    "rawdata".getBytes(),
                    {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00} // 常见填充
            };

            for (byte[] marker : thermalMarkers) {
                int offset = indexOf(app1Data, marker);
                if (offset != -1) {
                    thermalDataOffset = offset + marker.length;
                    System.out.println("找到热数据标记，偏移: " + offset);
                    break;
                }
            }

            if (thermalDataOffset == -1) {
                // 如果没有找到标记，假设热数据在固定位置
                thermalDataOffset = FLIR_RAW_DATA_OFFSET;
            }

            // 尝试提取热数据（假设是16位灰度）
            if (thermalDataOffset + 2 < app1Data.length) {
                // 假设图像尺寸（需要从元数据获取）
                int width = 640;  // 常见FLIR分辨率
                int height = 480;

                // 计算热数据大小
                thermalDataLength = width * height * 2; // 16位=2字节每像素

                if (thermalDataOffset + thermalDataLength <= app1Data.length) {
                    byte[] thermalBytes = Arrays.copyOfRange(app1Data, thermalDataOffset,
                            thermalDataOffset + thermalDataLength);

                    // 解析温度值
                    float[][] temperatures = parse16BitThermalData(thermalBytes, width, height);

                    if (temperatures != null) {
                        tempData.setTemperatures(temperatures);
                        tempData.setWidth(width);
                        tempData.setHeight(height);

                        // 提取校准参数（如果存在）
                        extractCalibrationParameters(app1Data, tempData);

                        return true;
                    }
                }
            }

            return false;

        } catch (Exception e) {
            System.err.println("解析FLIR APP1数据失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 提取FLIR原始数据（备用方法）
     */
    private static boolean extractFlirRawData(byte[] fileData, TemperatureData tempData) {
        try {
            // 尝试在文件中搜索原始热数据
            // 常见模式：连续的0x00或特定值

            // 假设图像尺寸
            int width = 640;
            int height = 480;
            int pixelCount = width * height;
            int expectedSize = pixelCount * 2; // 16位

            // 搜索可能的热数据区域
            for (int i = 0; i < fileData.length - expectedSize; i += 1024) {
                // 检查区域是否看起来像热数据
                if (isLikelyThermalData(fileData, i, expectedSize)) {
                    System.out.println("发现可能的热数据，偏移: " + i);

                    byte[] thermalBytes = Arrays.copyOfRange(fileData, i, i + expectedSize);
                    float[][] temperatures = parse16BitThermalData(thermalBytes, width, height);

                    if (temperatures != null) {
                        tempData.setTemperatures(temperatures);
                        tempData.setWidth(width);
                        tempData.setHeight(height);
                        return true;
                    }
                }
            }

            return false;

        } catch (Exception e) {
            System.err.println("提取FLIR原始数据失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 提取Seek Thermal温度数据
     */
    private static boolean extractSeekTemperatureData(byte[] fileData, TemperatureData tempData) {
        try {
            // Seek Thermal通常使用RAW格式或特殊JPEG

            // 检查文件扩展名
            // .seek或.raw扩展名可能是原始热数据

            // 尝试解析为16位原始数据
            int width = 208;  // Seek Thermal常见分辨率
            int height = 156;
            int expectedSize = width * height * 2;

            if (fileData.length >= expectedSize) {
                // 可能是原始文件
                byte[] thermalBytes = Arrays.copyOfRange(fileData, 0, expectedSize);
                float[][] temperatures = parse16BitThermalData(thermalBytes, width, height);

                if (temperatures != null) {
                    tempData.setTemperatures(temperatures);
                    tempData.setWidth(width);
                    tempData.setHeight(height);

                    // Seek Thermal需要特殊的校准
                    applySeekCalibration(tempData);

                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            System.err.println("Seek Thermal数据提取失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 提取标准JPEG中的热数据
     */
    private static boolean extractStandardThermalData(byte[] fileData, TemperatureData tempData) {
        try {
            // 尝试使用ImageIO读取
            ByteArrayInputStream bais = new ByteArrayInputStream(fileData);
            BufferedImage img = ImageIO.read(bais);

            if (img == null) {
                return false;
            }

            int width = img.getWidth();
            int height = img.getHeight();

            // 检查图像类型
            int imageType = img.getType();

            // 如果是16位灰度，可能是热数据
            if (imageType == BufferedImage.TYPE_USHORT_GRAY) {
                System.out.println("检测到16位灰度图像（可能是热数据）");

                float[][] temperatures = new float[height][width];

                // 从图像中提取像素值
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int rgb = img.getRGB(x, y);
                        // 16位灰度值在0-65535之间
                        int grayValue = rgb & 0xFFFF;

                        // 转换为温度（需要校准参数）
                        temperatures[y][x] = grayValueToTemperature(grayValue);
                    }
                }

                tempData.setTemperatures(temperatures);
                tempData.setWidth(width);
                tempData.setHeight(height);

                return true;
            }

            // 如果是8位图像，可能是伪彩色热图
            // 需要查找分离的温度数据

            return false;

        } catch (Exception e) {
            System.err.println("标准热数据提取失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 尝试所有提取方法
     */
    private static boolean tryAllExtractionMethods(byte[] fileData, TemperatureData tempData) {
        // 按顺序尝试不同方法

        System.out.println("尝试方法1: 标准JPEG热数据提取");
        if (extractStandardThermalData(fileData, tempData)) {
            return true;
        }

        System.out.println("尝试方法2: FLIR数据提取");
        if (extractFlirTemperatureData(fileData, tempData)) {
            return true;
        }

        System.out.println("尝试方法3: Seek Thermal数据提取");
        if (extractSeekTemperatureData(fileData, tempData)) {
            return true;
        }

        System.out.println("尝试方法4: 通用热数据搜索");
        return searchForThermalData(fileData, tempData);
    }

    /**
     * 通用热数据搜索
     */
    private static boolean searchForThermalData(byte[] fileData, TemperatureData tempData) {
        try {
            // 尝试不同尺寸
            int[] possibleWidths = {160, 320, 640, 800};
            int[] possibleHeights = {120, 240, 480, 600};

            for (int width : possibleWidths) {
                for (int height : possibleHeights) {
                    int pixelCount = width * height;
                    int expectedSize16bit = pixelCount * 2;
                    int expectedSize8bit = pixelCount;

                    // 搜索16位数据
                    for (int offset = 0; offset < fileData.length - expectedSize16bit; offset += 1024) {
                        if (isLikelyThermalData(fileData, offset, expectedSize16bit)) {
                            System.out.println("发现可能的热数据 (16位), 尺寸: " + width + "x" + height);

                            byte[] thermalBytes = Arrays.copyOfRange(fileData, offset, offset + expectedSize16bit);
                            float[][] temperatures = parse16BitThermalData(thermalBytes, width, height);

                            if (temperatures != null) {
                                tempData.setTemperatures(temperatures);
                                tempData.setWidth(width);
                                tempData.setHeight(height);
                                return true;
                            }
                        }
                    }

                    // 搜索8位数据
                    for (int offset = 0; offset < fileData.length - expectedSize8bit; offset += 1024) {
                        if (isLikelyThermalData8bit(fileData, offset, expectedSize8bit)) {
                            System.out.println("发现可能的热数据 (8位), 尺寸: " + width + "x" + height);

                            byte[] thermalBytes = Arrays.copyOfRange(fileData, offset, offset + expectedSize8bit);
                            float[][] temperatures = parse8BitThermalData(thermalBytes, width, height);

                            if (temperatures != null) {
                                tempData.setTemperatures(temperatures);
                                tempData.setWidth(width);
                                tempData.setHeight(height);
                                return true;
                            }
                        }
                    }
                }
            }

            return false;

        } catch (Exception e) {
            System.err.println("通用热数据搜索失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 解析16位热数据
     */
    private static float[][] parse16BitThermalData(byte[] data, int width, int height) {
        try {
            if (data.length != width * height * 2) {
                System.err.println("数据长度不匹配: 期望 " + (width * height * 2) + " 字节, 实际 " + data.length + " 字节");
                return null;
            }

            float[][] temperatures = new float[height][width];

            // 两种可能的字节序: 大端序和小端序
            boolean bigEndian = true;

            // 尝试检测字节序
            if (data.length >= 4) {
                int val1 = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
                int val2 = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);

                // 热数据通常在某个范围内
                if (val1 > 10000 && val2 > 10000) {
                    // 可能字节序错误
                    bigEndian = false;
                }
            }

            int pixelIndex = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int byteIndex = pixelIndex * 2;

                    int rawValue;
                    if (bigEndian) {
                        rawValue = ((data[byteIndex] & 0xFF) << 8) | (data[byteIndex + 1] & 0xFF);
                    } else {
                        rawValue = ((data[byteIndex + 1] & 0xFF) << 8) | (data[byteIndex] & 0xFF);
                    }

                    // 转换为温度（需要校准）
                    temperatures[y][x] = rawValueToTemperature(rawValue);

                    pixelIndex++;
                }
            }

            return temperatures;

        } catch (Exception e) {
            System.err.println("解析16位热数据失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析8位热数据
     */
    private static float[][] parse8BitThermalData(byte[] data, int width, int height) {
        try {
            if (data.length != width * height) {
                return null;
            }

            float[][] temperatures = new float[height][width];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    int rawValue = data[index] & 0xFF;

                    // 8位数据通常需要不同的转换
                    temperatures[y][x] = rawValueToTemperature8bit(rawValue);
                }
            }

            return temperatures;

        } catch (Exception e) {
            System.err.println("解析8位热数据失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 提取校准参数
     */
    private static void extractCalibrationParameters(byte[] app1Data, TemperatureData tempData) {
        try {
            // 在APP1数据中搜索校准参数
            // 常见参数: 发射率、环境温度、反射温度、大气温度等

            // 搜索温度相关关键词
            String app1String = new String(app1Data);
            String[] tempKeywords = {
                    "Emissivity", "ReflectedTemp", "AtmosphericTemp",
                    "IRWindowTemp", "IRWindowTransmission",
                    "PlanckR1", "PlanckB", "PlanckF", "PlanckO", "PlanckR2"
            };

            for (String keyword : tempKeywords) {
                int index = app1String.indexOf(keyword);
                if (index != -1) {
                    // 提取值
                    String substring = app1String.substring(index, Math.min(index + 100, app1String.length()));
                    System.out.println("发现校准参数: " + substring.substring(0, Math.min(50, substring.length())));

                    tempData.addCalibrationParameter(keyword, substring);
                }
            }

        } catch (Exception e) {
            System.err.println("提取校准参数失败: " + e.getMessage());
        }
    }

    /**
     * 应用Seek Thermal校准
     */
    private static void applySeekCalibration(TemperatureData tempData) {
        // Seek Thermal需要特殊校准
        // 这里使用简化校准
        float[][] temps = tempData.getTemperatures();

        for (int y = 0; y < tempData.getHeight(); y++) {
            for (int x = 0; x < tempData.getWidth(); x++) {
                // Seek Thermal校准公式（简化版）
                float raw = temps[y][x];
                float calibrated = (raw - 1000) * 0.1f; // 简化校准
                temps[y][x] = calibrated;
            }
        }

        tempData.addCalibrationParameter("SeekCalibration", "简化校准: (raw-1000)*0.1");
    }

    /**
     * 原始值转换为温度（16位）
     */
    private static float rawValueToTemperature(int rawValue) {
        // 通用转换公式（需要根据具体相机调整）
        // 真实转换需要校准参数

        // 简化转换：假设线性关系
        // 典型范围：raw 10000-30000 对应 -20°C 到 120°C

        float minRaw = 10000.0f;
        float maxRaw = 30000.0f;
        float minTemp = -20.0f;
        float maxTemp = 120.0f;

        float temp = minTemp + (rawValue - minRaw) * (maxTemp - minTemp) / (maxRaw - minRaw);

        // 限制范围
        temp = Math.max(-50, Math.min(500, temp));

        return temp;
    }

    /**
     * 原始值转换为温度（8位）
     */
    private static float rawValueToTemperature8bit(int rawValue) {
        // 8位数据通常范围0-255
        // 转换为温度范围

        float temp = (rawValue / 255.0f) * 100.0f - 20.0f; // -20°C 到 80°C
        return temp;
    }

    /**
     * 灰度值转换为温度
     */
    private static float grayValueToTemperature(int grayValue) {
        // 16位灰度值转换为温度
        return rawValueToTemperature(grayValue);
    }

    /**
     * 检查数据是否可能是热数据
     */
    private static boolean isLikelyThermalData(byte[] data, int offset, int length) {
        if (offset + length > data.length) {
            return false;
        }

        // 检查数据变化是否合理
        // 热数据通常有平滑的变化

        int sampleCount = Math.min(100, length / 2);
        int lastValue = -1;
        int similarCount = 0;

        for (int i = 0; i < sampleCount; i++) {
            int byteIndex = offset + i * 2;
            if (byteIndex + 1 >= data.length) break;

            int value = ((data[byteIndex] & 0xFF) << 8) | (data[byteIndex + 1] & 0xFF);

            if (lastValue != -1) {
                int diff = Math.abs(value - lastValue);
                if (diff < 100) { // 相邻像素通常变化不大
                    similarCount++;
                }
            }

            lastValue = value;
        }

        // 如果大部分相邻像素值相似，可能是热数据
        float similarityRatio = (float)similarCount / (sampleCount - 1);
        return similarityRatio > 0.7;
    }

    /**
     * 检查8位数据是否可能是热数据
     */
    private static boolean isLikelyThermalData8bit(byte[] data, int offset, int length) {
        if (offset + length > data.length) {
            return false;
        }

        // 简单检查：数据是否在合理范围内
        int validCount = 0;

        for (int i = 0; i < Math.min(100, length); i++) {
            int value = data[offset + i] & 0xFF;
            if (value > 0 && value < 250) { // 排除极端值
                validCount++;
            }
        }

        float validRatio = (float)validCount / Math.min(100, length);
        return validRatio > 0.8;
    }

    /**
     * 在JPEG文件中查找段
     */
    private static int findJpegSegment(byte[] data, int marker) {
        for (int i = 0; i < data.length - 1; i++) {
            if (data[i] == (byte)0xFF && (data[i+1] & 0xFF) == marker) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 检查是否为JPEG文件
     */
    private static boolean isJpegFile(byte[] data) {
        return data.length > 2 && data[0] == (byte)0xFF && data[1] == (byte)0xD8;
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes, int start, int length) {
        StringBuilder sb = new StringBuilder();
        int end = Math.min(start + length, bytes.length);
        for (int i = start; i < end; i++) {
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }

    /**
     * 检查字节数组是否包含子数组
     */
    private static boolean containsBytes(byte[] source, byte[] target) {
        return indexOf(source, target) != -1;
    }

    /**
     * 在字节数组中查找子数组
     */
    private static int indexOf(byte[] source, byte[] target) {
        for (int i = 0; i <= source.length - target.length; i++) {
            boolean found = true;
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
    }

    /**
     * 打印温度报告
     */
    private static void printTemperatureReport(TemperatureData tempData) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("温度数据提取成功!");
        System.out.println("=".repeat(60));

        System.out.printf("文件: %s\n", tempData.getFileName());
        System.out.printf("相机类型: %s\n", tempData.getCameraType());
        System.out.printf("图像尺寸: %d x %d\n", tempData.getWidth(), tempData.getHeight());
        System.out.printf("温度点数: %,d\n", tempData.getWidth() * tempData.getHeight());

        float[][] temps = tempData.getTemperatures();

        // 计算统计信息
        float minTemp = Float.MAX_VALUE;
        float maxTemp = Float.MIN_VALUE;
        float sumTemp = 0;
        int count = 0;

        for (int y = 0; y < tempData.getHeight(); y++) {
            for (int x = 0; x < tempData.getWidth(); x++) {
                float temp = temps[y][x];
                if (temp < minTemp) minTemp = temp;
                if (temp > maxTemp) maxTemp = temp;
                sumTemp += temp;
                count++;
            }
        }

        float avgTemp = sumTemp / count;

        System.out.println("\n温度统计:");
        System.out.printf("  最低温度: %.1f°C\n", minTemp);
        System.out.printf("  最高温度: %.1f°C\n", maxTemp);
        System.out.printf("  平均温度: %.1f°C\n", avgTemp);
        System.out.printf("  温度范围: %.1f°C\n", maxTemp - minTemp);

        // 显示测试点温度
        System.out.println("\n测试点温度:");

        // 计算中心点
        int centerX = tempData.getWidth() / 2;
        int centerY = tempData.getHeight() / 2;

        int[][] points = {
                {0, 0}, // 左上
                {tempData.getWidth() - 1, 0}, // 右上
                {0, tempData.getHeight() - 1}, // 左下
                {tempData.getWidth() - 1, tempData.getHeight() - 1}, // 右下
                {centerX, centerY} // 中心
        };

        String[] pointNames = {"左上角", "右上角", "左下角", "右下角", "中心点"};

        for (int i = 0; i < points.length; i++) {
            int x = points[i][0];
            int y = points[i][1];
            if (x >= 0 && x < tempData.getWidth() && y >= 0 && y < tempData.getHeight()) {
                float temp = temps[y][x];
                System.out.printf("  %s (%d, %d): %.1f°C\n", pointNames[i], x, y, temp);
            }
        }

        // 显示校准参数
        if (!tempData.getCalibrationParameters().isEmpty()) {
            System.out.println("\n校准参数:");
            for (Map.Entry<String, String> entry : tempData.getCalibrationParameters().entrySet()) {
                System.out.printf("  %s: %s\n", entry.getKey(),
                        entry.getValue().substring(0, Math.min(50, entry.getValue().length())));
            }
        }

        System.out.println("\n提示:");
        System.out.println("  1. 温度值为估算值，实际值可能因校准不同而有差异");
        System.out.println("  2. 如需精确温度，请使用相机原厂软件");
        System.out.println("  3. 可以指定输出CSV文件保存所有温度数据");
    }

    /**
     * 保存温度数据到CSV
     */
    private static void saveTemperatureData(TemperatureData tempData, String outputPath) throws IOException {
        float[][] temps = tempData.getTemperatures();

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            // 写入头部信息
            writer.println("红外热成像温度数据");
            writer.printf("文件:,%s\n", tempData.getFileName());
            writer.printf("相机类型:,%s\n", tempData.getCameraType());
            writer.printf("尺寸:,%d x %d\n", tempData.getWidth(), tempData.getHeight());
            writer.println();

            // 写入温度数据
            writer.println("温度矩阵 (°C):");
            for (int y = 0; y < tempData.getHeight(); y++) {
                for (int x = 0; x < tempData.getWidth(); x++) {
                    writer.printf("%.1f", temps[y][x]);
                    if (x < tempData.getWidth() - 1) {
                        writer.print(",");
                    }
                }
                writer.println();
            }

            System.out.println("\n温度数据已保存到: " + outputPath);
        }
    }

    /**
     * 温度数据容器类
     */
    static class TemperatureData {
        private String fileName;
        private CameraType cameraType = CameraType.UNKNOWN;
        private float[][] temperatures;
        private int width;
        private int height;
        private boolean success = false;
        private Map<String, String> calibrationParameters = new HashMap<>();

        public TemperatureData(String fileName) {
            this.fileName = fileName;
        }

        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public CameraType getCameraType() { return cameraType; }
        public void setCameraType(CameraType cameraType) { this.cameraType = cameraType; }

        public float[][] getTemperatures() { return temperatures; }
        public void setTemperatures(float[][] temperatures) { this.temperatures = temperatures; }

        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }

        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public Map<String, String> getCalibrationParameters() { return calibrationParameters; }
        public void addCalibrationParameter(String key, String value) {
            calibrationParameters.put(key, value);
        }
    }

    /**
     * 相机类型枚举
     */
    enum CameraType {
        FLIR("FLIR"),
        SEEK_THERMAL("Seek Thermal"),
        TESTO("Testo"),
        STANDARD_JPEG_WITH_THERMAL("标准JPEG带热数据"),
        UNKNOWN("未知");

        private final String displayName;

        CameraType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
