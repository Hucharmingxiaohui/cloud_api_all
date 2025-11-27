package com.dji.sample.df.wind.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.df.server.dto.HisUniTask.HisUniTaskParamsDTO;
import com.df.server.dto.HisUniTask.TaskReportDTO;
import com.dji.sample.df.electricInspectionDf.service.ReportService;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.wind.config.WaylineUrlConfig;
import com.dji.sample.df.wind.dao.FanWaylinePointsMapper;
import com.dji.sample.df.wind.handler.PictureSaveHandler;
import com.dji.sample.df.wind.config.FjFileConfig;
import com.dji.sample.df.wind.dao.DefectEntityMapper;
import com.dji.sample.df.wind.dao.FjReportMapper;
import com.dji.sample.df.wind.model.entity.*;
import com.dji.sample.df.wind.service.FjReportService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Service
public class FjReportServiceImpl implements FjReportService {

    @Autowired
    private FjReportMapper fjReportMapper;

    @Autowired
    IWaylineJobMapper waylineJobMapper;

    @Autowired
    private FjFileConfig fileConfig;

    @Autowired
    DefectEntityMapper defectEntityMapper;

    @Resource
    FanWaylinePointsMapper fanWaylinePointsMapper;

    @Autowired
    private WaylineUrlConfig waylineUrlConfig;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String createNewReport(String jobId) {
//        fjReportMapper.delete(new LambdaQueryWrapper<FjReportEntity>().eq(FjReportEntity::getTaskPatrolledId,jobId));
        FjReportEntity reportEntity = new FjReportEntity();
        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>().
                eq(WaylineJobEntity::getJobId, jobId));
        reportEntity.setId(UUID.randomUUID().toString());
        reportEntity.setName(waylineJobEntity.getName());
        reportEntity.setTaskPatrolledId(jobId);
        reportEntity.setReportStartTime(new Date());
        fjReportMapper.insert(reportEntity);
        return reportEntity.getId();
    }

    /**
     * 查看任务报告
     *
     * @param params
     * @return
     */
    @Override
    public TaskReportDTO lookReport(HisUniTaskParamsDTO params) {
        return null;
    }

    /**
     * 生成巡视报告
     *
     * @param reportId
     */
    @Override
    public void genPatrolTaskWordNew(String reportId,  String jobId) {

        // 1. 从数据库获取巡检任务、风机、缺陷等信息
        FjReportEntity fjReportEntity = fjReportMapper.selectById(reportId);
        String taskName = fjReportEntity.getName();
        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(
                new LambdaQueryWrapper<WaylineJobEntity>()
                        .eq(WaylineJobEntity::getJobId, jobId)
        );
        Long beginTime = waylineJobEntity.getBeginTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String formattedTime = sdf.format(new Date(beginTime));

        // 假设缺陷列表（实际需从数据库获取）
        List<DefectEntity> defectList = defectEntityMapper.selectList(new LambdaQueryWrapper<DefectEntity>()
                .eq(DefectEntity::getJobId,jobId));
//      缺陷数
        long count = defectList.stream()
                .filter(defect -> !defect.getDefectType().contains("无缺陷"))
                .filter(defect -> !defect.getDefectType().contains("无结果"))
                .count();
        // 从Redis获取fanPoints数据并解析为JSONArray
//        String fanPointsStr = (String) redisUtils.get("fanPoints");
        FanWaylinePoints fanWaylinePoints = fanWaylinePointsMapper.selectOne(new LambdaQueryWrapper<FanWaylinePoints>()
                .eq(FanWaylinePoints::getJobId, jobId));
        JSONArray djiPoints = JSON.parseArray(fanWaylinePoints.getDjiFanPoints());
        JSONArray videoPoints = JSON.parseArray(fanWaylinePoints.getVideoFanPoints());
        Integer jobType = fanWaylinePoints.getJobType();
        int pointCount=0;
        if(jobType==0){
             pointCount = djiPoints.size();
        }else if(jobType==1){
             pointCount = djiPoints.size()+videoPoints.size();
        }

        String taskDesp = "本次巡检 1 台风机，共计 "+pointCount+ "个巡检点位，其中迎风面 "+pointCount/2+" 个点位，背风面 "+pointCount/2+" 个点位。\n" +
                "识别缺陷 " + count + " 处。";

        // 2. 创建Word文档
        XWPFDocument document = new XWPFDocument();

        // 3. 插入标题
        XWPFParagraph title = document.createParagraph();
        XWPFRun titleRun = title.createRun();
        titleRun.setText("东方电子风场巡检报告");
        titleRun.setBold(true);
        titleRun.setFontSize(18); // 字体大小
        titleRun.setUnderline(UnderlinePatterns.SINGLE); // 下划线
        title.setAlignment(ParagraphAlignment.CENTER); // 居中对齐

        // 4. 插入巡检单位、任务名、任务时间、任务描述
        XWPFParagraph unit = document.createParagraph();
        XWPFRun unitRun = unit.createRun();
        unitRun.setText("巡检单位：东方电子股份有限公司");
        unitRun.setFontSize(12);

        XWPFParagraph taskNamePara = document.createParagraph();
        XWPFRun taskNameRun = taskNamePara.createRun();
        taskNameRun.setText("任务名：" + taskName);
        taskNameRun.setFontSize(12);

        XWPFParagraph taskTimePara = document.createParagraph();
        XWPFRun taskTimeRun = taskTimePara.createRun();
        taskTimeRun.setText("任务时间：" + formattedTime);
        taskTimeRun.setFontSize(12);

        XWPFParagraph taskDespPara = document.createParagraph();
        XWPFRun taskDespRun = taskDespPara.createRun();
        taskDespRun.setText("任务描述：" + taskDesp);
        taskDespRun.setFontSize(12);

        // 5. 插入“缺陷分析详情”表格
        // 创建指定行列的表格，1行8列对应表头
        String[] headerTitles = {"序号", "扇叶名称", "扇叶部位", "采集时间", "缺陷主要类型", "缺陷描述", "图片详情"};
        XWPFTable table = document.createTable(1, headerTitles.length);
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headerTitles.length; i++) {
            XWPFTableCell headerCell = headerRow.getCell(i);
            XWPFParagraph headerPara = headerCell.getParagraphArray(0);
            XWPFRun headerRun = headerPara.createRun();
            headerRun.setText(headerTitles[i]);
            headerRun.setBold(true);
            headerRun.setFontSize(12);
        }


     // 自定义方法，返回缺陷实体列表
        for (int i = 0; i < defectList.size(); i++) {
            DefectEntity defect = defectList.get(i);
            XWPFTableRow row = table.createRow();
            XWPFTableCell cell1 = row.getCell(0);
            cell1.getParagraphArray(0).createRun().setText(String.valueOf(i + 1));
            XWPFTableCell cell2 = row.getCell(1);
            cell2.getParagraphArray(0).createRun().setText(defect.getFanCode());
            XWPFTableCell cell3 = row.getCell(2);
            cell3.getParagraphArray(0).createRun().setText(defect.getFanPart());
            XWPFTableCell cell4 = row.getCell(3);
            cell4.getParagraphArray(0).createRun().setText(defect.getAcquisitionTime());
            XWPFTableCell cell5 = row.getCell(4);
            cell5.getParagraphArray(0).createRun().setText(defect.getDefectType());
            XWPFTableCell cell6 = row.getCell(5);
            cell6.getParagraphArray(0).createRun().setText(defect.getDefectDescription());
            // 修改图片插入部分，添加压缩和尺寸控制
            XWPFTableCell cell7 = row.getCell(6);
            XWPFRun imageRun = cell7.getParagraphArray(0).createRun();
            String imagePath = defect.getImagePath();
            String imageName = extractFileName(imagePath);

            if (new File(imagePath).exists()) {
                try {
                    // 压缩图片后再插入
                    byte[] compressedImage = compressImage(imagePath, 0.7f, 800, 600); // 质量0.7，最大尺寸800x600
                    imageRun.addPicture(
                            new ByteArrayInputStream(compressedImage),
                            XWPFDocument.PICTURE_TYPE_JPEG,
                            imageName,
                            Units.toEMU(80),  // 减小显示尺寸到80像素
                            Units.toEMU(60)   // 减小显示尺寸到60像素
                    );
                } catch (Exception e) {
                    imageRun.setText("图片处理失败");
                }
            } else {
                imageRun.setText("无图片");
            }
        }

        // 6. 保存Word文档
//        String reportPath = "D:\\report\\巡检报告_" + taskName + ".docx";
        String reportPath = fileConfig.getFileReportPath() + "/"+ taskName +".docx";
        try (FileOutputStream fos = new FileOutputStream(reportPath)) {
            document.write(fos);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("报告生成成功：" + reportPath);
    }

    @Override
    public List<String> generateFileNames(List<MediaFileEntity> mediaFileEntities, JSONArray points) {
        List<String> fileNames = new ArrayList<>();
        int index = 0;

        for (MediaFileEntity mediaFileEntity : mediaFileEntities) {
            String fileName;
            if (index < points.size()) {
                // 使用Redis中的命名规则
                String pointName = points.getString(index);
                fileName = pointName + ".jpg";
            } else {
                // 如果图片数量超过Redis规则，使用原始文件名
                String originalName = mediaFileEntity.getFileName() != null ?
                        mediaFileEntity.getFileName() :
                        "file_" + mediaFileEntity.getFileId();
                // 确保文件扩展名
                if (!originalName.toLowerCase().endsWith(".jpg") &&
                        !originalName.toLowerCase().endsWith(".jpeg")) {
                    fileName = originalName + ".jpg";
                } else {
                    fileName = originalName;
                }
            }
            fileNames.add(fileName);
            index++;
        }
        return fileNames;
    }

    @Override
    public AnalysisResponse sendAnalysisRequest(AnalysisRequest request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(waylineUrlConfig.getAnalysisUrl()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() == 200) {
                return objectMapper.readValue(httpResponse.body(), AnalysisResponse.class);
            } else {
                System.err.println("请求失败，状态码: " + httpResponse.statusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("发送分析请求时发生错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 图片压缩方法
     */
    private byte[] compressImage(String imagePath, float quality, int maxWidth, int maxHeight) throws IOException {
        BufferedImage originalImage = ImageIO.read(new File(imagePath));

        // 计算缩放比例
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        double scale = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
        scale = Math.min(scale, 1.0); // 只缩小不放大

        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);

        // 创建缩放后的图片
        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();

        // 压缩质量
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(scaledImage, null, null), param);
        }
        writer.dispose();

        return baos.toByteArray();
    }

    public static String extractFileName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        // 处理正斜杠
        int lastForwardSlash = path.lastIndexOf('/');
        if (lastForwardSlash >= 0) {
            return path.substring(lastForwardSlash + 1);
        }

        // 处理反斜杠（Windows路径）
        int lastBackwardSlash = path.lastIndexOf('\\');
        if (lastBackwardSlash >= 0) {
            return path.substring(lastBackwardSlash + 1);
        }

        // 没有斜杠，直接返回原字符串
        return path;
    }



    /**
     * 将API响应数据转换为缺陷对象列表
     * @param response API响应数据
     * @return 缺陷对象列表
     */
    public List<DefectEntity> convertToDefects(AnalysisResponse response) {
        List<DefectEntity> defects = new ArrayList<>();
        String currentTime = getCurrentTime();

        for (AnalysisResponse.ResultItem item : response.getResultsList()) {
            // 处理所有类型的desc数据
            if (item.isDescList()) {
                // desc是列表类型
                List<String> defectTypes = item.getDescAsList();
                if (defectTypes != null) {
                    DefectEntity defect = createDefectFromResult(item, defectTypes, currentTime);
                    defects.add(defect);
                }
            } else if (item.isDescString()) {
                // desc是字符串类型，包括"无缺陷/无结果"
                String descString = item.getDescAsString();
                if (descString != null) {
                    // 将字符串转换为单元素列表
                    List<String> defectTypes = Collections.singletonList(descString);
                    DefectEntity defect = createDefectFromResult(item, defectTypes, currentTime);
                    defects.add(defect);
                }
            }
        }

        return defects;
    }

    /**
     * 从结果项创建缺陷对象
     */
    private DefectEntity createDefectFromResult(AnalysisResponse.ResultItem item,
                                                List<String> defectTypes, String currentTime) {
        String imagePath = item.getResImagePath();
        String[] parsedInfo = parseImageInfo(imagePath);

        DefectEntity defect = new DefectEntity();
        defect.setFanCode(parsedInfo[0]);
        // 检查是否包含_result0后缀
        String FanPart=null;
        if (parsedInfo[1].endsWith("_result0")) {
            FanPart= parsedInfo[1].substring(0, parsedInfo[1].length() - 8);
        }else {
            FanPart= parsedInfo[1];
        }
        defect.setFanPart(FanPart);
        defect.setAcquisitionTime(currentTime);
        defect.setImagePath(imagePath);


        // 设置缺陷类型和描述
        Map<String, Integer> defectCount = countDefectTypes(defectTypes);
        String mainDefectType = getMainDefectType(defectCount);
        defect.setDefectType(mainDefectType);
        defect.setDefectDescription(generateDefectDescription(defectCount));

        return defect;
    }

    /**
     * 统计缺陷类型数量
     */
    private Map<String, Integer> countDefectTypes(List<String> defectTypes) {
        Map<String, Integer> countMap = new HashMap<>();
        for (String type : defectTypes) {
            countMap.put(type, countMap.getOrDefault(type, 0) + 1);
        }
        return countMap;
    }

    /**
     * 获取主要缺陷类型
     */
    private String getMainDefectType(Map<String, Integer> defectCount) {
        return defectCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("未知缺陷");
    }

    /**
     * 生成缺陷描述
     */
    private String generateDefectDescription(Map<String, Integer> defectCount) {
        StringBuilder description = new StringBuilder();
        for (Map.Entry<String, Integer> entry : defectCount.entrySet()) {
            if (description.length() > 0) {
                description.append("; ");
            }
            description.append(entry.getKey()).append("(").append(entry.getValue()).append("处)");
        }
        return description.toString();
    }

    /**
     * 解析图片路径信息
     */
    private String[] parseImageInfo(String imagePath) {
        String fileName = getFileNameFromPath(imagePath);
        String fanCode = "未知风机";
        String fanPart = "未知部件";

        try {
            String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
            String[] parts = nameWithoutExt.split("-");

            if (parts.length >= 1) {
                fanCode = parts[0].trim();
            }
            if (parts.length >= 2) {
                fanPart = parts[1].trim();
            }
            if (parts.length >= 3) {
                fanPart += "-" + parts[2].trim();
            }
        } catch (Exception e) {
            System.err.println("解析图片路径失败: " + imagePath);
        }

        return new String[]{fanCode, fanPart};
    }

    /**
     * 从路径中提取文件名
     */
    private String getFileNameFromPath(String filePath) {
        if (filePath == null) return "未知文件";
        int lastSlash = filePath.lastIndexOf('/');
        return lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;
    }

    /**
     * 获取当前时间
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 处理API响应并新增缺陷数据
     * @param response API响应数据
     */
    public void processAndAddDefects(AnalysisResponse response,String jobId) {
        if (response == null || response.getResultsList() == null) {
            System.out.println("响应数据为空");
            return;
        }

        List<DefectEntity> defects = convertToDefects(response);
        addDefects(defects,jobId);
    }

    /**
     * 新增缺陷数据
     */
    public void addDefects(List<DefectEntity> defects,String jobId) {
        if (defects == null || defects.isEmpty()) {
            System.out.println("没有需要新增的缺陷数据");
            return;
        }

        System.out.println("开始新增 " + defects.size() + " 条缺陷数据:");

        for (int i = 0; i < defects.size(); i++) {
            DefectEntity defect = defects.get(i);
            defect.setJobId(jobId);
            defectEntityMapper.insert(defect);
            System.out.println((i + 1) + ". " + defect);
        }

        System.out.println("缺陷数据新增完成");
    }
    @Override
    public void downloadDocxFile(String filePath, HttpServletResponse response) {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new RuntimeException("文件不存在: " + filePath);
        }

        if (!filePath.toLowerCase().endsWith(".docx")) {
            throw new RuntimeException("文件格式错误，只支持DOCX格式");
        }

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + new String(file.getName().getBytes("UTF-8"), "ISO-8859-1"));
            response.setHeader("Content-Length", String.valueOf(file.length()));

            // 将文件写入响应流
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();

        } catch (IOException e) {
            throw new RuntimeException("文件下载失败");
        }
    }

}
