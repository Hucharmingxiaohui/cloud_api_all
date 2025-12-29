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
import java.util.function.Function;

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
import java.util.stream.Collectors;

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
//    @Override
//    public void genPatrolTaskWordNew(String reportId,  String jobId) {
//
//        // 1. 从数据库获取巡检任务、风机、缺陷等信息
//        FjReportEntity fjReportEntity = fjReportMapper.selectById(reportId);
//        String taskName = fjReportEntity.getName();
//        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(
//                new LambdaQueryWrapper<WaylineJobEntity>()
//                        .eq(WaylineJobEntity::getJobId, jobId)
//        );
//        Long beginTime = waylineJobEntity.getBeginTime();
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        String formattedTime = sdf.format(new Date(beginTime));
//
//        // 假设缺陷列表（实际需从数据库获取）
//        List<DefectEntity> defectList = defectEntityMapper.selectList(new LambdaQueryWrapper<DefectEntity>()
//                .eq(DefectEntity::getJobId,jobId));
////      缺陷数
//        long count = defectList.stream()
//                .filter(defect -> !defect.getDefectType().contains("无缺陷"))
//                .filter(defect -> !defect.getDefectType().contains("无结果"))
//                .count();
//        // 从Redis获取fanPoints数据并解析为JSONArray
////        String fanPointsStr = (String) redisUtils.get("fanPoints");
//        FanWaylinePoints fanWaylinePoints = fanWaylinePointsMapper.selectOne(new LambdaQueryWrapper<FanWaylinePoints>()
//                .eq(FanWaylinePoints::getJobId, jobId));
//        JSONArray djiPoints = JSON.parseArray(fanWaylinePoints.getDjiFanPoints());
//        JSONArray videoPoints = JSON.parseArray(fanWaylinePoints.getVideoFanPoints());
//        Integer jobType = fanWaylinePoints.getJobType();
//        int pointCount=0;
//        if(jobType==0){
//             pointCount = djiPoints.size();
//        }else if(jobType==1){
//             pointCount = djiPoints.size()+videoPoints.size();
//        }
//
//        String taskDesp = "本次巡检 1 台风机，共计 "+pointCount+ "个巡检点位，其中迎风面 "+pointCount/2+" 个点位，背风面 "+pointCount/2+" 个点位。\n" +
//                "识别缺陷 " + count + " 处。";
//
//        // 2. 创建Word文档
//        XWPFDocument document = new XWPFDocument();
//
//        // 3. 插入标题
//        XWPFParagraph title = document.createParagraph();
//        XWPFRun titleRun = title.createRun();
//        titleRun.setText("东方电子风场巡检报告");
//        titleRun.setBold(true);
//        titleRun.setFontSize(18); // 字体大小
//        titleRun.setUnderline(UnderlinePatterns.SINGLE); // 下划线
//        title.setAlignment(ParagraphAlignment.CENTER); // 居中对齐
//
//        // 4. 插入巡检单位、任务名、任务时间、任务描述
//        XWPFParagraph unit = document.createParagraph();
//        XWPFRun unitRun = unit.createRun();
//        unitRun.setText("巡检单位：东方电子股份有限公司");
//        unitRun.setFontSize(12);
//
//        XWPFParagraph taskNamePara = document.createParagraph();
//        XWPFRun taskNameRun = taskNamePara.createRun();
//        taskNameRun.setText("任务名：" + taskName);
//        taskNameRun.setFontSize(12);
//
//        XWPFParagraph taskTimePara = document.createParagraph();
//        XWPFRun taskTimeRun = taskTimePara.createRun();
//        taskTimeRun.setText("任务时间：" + formattedTime);
//        taskTimeRun.setFontSize(12);
//
//        XWPFParagraph taskDespPara = document.createParagraph();
//        XWPFRun taskDespRun = taskDespPara.createRun();
//        taskDespRun.setText("任务描述：" + taskDesp);
//        taskDespRun.setFontSize(12);
//
//        // 5. 插入“缺陷分析详情”表格
//        // 创建指定行列的表格，1行8列对应表头
//        String[] headerTitles = {"序号", "扇叶名称", "扇叶部位", "采集时间", "缺陷主要类型", "缺陷描述", "图片详情"};
//        XWPFTable table = document.createTable(1, headerTitles.length);
//        XWPFTableRow headerRow = table.getRow(0);
//        for (int i = 0; i < headerTitles.length; i++) {
//            XWPFTableCell headerCell = headerRow.getCell(i);
//            XWPFParagraph headerPara = headerCell.getParagraphArray(0);
//            XWPFRun headerRun = headerPara.createRun();
//            headerRun.setText(headerTitles[i]);
//            headerRun.setBold(true);
//            headerRun.setFontSize(12);
//        }
//
//
//     // 自定义方法，返回缺陷实体列表
//        for (int i = 0; i < defectList.size(); i++) {
//            DefectEntity defect = defectList.get(i);
//            XWPFTableRow row = table.createRow();
//            XWPFTableCell cell1 = row.getCell(0);
//            cell1.getParagraphArray(0).createRun().setText(String.valueOf(i + 1));
//            XWPFTableCell cell2 = row.getCell(1);
//            cell2.getParagraphArray(0).createRun().setText(defect.getFanCode());
//            XWPFTableCell cell3 = row.getCell(2);
//            cell3.getParagraphArray(0).createRun().setText(defect.getFanPart());
//            XWPFTableCell cell4 = row.getCell(3);
//            cell4.getParagraphArray(0).createRun().setText(defect.getAcquisitionTime());
//            XWPFTableCell cell5 = row.getCell(4);
//            cell5.getParagraphArray(0).createRun().setText(defect.getDefectType());
//            XWPFTableCell cell6 = row.getCell(5);
//            cell6.getParagraphArray(0).createRun().setText(defect.getDefectDescription());
//            // 修改图片插入部分，添加压缩和尺寸控制
//            XWPFTableCell cell7 = row.getCell(6);
//            XWPFRun imageRun = cell7.getParagraphArray(0).createRun();
//            String imagePath = defect.getImagePath();
//            String imageName = extractFileName(imagePath);
//
//            if (new File(imagePath).exists()) {
//                try {
//                    // 压缩图片后再插入
//                    byte[] compressedImage = compressImage(imagePath, 0.7f, 800, 600); // 质量0.7，最大尺寸800x600
//                    imageRun.addPicture(
//                            new ByteArrayInputStream(compressedImage),
//                            XWPFDocument.PICTURE_TYPE_JPEG,
//                            imageName,
//                            Units.toEMU(80),  // 减小显示尺寸到80像素
//                            Units.toEMU(60)   // 减小显示尺寸到60像素
//                    );
//                } catch (Exception e) {
//                    imageRun.setText("图片处理失败");
//                }
//            } else {
//                imageRun.setText("无图片");
//            }
//        }
//
//        // 6. 保存Word文档
////        String reportPath = "D:\\report\\巡检报告_" + taskName + ".docx";
//        String reportPath = fileConfig.getFileReportPath() + "/"+ taskName +".docx";
//        try (FileOutputStream fos = new FileOutputStream(reportPath)) {
//            document.write(fos);
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        System.out.println("报告生成成功：" + reportPath);
//    }


    @Override
    public void genPatrolTaskWordNew(String reportId, String jobId) {
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

        // 风机编号写死为"7号风机"（后面可以修改为从数据库获取）

        String fanCode = waylineJobEntity.getFanName();

        // 风电场名称写死
        String windFarmName = "广西龙源蔚蓝风电场";

        // 获取缺陷列表
        List<DefectEntity> defectList = defectEntityMapper.selectList(new LambdaQueryWrapper<DefectEntity>()
                .eq(DefectEntity::getJobId, jobId));

        // 计算缺陷数量
        long count = defectList.stream()
                .filter(defect -> !defect.getDefectType().contains("无缺陷"))
                .filter(defect -> !defect.getDefectType().contains("无结果"))
                .count();

        String mostCommonDefectType = defectList.stream()
                .filter(defect -> !defect.getDefectType().contains("无缺陷"))
                .filter(defect -> !defect.getDefectType().contains("无结果"))
                .map(DefectEntity::getDefectType)  // 获取缺陷类型
                .filter(defectType -> defectType != null && !defectType.trim().isEmpty())  // 过滤空值
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))  // 按类型分组计数
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())  // 找到出现次数最多的
                .map(Map.Entry::getKey)  // 获取缺陷类型
                .orElse("无缺陷");  // 如果没有缺陷，返回"无"

        // 获取巡检点位信息
        FanWaylinePoints fanWaylinePoints = fanWaylinePointsMapper.selectOne(new LambdaQueryWrapper<FanWaylinePoints>()
                .eq(FanWaylinePoints::getJobId, jobId));
        JSONArray djiPoints = JSON.parseArray(fanWaylinePoints.getDjiFanPoints());
        JSONArray videoPoints = JSON.parseArray(fanWaylinePoints.getVideoFanPoints());
        Integer jobType = fanWaylinePoints.getJobType();
        int pointCount = 0;
        if (jobType == 0) {
            pointCount = djiPoints.size();
        } else if (jobType == 1) {
            pointCount = djiPoints.size() + videoPoints.size();
        }

        // 2. 创建Word文档
        XWPFDocument document = new XWPFDocument();

        // 3. 添加标题部分（模拟第一种报告的格式）
        // 添加风电场名称标题
        XWPFParagraph farmTitle = document.createParagraph();
        XWPFRun farmTitleRun = farmTitle.createRun();
        farmTitleRun.setText(windFarmName);
        farmTitleRun.setBold(true);
        farmTitleRun.setFontSize(18);
        farmTitle.setAlignment(ParagraphAlignment.CENTER);

        // 添加报告类型标题
        XWPFParagraph reportTitle = document.createParagraph();
        XWPFRun reportTitleRun = reportTitle.createRun();
        reportTitleRun.setText("无人机叶片智能巡检结果分析报告");
        reportTitleRun.setBold(true);
        reportTitleRun.setFontSize(16);
        reportTitle.setAlignment(ParagraphAlignment.CENTER);

        // 添加公司名称
        XWPFParagraph companyTitle = document.createParagraph();
        XWPFRun companyTitleRun = companyTitle.createRun();
        companyTitleRun.setText("东方电子股份有限公司");
        companyTitleRun.setBold(true);
        companyTitleRun.setFontSize(14);
        companyTitle.setAlignment(ParagraphAlignment.CENTER);

        // 添加日期
        XWPFParagraph datePara = document.createParagraph();
        XWPFRun dateRun = datePara.createRun();
        dateRun.setText(formattedTime);
        dateRun.setFontSize(12);
        datePara.setAlignment(ParagraphAlignment.CENTER);

        // 添加空行
        document.createParagraph();

        // 添加基本信息表格（风机编号和巡检地点）
        XWPFTable infoTable = document.createTable(2, 2);
        infoTable.setWidth("100%");

        XWPFTableRow row1 = infoTable.getRow(0);
        XWPFTableCell cell11 = row1.getCell(0);
        cell11.setText("风机编号");
        cell11.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        cell11.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
        XWPFTableCell cell12 = row1.getCell(1);
        cell12.setText(fanCode);
        cell12.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        cell12.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

        XWPFTableRow row2 = infoTable.getRow(1);
        XWPFTableCell cell21 = row2.getCell(0);
        cell21.setText("巡检地点");
        cell21.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        cell21.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
        XWPFTableCell cell22 = row2.getCell(1);
        cell22.setText(windFarmName);
        cell22.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        cell22.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

        // 添加空行
        document.createParagraph();

        // 添加编制单位等信息
        XWPFParagraph unitPara = document.createParagraph();
        XWPFRun unitRun = unitPara.createRun();
        unitRun.setText("编制单位：东方电子股份有限公司");
        unitRun.setFontSize(11);
        unitPara.setAlignment(ParagraphAlignment.LEFT);

        XWPFParagraph addressPara = document.createParagraph();
        XWPFRun addressRun = addressPara.createRun();
        addressRun.setText("地址：山东省烟台市芝罘区机场路2号");
        addressRun.setFontSize(11);
        addressPara.setAlignment(ParagraphAlignment.LEFT);

        XWPFParagraph contactPara = document.createParagraph();
        XWPFRun contactRun = contactPara.createRun();
        contactRun.setText("联系方式：0535-5520188");
        contactRun.setFontSize(11);
        contactPara.setAlignment(ParagraphAlignment.LEFT);

        // 添加空行
        document.createParagraph();

        // 添加注意事项标题
        XWPFParagraph noticeTitle = document.createParagraph();
        XWPFRun noticeTitleRun = noticeTitle.createRun();
        noticeTitleRun.setText("注意事项");
        noticeTitleRun.setBold(true);
        noticeTitleRun.setFontSize(12);
        noticeTitle.setAlignment(ParagraphAlignment.LEFT);

        // 添加注意事项内容
        XWPFParagraph notice1 = document.createParagraph();
        XWPFRun notice1Run = notice1.createRun();
        notice1Run.setText("1．风电场无人机叶片智能巡检项目针对风电机组叶片外部进行检测，叶片内部检测不在此巡检作业范围。");
        notice1Run.setFontSize(11);
        notice1.setAlignment(ParagraphAlignment.LEFT);

        XWPFParagraph notice2 = document.createParagraph();
        XWPFRun notice2Run = notice2.createRun();
        notice2Run.setText("2．风电场无人机叶片巡检结果分析报告根据行业标准《NB/T 10593-2021风电场无人机叶片检测技术规范》进行编制，并提供相关处理建议。风电场在参照执行时，请结合现场实际情况进行。");
        notice2Run.setFontSize(11);
        notice2.setAlignment(ParagraphAlignment.LEFT);

        XWPFParagraph notice3 = document.createParagraph();
        XWPFRun notice3Run = notice3.createRun();
        notice3Run.setText("3.对报告若有异议，请在收到报告之日起一个月内向本公司提出，逾期不再受理。");
        notice3Run.setFontSize(11);
        notice3.setAlignment(ParagraphAlignment.LEFT);

        XWPFParagraph notice4 = document.createParagraph();
        XWPFRun notice4Run = notice4.createRun();
        notice4Run.setText("4．未经中能电力科技开发有限公司书面许可，部分复制、摘用或篡改本报告内容，引起法律纠纷，责任自负。");
        notice4Run.setFontSize(11);
        notice4.setAlignment(ParagraphAlignment.LEFT);

        // 添加空行
        document.createParagraph();

        // 4. 添加报告概述
        XWPFParagraph section1Title = document.createParagraph();
        XWPFRun section1Run = section1Title.createRun();
        section1Run.setText("1 报告概述");
        section1Run.setBold(true);
        section1Run.setFontSize(14);
        section1Title.setAlignment(ParagraphAlignment.LEFT);

        // 1.1 巡检概况
        XWPFParagraph subSection11 = document.createParagraph();
        XWPFRun subSection11Run = subSection11.createRun();
        subSection11Run.setText("1.1 巡检概况");
        subSection11Run.setBold(true);
        subSection11Run.setFontSize(12);
        subSection11.setAlignment(ParagraphAlignment.LEFT);

        // 创建巡检概况表格
        XWPFTable overviewTable = document.createTable(4, 2);
        overviewTable.setWidth("100%");

        // 填充巡检概况表格
        String[][] overviewData = {
                {"委托单位", windFarmName},
                {"巡检时间", formattedTime},
                {"点位数量", String.valueOf(pointCount)},
                {"风机编号", fanCode}
        };

        for (int i = 0; i < 4; i++) {
            XWPFTableRow row = overviewTable.getRow(i);
            row.getCell(0).setText(overviewData[i][0]);
            row.getCell(0).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
            row.getCell(1).setText(overviewData[i][1]);
            row.getCell(1).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
        }

        // 添加主要缺陷项目
        XWPFParagraph defectSummary = document.createParagraph();
        XWPFRun defectSummaryRun = defectSummary.createRun();
        defectSummaryRun.setText("使用无人机检测叶片表面存在的主要缺陷项目如下："+mostCommonDefectType);
        defectSummaryRun.setFontSize(11);
        defectSummary.setAlignment(ParagraphAlignment.LEFT);

        // 添加空行
        document.createParagraph();

        // 1.2 无人机巡检叶片典型缺陷类型分类
        XWPFParagraph subSection12 = document.createParagraph();
        XWPFRun subSection12Run = subSection12.createRun();
        subSection12Run.setText("1.2无人机巡检叶片典型缺陷类型分类");
        subSection12Run.setBold(true);
        subSection12Run.setFontSize(12);
        subSection12.setAlignment(ParagraphAlignment.LEFT);

        // 创建缺陷类型分类表格
        XWPFTable defectTypeTable = document.createTable(9, 2);
        defectTypeTable.setWidth("100%");

        // 填充表头
        XWPFTableRow headerRow = defectTypeTable.getRow(0);
        headerRow.getCell(0).setText("缺陷类型");
        headerRow.getCell(0).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
        headerRow.getCell(1).setText("缺陷数量");
        headerRow.getCell(1).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

        // 填充缺陷类型数据（这里写死数据，实际应该从数据库统计）
        String[][] defectTypeData = {
                {"雷击损伤", "0"},
                {"胶衣脱落", "0"},
                {"胶漆鼓包", "0"},
                {"轮毂漏油", "0"},
                {"叶片覆冰", "0"},
                {"叶片开裂", "0"},
                {"叶片腐蚀", "0"},
                {"塔筒腐蚀", "0"}
        };

        for (int i = 0; i < 8; i++) {
            XWPFTableRow row = defectTypeTable.getRow(i + 1);
            row.getCell(0).setText(defectTypeData[i][0]);
            row.getCell(0).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
            row.getCell(1).setText(defectTypeData[i][1]);
            row.getCell(1).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
        }

        // 添加空行
        document.createParagraph();

        // 1.3 巡检内容
        XWPFParagraph subSection13 = document.createParagraph();
        XWPFRun subSection13Run = subSection13.createRun();
        subSection13Run.setText("1.3 巡检内容");
        subSection13Run.setBold(true);
        subSection13Run.setFontSize(12);
        subSection13.setAlignment(ParagraphAlignment.LEFT);

        // 添加第一点巡检内容
        XWPFParagraph content1 = document.createParagraph();
        XWPFRun content1Run = content1.createRun();
        content1Run.setText("1）风机不停机巡检拍摄主要内容：分别拍摄风机三个叶片的迎风面、背风面、塔筒、轮毂、防雨裙；");
        content1Run.setFontSize(11);
        content1.setAlignment(ParagraphAlignment.LEFT);

        // 添加第二点巡检内容
        XWPFParagraph content2 = document.createParagraph();
        XWPFRun content2Run = content2.createRun();
        content2Run.setText("2）不停机时，无需对风机进行干预，无人机自动起飞，识别风机偏航角，并飞向风机正前方识别风机状态，自适应规划针对当前风机运行状态的巡检航线，在这个过程中依次拍摄：A、B、C叶片迎风面（用户可自行设置每叶片巡检段数）、迎风面塔筒（用户可自行设置塔筒巡检段数）、A、B、C叶片背风面（用户可自行设置每叶片巡检段数）、轮毂、背风面塔筒（用户可自行设置塔筒巡检段数）。");
        content2Run.setFontSize(11);
        content2.setAlignment(ParagraphAlignment.LEFT);

        // 添加空行
        document.createParagraph();

        // 5. 巡检结果汇总
        XWPFParagraph resultSummaryTitle = document.createParagraph();
        XWPFRun resultSummaryRun = resultSummaryTitle.createRun();
        resultSummaryRun.setText("2.巡检结果汇总");
        resultSummaryRun.setBold(true);
        resultSummaryRun.setFontSize(14);
        resultSummaryTitle.setAlignment(ParagraphAlignment.LEFT);

        // 任务描述
        XWPFParagraph taskDescription = document.createParagraph();
        XWPFRun taskDescriptionRun = taskDescription.createRun();
        taskDescriptionRun.setText("任务描述：本次巡检 1 台风机，共计 " + pointCount + "个巡检点位，其中迎风面 " +
                pointCount/2 + " 个点位，背风面 " + pointCount/2 + " 个点位。 识别缺陷 " + count + " 处。");
        taskDescriptionRun.setFontSize(11);
        taskDescription.setAlignment(ParagraphAlignment.LEFT);

        // 添加空行
        document.createParagraph();

        // 6. 添加缺陷详情（按第一种报告的格式）
        // 缺陷详情表格表头
        String[] defectHeaderTitles = {"序号", "扇叶名称", "扇叶部位", "采集时间", "缺陷主要类型", "缺陷描述"};

        for (int i = 0; i < defectList.size(); i++) {
            DefectEntity defect = defectList.get(i);

            // 创建缺陷信息表格（2行：表头行和数据行）
            XWPFTable defectTable = document.createTable(2, 6);
            defectTable.setWidth("100%");

            // 表头行
            XWPFTableRow headerRow1 = defectTable.getRow(0);
            for (int j = 0; j < defectHeaderTitles.length; j++) {
                XWPFTableCell cell = headerRow1.getCell(j);
                cell.setText(defectHeaderTitles[j]);
                // 设置表头居中
                cell.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
            }

            // 数据行
            XWPFTableRow dataRow = defectTable.getRow(1);
            dataRow.getCell(0).setText(String.valueOf(i + 1));
            dataRow.getCell(0).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
            dataRow.getCell(1).setText(defect.getFanCode());
            dataRow.getCell(1).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
            dataRow.getCell(2).setText(defect.getFanPart());
            dataRow.getCell(2).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
            dataRow.getCell(3).setText(defect.getAcquisitionTime());
            dataRow.getCell(3).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
            dataRow.getCell(4).setText(defect.getDefectType());
            dataRow.getCell(4).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
            dataRow.getCell(5).setText(defect.getDefectDescription());
            dataRow.getCell(5).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            // 添加空行
            document.createParagraph();

            // 添加图片（不压缩，大图，适应页面宽度）
            String imagePath = defect.getImagePath();
            if (new File(imagePath).exists()) {
                try {
                    XWPFParagraph imagePara = document.createParagraph();
                    imagePara.setAlignment(ParagraphAlignment.CENTER); // 图片居中
                    XWPFRun imageRun = imagePara.createRun();

                    // 使用原图，不压缩
                    FileInputStream fis = new FileInputStream(imagePath);

                    // 获取图片原始尺寸
                    BufferedImage bufferedImage = ImageIO.read(new File(imagePath));
                    int originalWidth = bufferedImage.getWidth();
                    int originalHeight = bufferedImage.getHeight();

                    // 尝试方法1：使用非常大的EMU值
                    // 1英寸 = 914400 EMU, 1厘米 = 360000 EMU
                    // 我们设置一个非常大的尺寸，比如20厘米宽，15厘米高

                    System.out.println("插入图片: " + imagePath);
                    System.out.println("原始尺寸: " + originalWidth + "x" + originalHeight + " 像素");

                    // 方法1：使用固定的大EMU值
                    int targetWidthEMU = 14 * 360000;  // 20厘米 = 7,200,000 EMU
                    int targetHeightEMU = 11 * 360000; // 15厘米 = 5,400,000 EMU

                    System.out.println("目标EMU尺寸: " + targetWidthEMU + "x" + targetHeightEMU + " EMU");

                    // 使用addPicture方法，直接传入EMU值
                    imageRun.addPicture(
                            fis,
                            XWPFDocument.PICTURE_TYPE_JPEG,
                            extractFileName(imagePath),
                            targetWidthEMU,
                            targetHeightEMU
                    );
                    fis.close();

                    // 图片后添加空行
                    document.createParagraph();
                } catch (Exception e) {
                    e.printStackTrace();
                    // 方法1失败，尝试方法2：使用更大的EMU值
                    try {
                        XWPFParagraph imagePara = document.createParagraph();
                        imagePara.setAlignment(ParagraphAlignment.CENTER);
                        XWPFRun imageRun = imagePara.createRun();

                        FileInputStream fis = new FileInputStream(imagePath);

                        // 方法2：使用非常大的EMU值（接近页面宽度）
                        // A4纸宽度约21厘米，高度29.7厘米
                        // 我们设置宽度为15厘米，高度按比例计算
                        double widthCm = 15.0; // 15厘米
                        double heightCm = 11.0; // 11厘米

                        int widthEMU = (int)(widthCm * 360000);  // 15厘米 = 5,400,000 EMU
                        int heightEMU = (int)(heightCm * 360000); // 11厘米 = 3,960,000 EMU

                        System.out.println("方法2目标尺寸: " + widthEMU + "x" + heightEMU + " EMU");

                        imageRun.addPicture(
                                fis,
                                XWPFDocument.PICTURE_TYPE_JPEG,
                                extractFileName(imagePath),
                                widthEMU,
                                heightEMU
                        );
                        fis.close();

                        document.createParagraph();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        // 方法3：尝试不使用ImageIO读取尺寸，直接使用原图
                        try {
                            XWPFParagraph imagePara = document.createParagraph();
                            imagePara.setAlignment(ParagraphAlignment.CENTER);
                            XWPFRun imageRun = imagePara.createRun();

                            FileInputStream fis = new FileInputStream(imagePath);

                            // 方法3：使用Word页面宽度（大约15厘米）
                            // 直接使用固定的大EMU值
                            int veryLargeWidthEMU = 10000000;  // 约27.8厘米
                            int veryLargeHeightEMU = 7500000;  // 约20.8厘米

                            imageRun.addPicture(
                                    fis,
                                    XWPFDocument.PICTURE_TYPE_JPEG,
                                    extractFileName(imagePath),
                                    veryLargeWidthEMU,
                                    veryLargeHeightEMU
                            );
                            fis.close();

                            document.createParagraph();
                        } catch (Exception ex2) {
                            ex2.printStackTrace();
                            XWPFParagraph errorPara = document.createParagraph();
                            errorPara.setAlignment(ParagraphAlignment.CENTER);
                            XWPFRun errorRun = errorPara.createRun();
                            errorRun.setText("图片插入失败: " + ex2.getMessage());
                        }
                    }
                }
            } else {
                XWPFParagraph noImagePara = document.createParagraph();
                noImagePara.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun noImageRun = noImagePara.createRun();
                noImageRun.setText("无图片，路径: " + imagePath);
            }

            // 添加空行分隔下一个缺陷
            document.createParagraph();
        }

        // 7. 保存Word文档
        String reportPath = fileConfig.getFileReportPath() + "/" + taskName + ".docx";
        try (FileOutputStream fos = new FileOutputStream(reportPath)) {
            document.write(fos);
            System.out.println("报告生成成功：" + reportPath);
        } catch (IOException e) {
            throw new RuntimeException("报告保存失败", e);
        }
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
    public Map<String, Integer> countDefectTypes(List<String> defectTypes) {
        Map<String, Integer> countMap = new HashMap<>();
        for (String type : defectTypes) {
            countMap.put(type, countMap.getOrDefault(type, 0) + 1);
        }
        return countMap;
    }

    /**
     * 获取主要缺陷类型
     */
    public String getMainDefectType(Map<String, Integer> defectCount) {
        return defectCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("未知缺陷");
    }

    /**
     * 生成缺陷描述
     */
    public String generateDefectDescription(Map<String, Integer> defectCount) {
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
