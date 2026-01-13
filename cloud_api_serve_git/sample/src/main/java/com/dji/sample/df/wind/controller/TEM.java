package com.dji.sample.df.wind.controller;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TEM {

    public static void main(String[] args) {


        String imagePath = "C:\\Users\\90828\\Desktop\\fsdownload\\巡视热成像.jpg";
        File imageFile = new File(imagePath);

        if (!imageFile.exists()) {
            System.out.println("错误: 文件不存在 - " + imagePath);
            return;
        }

        try {
            ThermalAnalysisResult result = analyzeThermalImage(imageFile);
            printAnalysisResult(result);
        } catch (Exception e) {
            System.err.println("分析过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 分析热成像图片
     */
    public static ThermalAnalysisResult analyzeThermalImage(File imageFile) throws IOException {
        ThermalAnalysisResult result = new ThermalAnalysisResult();
        result.setFileName(imageFile.getName());
        result.setFilePath(imageFile.getAbsolutePath());
        result.setFileSize(imageFile.length());

        // 检测是否为图片文件
        if (!isImageFile(imageFile)) {
            result.setError("文件不是有效的图片格式");
            return result;
        }

        try {
            // 1. 检查文件扩展名和MIME类型
            String fileName = imageFile.getName().toLowerCase();
            result.setFileExtension(getFileExtension(fileName));

            // 2. 读取文件头部信息
            byte[] fileBytes = Files.readAllBytes(imageFile.toPath());
            result.setFileSignature(analyzeFileSignature(fileBytes));

            // 3. 检查常见的红外相机标记
            checkForIRMarkers(fileBytes, result);

            // 4. 检查EXIF元数据
            checkExifMetadata(imageFile, result);

            // 5. 检查图片属性和模式
            checkImageProperties(imageFile, result);

            // 6. 检查文件内容中的温度相关关键词
            checkForTemperatureKeywords(fileBytes, result);

            // 7. 计算综合评分
            calculateThermalProbability(result);

        } catch (Exception e) {
            result.setError("分析失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 检查是否为图片文件
     */
    private static boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".png") || name.endsWith(".bmp") ||
                name.endsWith(".tif") || name.endsWith(".tiff") ||
                name.endsWith(".dng") || name.endsWith(".raw");
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex + 1) : "未知";
    }

    /**
     * 分析文件签名/魔数
     */
    private static String analyzeFileSignature(byte[] fileBytes) {
        if (fileBytes.length < 4) return "文件太小";

        String hex = bytesToHex(Arrays.copyOfRange(fileBytes, 0, 4));

        // 常见图片格式的魔数
        switch (hex) {
            case "FFD8FFE0":
            case "FFD8FFE1":
            case "FFD8FFE2":
            case "FFD8FFE8": return "JPEG (标准)";
            case "FFD8FFDB": return "JPEG (部分红外相机使用)";
            case "89504E47": return "PNG";
            case "49492A00": return "TIFF (Intel字节序)";
            case "4D4D002A": return "TIFF (Motorola字节序)";
            case "424D": return "BMP";
            case "52494646": return "RIFF (可能包含热数据)";
            default: return "未知格式 (" + hex + ")";
        }
    }

    /**
     * 检查红外相机标记
     */
    private static void checkForIRMarkers(byte[] fileBytes, ThermalAnalysisResult result) {
        String fileContent = new String(fileBytes);
        byte[] searchBytes = fileBytes;

        // 常见红外相机标记
        String[] irMarkers = {
                "FLIR", "Thermal", "Thermographic", "Infrared", "IR",
                "Seek", "Testo", "Fluke", "FLIROne", "CAT",
                "FLIR Systems", "temperature", "Temp", "TEMP",
                "ThermalData", "IRData", "Heat", "Emissivity"
        };

        // 转换为字节数组进行搜索
        for (String marker : irMarkers) {
            byte[] markerBytes = marker.getBytes();
            if (containsBytes(searchBytes, markerBytes)) {
                result.addIrMarker(marker);
                result.incrementScore(10);
            }
        }

        // 检查特定二进制模式（一些红外相机的标识）
        byte[][] binaryPatterns = {
                {0x46, 0x4C, 0x49, 0x52}, // FLIR
                {0x54, 0x48, 0x45, 0x52}, // THER
                {0x49, 0x52, 0x49, 0x53}  // IRIS (某些相机)
        };

        for (byte[] pattern : binaryPatterns) {
            if (containsBytes(searchBytes, pattern)) {
                result.incrementScore(20);
            }
        }
    }

    /**
     * 检查EXIF元数据
     */
    private static void checkExifMetadata(File imageFile, ThermalAnalysisResult result) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(imageFile)) {
            ImageReader reader = ImageIO.getImageReaders(iis).next();
            reader.setInput(iis);

            IIOMetadata metadata = reader.getImageMetadata(0);
            if (metadata != null) {
                String[] metadataFormatNames = metadata.getMetadataFormatNames();

                for (String formatName : metadataFormatNames) {
                    Node root = metadata.getAsTree(formatName);
                    findThermalNodes(root, result);
                }

                // 特殊处理：尝试读取APP1段（FLIR等相机使用）
                if (metadataFormatNames.length > 0) {
                    result.setHasMetadata(true);
                    result.incrementScore(15);
                }
            }
        } catch (Exception e) {
            // EXIF读取失败是正常的，不是所有图片都有EXIF
            result.addWarning("无法读取EXIF数据: " + e.getMessage());
        }
    }

    /**
     * 递归查找热相关节点
     */
    private static void findThermalNodes(Node node, ThermalAnalysisResult result) {
        // 检查当前节点
        String nodeName = node.getNodeName().toLowerCase();
        String nodeValue = node.getNodeValue();

        if (nodeValue != null) {
            String lowerValue = nodeValue.toLowerCase();
            if (nodeName.contains("temp") || nodeName.contains("thermal") ||
                    nodeName.contains("ir") || nodeName.contains("heat")) {
                result.addMetadataEntry(nodeName + " = " + nodeValue);
                result.incrementScore(5);
            } else if (lowerValue.contains("temp") || lowerValue.contains("thermal") ||
                    lowerValue.contains("flir") || lowerValue.contains("infrared")) {
                result.addMetadataEntry(nodeName + " = " + nodeValue);
                result.incrementScore(5);
            }
        }

        // 检查属性
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                String attrName = attr.getNodeName().toLowerCase();
                String attrValue = attr.getNodeValue();

                if (attrName.contains("temp") || attrName.contains("thermal") ||
                        (attrValue != null && (attrValue.toLowerCase().contains("temp") ||
                                attrValue.toLowerCase().contains("thermal")))) {
                    result.addMetadataEntry("属性: " + attrName + " = " + attrValue);
                    result.incrementScore(3);
                }
            }
        }

        // 递归检查子节点
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            findThermalNodes(children.item(i), result);
        }
    }

    /**
     * 检查图片属性
     */
    private static void checkImageProperties(File imageFile, ThermalAnalysisResult result) {
        try {
            // 尝试读取图片
            var bufferedImage = ImageIO.read(imageFile);
            if (bufferedImage != null) {
                int width = bufferedImage.getWidth();
                int height = bufferedImage.getHeight();
                result.setImageDimensions(width + "x" + height);

                // 检查是否为灰度图（红外热成像通常是灰度图）
                int imageType = bufferedImage.getType();
                boolean isGrayscale = (imageType == 10) || // TYPE_BYTE_GRAY
                        (imageType == 11) || // TYPE_USHORT_GRAY
                        (imageType == 12);   // TYPE_BYTE_BINARY

                if (isGrayscale) {
                    result.setGrayscale(true);
                    result.incrementScore(10);
                    result.addFinding("图像是灰度图（红外热成像的典型特征）");
                }

                // 检查图像模式
                String colorModel = bufferedImage.getColorModel().toString();
                if (colorModel.toLowerCase().contains("gray") ||
                        colorModel.toLowerCase().contains("grey")) {
                    result.incrementScore(5);
                }
            }
        } catch (Exception e) {
            result.addWarning("无法读取图像属性: " + e.getMessage());
        }
    }

    /**
     * 检查温度相关关键词
     */
    private static void checkForTemperatureKeywords(byte[] fileBytes, ThermalAnalysisResult result) {
        // 转换为字符串进行搜索（可能只适用于文本元数据）
        String fileString = new String(fileBytes);
        String lowerFileString = fileString.toLowerCase();

        String[] tempKeywords = {
                "temperature", "temp", "thermal", "infrared", "ir",
                "celsius", "centigrade", "fahrenheit", "kelvin",
                "emissivity", "reflection", "atmospheric",
                "object parameters", "measurement parameters",
                "calibration", "calibrated", "range", "scale"
        };

        for (String keyword : tempKeywords) {
            if (lowerFileString.contains(keyword.toLowerCase())) {
                result.addTemperatureKeyword(keyword);
                result.incrementScore(3);
            }
        }
    }

    /**
     * 计算热成像概率
     */
    private static void calculateThermalProbability(ThermalAnalysisResult result) {
        int score = result.getScore();

        if (score >= 50) {
            result.setThermalProbability("高 (>90%)");
            result.setContainsTemperatureData(true);
        } else if (score >= 30) {
            result.setThermalProbability("中 (50-90%)");
            result.setContainsTemperatureData(true);
        } else if (score >= 15) {
            result.setThermalProbability("低 (20-50%)");
            result.setContainsTemperatureData("可能");
        } else {
            result.setThermalProbability("很低 (<20%)");
            result.setContainsTemperatureData(false);
        }
    }

    /**
     * 打印分析结果
     */
    private static void printAnalysisResult(ThermalAnalysisResult result) {
        System.out.println("=".repeat(60));
        System.out.println("红外热成像图片分析报告");
        System.out.println("=".repeat(60));

        System.out.printf("文件: %s\n", result.getFileName());
        System.out.printf("路径: %s\n", result.getFilePath());
        System.out.printf("大小: %d 字节\n", result.getFileSize());
        System.out.printf("扩展名: %s\n", result.getFileExtension());
        System.out.printf("文件签名: %s\n", result.getFileSignature());

        if (result.getImageDimensions() != null) {
            System.out.printf("图像尺寸: %s\n", result.getImageDimensions());
        }

        System.out.println("\n" + "-".repeat(60));
        System.out.println("热成像特征检测:");
        System.out.println("-".repeat(60));

        System.out.printf("包含温度数据: %s\n", result.getContainsTemperatureData());
        System.out.printf("热成像概率: %s\n", result.getThermalProbability());
        System.out.printf("检测分数: %d/100\n", result.getScore());
        System.out.printf("是灰度图: %s\n", result.isGrayscale());
        System.out.printf("有元数据: %s\n", result.hasMetadata());

        if (!result.getIrMarkers().isEmpty()) {
            System.out.println("\n检测到的红外相机标记:");
            for (String marker : result.getIrMarkers()) {
                System.out.println("  • " + marker);
            }
        }

        if (!result.getTemperatureKeywords().isEmpty()) {
            System.out.println("\n检测到的温度相关关键词:");
            for (String keyword : result.getTemperatureKeywords()) {
                System.out.println("  • " + keyword);
            }
        }

        if (!result.getMetadataEntries().isEmpty()) {
            System.out.println("\n检测到的相关元数据:");
            for (String entry : result.getMetadataEntries()) {
                System.out.println("  • " + entry);
            }
        }

        if (!result.getFindings().isEmpty()) {
            System.out.println("\n发现:");
            for (String finding : result.getFindings()) {
                System.out.println("  • " + finding);
            }
        }

        if (!result.getWarnings().isEmpty()) {
            System.out.println("\n警告:");
            for (String warning : result.getWarnings()) {
                System.out.println("  ⚠ " + warning);
            }
        }

        if (result.getError() != null) {
            System.out.println("\n错误: " + result.getError());
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("总结:");
        System.out.println("=".repeat(60));

        if (result.getContainsTemperatureData() instanceof Boolean) {
            boolean hasTempData = (Boolean) result.getContainsTemperatureData();
            if (hasTempData) {
                System.out.println("✅ 该图片很可能包含温度数据（红外热成像图片）");
                System.out.println("   可以尝试使用专业红外分析软件提取温度信息");
            } else {
                System.out.println("❌ 该图片可能不包含温度数据");
                System.out.println("   可能是普通可见光图片，或温度数据已被移除");
            }
        } else {
            System.out.println("❓ 无法确定是否包含温度数据");
            System.out.println("   建议使用原厂软件或专业热分析工具进一步检查");
        }

        System.out.println("\n提示: 真正的红外热成像图片通常:");
        System.out.println("  1. 包含EXIF元数据中的温度参数");
        System.out.println("  2. 来自FLIR、Seek Thermal、Testo等品牌设备");
        System.out.println("  3. 可能是灰度或伪彩色图像");
        System.out.println("  4. 文件较大（包含额外的热数据层）");
    }

    // ==================== 辅助方法 ====================

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * 检查字节数组是否包含子数组
     */
    private static boolean containsBytes(byte[] source, byte[] target) {
        if (target.length == 0) return true;
        if (source.length < target.length) return false;

        for (int i = 0; i <= source.length - target.length; i++) {
            boolean found = true;
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return true;
        }
        return false;
    }

    // ==================== 结果类 ====================

    static class ThermalAnalysisResult {
        private String fileName;
        private String filePath;
        private long fileSize;
        private String fileExtension;
        private String fileSignature;
        private String imageDimensions;
        private boolean isGrayscale = false;
        private boolean hasMetadata = false;
        private Object containsTemperatureData = "未知";
        private String thermalProbability = "未知";
        private int score = 0;
        private String error = null;

        private List<String> irMarkers = new ArrayList<>();
        private List<String> temperatureKeywords = new ArrayList<>();
        private List<String> metadataEntries = new ArrayList<>();
        private List<String> findings = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        public String getFileExtension() { return fileExtension; }
        public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }

        public String getFileSignature() { return fileSignature; }
        public void setFileSignature(String fileSignature) { this.fileSignature = fileSignature; }

        public String getImageDimensions() { return imageDimensions; }
        public void setImageDimensions(String imageDimensions) { this.imageDimensions = imageDimensions; }

        public boolean isGrayscale() { return isGrayscale; }
        public void setGrayscale(boolean grayscale) { isGrayscale = grayscale; }

        public boolean hasMetadata() { return hasMetadata; }
        public void setHasMetadata(boolean hasMetadata) { this.hasMetadata = hasMetadata; }

        public Object getContainsTemperatureData() { return containsTemperatureData; }
        public void setContainsTemperatureData(Object containsTemperatureData) {
            this.containsTemperatureData = containsTemperatureData;
        }

        public String getThermalProbability() { return thermalProbability; }
        public void setThermalProbability(String thermalProbability) { this.thermalProbability = thermalProbability; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public void incrementScore(int points) { this.score += points; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public List<String> getIrMarkers() { return irMarkers; }
        public void addIrMarker(String marker) { this.irMarkers.add(marker); }

        public List<String> getTemperatureKeywords() { return temperatureKeywords; }
        public void addTemperatureKeyword(String keyword) { this.temperatureKeywords.add(keyword); }

        public List<String> getMetadataEntries() { return metadataEntries; }
        public void addMetadataEntry(String entry) { this.metadataEntries.add(entry); }

        public List<String> getFindings() { return findings; }
        public void addFinding(String finding) { this.findings.add(finding); }

        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }
    }
}
