package com.dji.sample.df.wind.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.df.server.dto.HisUniTask.HisUniTaskParamsDTO;
import com.df.server.dto.HisUniTask.TaskReportDTO;
import com.df.server.entity.sys.SysDictDataEntity;
import com.df.server.mapper.sys.SysDictDataMapper;
import com.dji.sample.center.dao.UniPointMapper2;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.electricInspectionDf.service.ReportService;
import com.dji.sample.df.electricInspectionDf.service.impl.ResultServiceImpl;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileDTO;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.mediaDf.service.IFileServiceDf;
import com.dji.sample.df.wind.config.WaylineUrlConfig;
import com.dji.sample.df.wind.dao.*;
import com.dji.sample.df.wind.handler.PictureSaveHandler;
import com.dji.sample.df.wind.config.FjFileConfig;
import com.dji.sample.df.wind.model.entity.*;
import com.dji.sample.df.wind.service.FjReportService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.dto.WaylineJobDTO;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sample.wayline.service.impl.WaylineJobServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigInteger;
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

import static com.dji.sample.df.wind.utils.FileUtil.convert;

@Slf4j
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
    WaylineJobServiceImpl waylineJobServiceimpl;

    @Autowired
    private WaylineUrlConfig waylineUrlConfig;

    @Autowired
    IFileServiceDf iFileServiceDf;

    @Autowired
    RecgPointsEntityMapper recgPointsEntityMapper;

    @Autowired
    RecgFileEntityMapper recgFileEntityMapper;

    @Autowired
    UniPointMapper2 uniPointMapper2;

    @Autowired
    SysDictDataMapper sysDictDataMapper;

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


//    @Override
//    public void genPatrolTaskWordNew(String reportId, String jobId) {
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
//        // 风机编号写死为"7号风机"（后面可以修改为从数据库获取）
//
//        String fanCode = waylineJobEntity.getFanName();
//
//        // 风电场名称写死
//        String windFarmName = "广西龙源蔚蓝风电场";
//
//        // 获取缺陷列表
//        List<DefectEntity> defectList = defectEntityMapper.selectList(new LambdaQueryWrapper<DefectEntity>()
//                .eq(DefectEntity::getJobId, jobId));
//
//        // 计算缺陷数量
//        long count = defectList.stream()
//                .filter(defect -> !defect.getDefectType().contains("无缺陷"))
//                .filter(defect -> !defect.getDefectType().contains("无结果"))
//                .count();
//
//        String mostCommonDefectType = defectList.stream()
//                .filter(defect -> !defect.getDefectType().contains("无缺陷"))
//                .filter(defect -> !defect.getDefectType().contains("无结果"))
//                .map(DefectEntity::getDefectType)  // 获取缺陷类型
//                .filter(defectType -> defectType != null && !defectType.trim().isEmpty())  // 过滤空值
//                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))  // 按类型分组计数
//                .entrySet()
//                .stream()
//                .max(Map.Entry.comparingByValue())  // 找到出现次数最多的
//                .map(Map.Entry::getKey)  // 获取缺陷类型
//                .orElse("无缺陷");  // 如果没有缺陷，返回"无"
//
//        // 获取巡检点位信息
//        FanWaylinePoints fanWaylinePoints = fanWaylinePointsMapper.selectOne(new LambdaQueryWrapper<FanWaylinePoints>()
//                .eq(FanWaylinePoints::getJobId, jobId));
//        JSONArray djiPoints = JSON.parseArray(fanWaylinePoints.getDjiFanPoints());
//        JSONArray videoPoints = JSON.parseArray(fanWaylinePoints.getVideoFanPoints());
//        Integer jobType = fanWaylinePoints.getJobType();
//        int pointCount = 0;
//        if (jobType == 0) {
//            pointCount = djiPoints.size();
//        } else if (jobType == 1) {
//            pointCount = djiPoints.size() + videoPoints.size();
//        }
//
//        // 2. 创建Word文档
//        XWPFDocument document = new XWPFDocument();
//
//        String projectPath = System.getProperty("user.dir");
//        String filePath = projectPath + File.separator + "file" + File.separator + "picture" + File.separator + "国家能源.png";
//
//        // 3. 添加标题部分（模拟第一种报告的格式）
//        // 添加风电场名称标题
//        XWPFParagraph farmTitle = document.createParagraph();
//        XWPFRun farmTitleRun = farmTitle.createRun();
//        farmTitleRun.setText(windFarmName);
//        farmTitleRun.setBold(true);
//        farmTitleRun.setFontSize(18);
//        farmTitle.setAlignment(ParagraphAlignment.CENTER);
//
//        // 添加报告类型标题
//        XWPFParagraph reportTitle = document.createParagraph();
//        XWPFRun reportTitleRun = reportTitle.createRun();
//        reportTitleRun.setText("无人机叶片智能巡检结果分析报告");
//        reportTitleRun.setBold(true);
//        reportTitleRun.setFontSize(16);
//        reportTitle.setAlignment(ParagraphAlignment.CENTER);
//
//        // 添加公司名称
//        XWPFParagraph companyTitle = document.createParagraph();
//        XWPFRun companyTitleRun = companyTitle.createRun();
//        companyTitleRun.setText("东方电子股份有限公司");
//        companyTitleRun.setBold(true);
//        companyTitleRun.setFontSize(14);
//        companyTitle.setAlignment(ParagraphAlignment.CENTER);
//
//        // 添加日期
//        XWPFParagraph datePara = document.createParagraph();
//        XWPFRun dateRun = datePara.createRun();
//        dateRun.setText(formattedTime);
//        dateRun.setFontSize(12);
//        datePara.setAlignment(ParagraphAlignment.CENTER);
//
//        // 添加空行
//        document.createParagraph();
//
//        // 添加基本信息表格（风机编号和巡检地点）
//        XWPFTable infoTable = document.createTable(2, 2);
//        infoTable.setWidth("100%");
//
//        XWPFTableRow row1 = infoTable.getRow(0);
//        XWPFTableCell cell11 = row1.getCell(0);
//        cell11.setText("风机编号");
//        cell11.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
//        cell11.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//        XWPFTableCell cell12 = row1.getCell(1);
//        cell12.setText(fanCode);
//        cell12.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
//        cell12.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//
//        XWPFTableRow row2 = infoTable.getRow(1);
//        XWPFTableCell cell21 = row2.getCell(0);
//        cell21.setText("巡检地点");
//        cell21.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
//        cell21.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//        XWPFTableCell cell22 = row2.getCell(1);
//        cell22.setText(windFarmName);
//        cell22.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
//        cell22.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//
//        // 添加空行
//        document.createParagraph();
//
//        // 添加编制单位等信息
//        XWPFParagraph unitPara = document.createParagraph();
//        XWPFRun unitRun = unitPara.createRun();
//        unitRun.setText("编制单位：东方电子股份有限公司");
//        unitRun.setFontSize(11);
//        unitPara.setAlignment(ParagraphAlignment.LEFT);
//
//        XWPFParagraph addressPara = document.createParagraph();
//        XWPFRun addressRun = addressPara.createRun();
//        addressRun.setText("地址：山东省烟台市芝罘区机场路2号");
//        addressRun.setFontSize(11);
//        addressPara.setAlignment(ParagraphAlignment.LEFT);
//
//        XWPFParagraph contactPara = document.createParagraph();
//        XWPFRun contactRun = contactPara.createRun();
//        contactRun.setText("联系方式：0535-5520188");
//        contactRun.setFontSize(11);
//        contactPara.setAlignment(ParagraphAlignment.LEFT);
//
//        // 添加空行
//        document.createParagraph();
//
//        // 添加注意事项标题
//        XWPFParagraph noticeTitle = document.createParagraph();
//        XWPFRun noticeTitleRun = noticeTitle.createRun();
//        noticeTitleRun.setText("注意事项");
//        noticeTitleRun.setBold(true);
//        noticeTitleRun.setFontSize(12);
//        noticeTitle.setAlignment(ParagraphAlignment.LEFT);
//
//        // 添加注意事项内容
//        XWPFParagraph notice1 = document.createParagraph();
//        XWPFRun notice1Run = notice1.createRun();
//        notice1Run.setText("1．风电场无人机叶片智能巡检项目针对风电机组叶片外部进行检测，叶片内部检测不在此巡检作业范围。");
//        notice1Run.setFontSize(11);
//        notice1.setAlignment(ParagraphAlignment.LEFT);
//
//        XWPFParagraph notice2 = document.createParagraph();
//        XWPFRun notice2Run = notice2.createRun();
//        notice2Run.setText("2．风电场无人机叶片巡检结果分析报告根据行业标准《NB/T 10593-2021风电场无人机叶片检测技术规范》进行编制，并提供相关处理建议。风电场在参照执行时，请结合现场实际情况进行。");
//        notice2Run.setFontSize(11);
//        notice2.setAlignment(ParagraphAlignment.LEFT);
//
//        XWPFParagraph notice3 = document.createParagraph();
//        XWPFRun notice3Run = notice3.createRun();
//        notice3Run.setText("3.对报告若有异议，请在收到报告之日起一个月内向本公司提出，逾期不再受理。");
//        notice3Run.setFontSize(11);
//        notice3.setAlignment(ParagraphAlignment.LEFT);
//
//        XWPFParagraph notice4 = document.createParagraph();
//        XWPFRun notice4Run = notice4.createRun();
//        notice4Run.setText("4．未经东方电子股份有限公司书面许可，部分复制、摘用或篡改本报告内容，引起法律纠纷，责任自负。");
//        notice4Run.setFontSize(11);
//        notice4.setAlignment(ParagraphAlignment.LEFT);
//
//        // 添加空行
//        document.createParagraph();
//
//        // 4. 添加报告概述
//        XWPFParagraph section1Title = document.createParagraph();
//        XWPFRun section1Run = section1Title.createRun();
//        section1Run.setText("1 报告概述");
//        section1Run.setBold(true);
//        section1Run.setFontSize(14);
//        section1Title.setAlignment(ParagraphAlignment.LEFT);
//
//        // 1.1 巡检概况
//        XWPFParagraph subSection11 = document.createParagraph();
//        XWPFRun subSection11Run = subSection11.createRun();
//        subSection11Run.setText("1.1 巡检概况");
//        subSection11Run.setBold(true);
//        subSection11Run.setFontSize(12);
//        subSection11.setAlignment(ParagraphAlignment.LEFT);
//
//        // 创建巡检概况表格
//        XWPFTable overviewTable = document.createTable(4, 2);
//        overviewTable.setWidth("100%");
//
//        // 填充巡检概况表格
//        String[][] overviewData = {
//                {"委托单位", windFarmName},
//                {"巡检时间", formattedTime},
//                {"点位数量", String.valueOf(pointCount)},
//                {"风机编号", fanCode}
//        };
//
//        for (int i = 0; i < 4; i++) {
//            XWPFTableRow row = overviewTable.getRow(i);
//            row.getCell(0).setText(overviewData[i][0]);
//            row.getCell(0).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//            row.getCell(1).setText(overviewData[i][1]);
//            row.getCell(1).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//        }
//
//        // 添加主要缺陷项目
//        XWPFParagraph defectSummary = document.createParagraph();
//        XWPFRun defectSummaryRun = defectSummary.createRun();
//        defectSummaryRun.setText("使用无人机检测叶片表面存在的主要缺陷项目如下："+mostCommonDefectType);
//        defectSummaryRun.setFontSize(11);
//        defectSummary.setAlignment(ParagraphAlignment.LEFT);
//
//        // 添加空行
//        document.createParagraph();
//
//        // 1.2 无人机巡检叶片典型缺陷类型分类
//        XWPFParagraph subSection12 = document.createParagraph();
//        XWPFRun subSection12Run = subSection12.createRun();
//        subSection12Run.setText("1.2无人机巡检叶片典型缺陷类型分类");
//        subSection12Run.setBold(true);
//        subSection12Run.setFontSize(12);
//        subSection12.setAlignment(ParagraphAlignment.LEFT);
//
//        // 创建缺陷类型分类表格
//        XWPFTable defectTypeTable = document.createTable(9, 2);
//        defectTypeTable.setWidth("100%");
//
//        // 填充表头
//        XWPFTableRow headerRow = defectTypeTable.getRow(0);
//        headerRow.getCell(0).setText("缺陷类型");
//        headerRow.getCell(0).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//        headerRow.getCell(1).setText("缺陷数量");
//        headerRow.getCell(1).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//
//        // 填充缺陷类型数据（这里写死数据，实际应该从数据库统计）
//        String[][] defectTypeData = {
//                {"雷击损伤", "0"},
//                {"胶衣脱落", "0"},
//                {"胶漆鼓包", "0"},
//                {"轮毂漏油", "0"},
//                {"叶片覆冰", "0"},
//                {"叶片开裂", "0"},
//                {"叶片腐蚀", "0"},
//                {"塔筒腐蚀", "0"}
//        };
//
//        for (int i = 0; i < 8; i++) {
//            XWPFTableRow row = defectTypeTable.getRow(i + 1);
//            row.getCell(0).setText(defectTypeData[i][0]);
//            row.getCell(0).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//            row.getCell(1).setText(defectTypeData[i][1]);
//            row.getCell(1).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//        }
//
//        // 添加空行
//        document.createParagraph();
//
//        // 1.3 巡检内容
//        XWPFParagraph subSection13 = document.createParagraph();
//        XWPFRun subSection13Run = subSection13.createRun();
//        subSection13Run.setText("1.3 巡检内容");
//        subSection13Run.setBold(true);
//        subSection13Run.setFontSize(12);
//        subSection13.setAlignment(ParagraphAlignment.LEFT);
//
//        // 添加第一点巡检内容
//        XWPFParagraph content1 = document.createParagraph();
//        XWPFRun content1Run = content1.createRun();
//        content1Run.setText("1）风机不停机巡检拍摄主要内容：分别拍摄风机三个叶片的迎风面、背风面、塔筒、轮毂、防雨裙；风机停机巡检拍摄主要内容：分别拍摄风机三个叶片的前缘、后缘、塔筒、轮毂、防雨裙。");
//        content1Run.setFontSize(11);
//        content1.setAlignment(ParagraphAlignment.LEFT);
//
//        // 添加第二点巡检内容
//        XWPFParagraph content2 = document.createParagraph();
//        XWPFRun content2Run = content2.createRun();
//        content2Run.setText("2）不停机时，无需对风机进行干预（停机时，需对风机上锁，保证偏航和叶轮方位角恒定），无人机自动起飞，识别风机偏航角，并飞向风机正前方识别风机状态，自适应规划针对当前风机运行状态的巡检航线，在这个过程中依次拍摄：A、B、C叶片迎风面或前缘（用户可自行设置每叶片巡检段数）、迎风面塔筒（用户可自行设置塔筒巡检段数）、A、B、C叶片背风面或后缘（用户可自行设置每叶片巡检段数）、轮毂、背风面塔筒（用户可自行设置塔筒巡检段数）。");
//        content2Run.setFontSize(11);
//        content2.setAlignment(ParagraphAlignment.LEFT);
//
//        // 添加空行
//        document.createParagraph();
//
//        // 5. 巡检结果汇总
//        XWPFParagraph resultSummaryTitle = document.createParagraph();
//        XWPFRun resultSummaryRun = resultSummaryTitle.createRun();
//        resultSummaryRun.setText("2.巡检结果汇总");
//        resultSummaryRun.setBold(true);
//        resultSummaryRun.setFontSize(14);
//        resultSummaryTitle.setAlignment(ParagraphAlignment.LEFT);
//
//        // 任务描述
//        XWPFParagraph taskDescription = document.createParagraph();
//        XWPFRun taskDescriptionRun = taskDescription.createRun();
//        taskDescriptionRun.setText("任务描述：本次巡检 1 台风机，共计 " + pointCount + "个巡检点位，其中迎风面 " +
//                pointCount/2 + " 个点位，背风面 " + pointCount/2 + " 个点位。 识别缺陷 " + count + " 处。");
//        taskDescriptionRun.setFontSize(11);
//        taskDescription.setAlignment(ParagraphAlignment.LEFT);
//
//        // 添加空行
//        document.createParagraph();
//
//        // 6. 添加缺陷详情（按第一种报告的格式）
//        // 缺陷详情表格表头
//        String[] defectHeaderTitles = {"序号", "扇叶名称", "扇叶部位", "采集时间", "缺陷主要类型", "缺陷描述"};
//
//        for (int i = 0; i < defectList.size(); i++) {
//            DefectEntity defect = defectList.get(i);
//
//            // 创建缺陷信息表格（2行：表头行和数据行）
//            XWPFTable defectTable = document.createTable(2, 6);
//            defectTable.setWidth("100%");
//
//            // 表头行
//            XWPFTableRow headerRow1 = defectTable.getRow(0);
//            for (int j = 0; j < defectHeaderTitles.length; j++) {
//                XWPFTableCell cell = headerRow1.getCell(j);
//                cell.setText(defectHeaderTitles[j]);
//                // 设置表头居中
//                cell.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//            }
//
//            // 数据行
//            XWPFTableRow dataRow = defectTable.getRow(1);
//            dataRow.getCell(0).setText(String.valueOf(i + 1));
//            dataRow.getCell(0).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//            dataRow.getCell(1).setText(defect.getFanCode());
//            dataRow.getCell(1).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//            dataRow.getCell(2).setText(defect.getFanPart());
//            dataRow.getCell(2).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//            dataRow.getCell(3).setText(defect.getAcquisitionTime());
//            dataRow.getCell(3).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//            dataRow.getCell(4).setText(defect.getDefectType());
//            dataRow.getCell(4).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//            dataRow.getCell(5).setText(defect.getDefectDescription());
//            dataRow.getCell(5).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
//
//            // 添加空行
//            document.createParagraph();
//
//            // 添加图片（不压缩，大图，适应页面宽度）
//            String imagePath = defect.getImagePath();
//            if (new File(imagePath).exists()) {
//                try {
//                    XWPFParagraph imagePara = document.createParagraph();
//                    imagePara.setAlignment(ParagraphAlignment.CENTER); // 图片居中
//                    XWPFRun imageRun = imagePara.createRun();
//
//                    // 使用原图，不压缩
//                    FileInputStream fis = new FileInputStream(imagePath);
//
//                    // 获取图片原始尺寸
//                    BufferedImage bufferedImage = ImageIO.read(new File(imagePath));
//                    int originalWidth = bufferedImage.getWidth();
//                    int originalHeight = bufferedImage.getHeight();
//
//                    // 尝试方法1：使用非常大的EMU值
//                    // 1英寸 = 914400 EMU, 1厘米 = 360000 EMU
//                    // 我们设置一个非常大的尺寸，比如20厘米宽，15厘米高
//
//                    System.out.println("插入图片: " + imagePath);
//                    System.out.println("原始尺寸: " + originalWidth + "x" + originalHeight + " 像素");
//
//                    // 方法1：使用固定的大EMU值
//                    int targetWidthEMU = 14 * 360000;  // 20厘米 = 7,200,000 EMU
//                    int targetHeightEMU = 11 * 360000; // 15厘米 = 5,400,000 EMU
//
//                    System.out.println("目标EMU尺寸: " + targetWidthEMU + "x" + targetHeightEMU + " EMU");
//
//                    // 使用addPicture方法，直接传入EMU值
//                    imageRun.addPicture(
//                            fis,
//                            XWPFDocument.PICTURE_TYPE_JPEG,
//                            extractFileName(imagePath),
//                            targetWidthEMU,
//                            targetHeightEMU
//                    );
//                    fis.close();
//
//                    // 图片后添加空行
//                    document.createParagraph();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    // 方法1失败，尝试方法2：使用更大的EMU值
//                    try {
//                        XWPFParagraph imagePara = document.createParagraph();
//                        imagePara.setAlignment(ParagraphAlignment.CENTER);
//                        XWPFRun imageRun = imagePara.createRun();
//
//                        FileInputStream fis = new FileInputStream(imagePath);
//
//                        // 方法2：使用非常大的EMU值（接近页面宽度）
//                        // A4纸宽度约21厘米，高度29.7厘米
//                        // 我们设置宽度为15厘米，高度按比例计算
//                        double widthCm = 15.0; // 15厘米
//                        double heightCm = 11.0; // 11厘米
//
//                        int widthEMU = (int)(widthCm * 360000);  // 15厘米 = 5,400,000 EMU
//                        int heightEMU = (int)(heightCm * 360000); // 11厘米 = 3,960,000 EMU
//
//                        System.out.println("方法2目标尺寸: " + widthEMU + "x" + heightEMU + " EMU");
//
//                        imageRun.addPicture(
//                                fis,
//                                XWPFDocument.PICTURE_TYPE_JPEG,
//                                extractFileName(imagePath),
//                                widthEMU,
//                                heightEMU
//                        );
//                        fis.close();
//
//                        document.createParagraph();
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                        // 方法3：尝试不使用ImageIO读取尺寸，直接使用原图
//                        try {
//                            XWPFParagraph imagePara = document.createParagraph();
//                            imagePara.setAlignment(ParagraphAlignment.CENTER);
//                            XWPFRun imageRun = imagePara.createRun();
//
//                            FileInputStream fis = new FileInputStream(imagePath);
//
//                            // 方法3：使用Word页面宽度（大约15厘米）
//                            // 直接使用固定的大EMU值
//                            int veryLargeWidthEMU = 10000000;  // 约27.8厘米
//                            int veryLargeHeightEMU = 7500000;  // 约20.8厘米
//
//                            imageRun.addPicture(
//                                    fis,
//                                    XWPFDocument.PICTURE_TYPE_JPEG,
//                                    extractFileName(imagePath),
//                                    veryLargeWidthEMU,
//                                    veryLargeHeightEMU
//                            );
//                            fis.close();
//
//                            document.createParagraph();
//                        } catch (Exception ex2) {
//                            ex2.printStackTrace();
//                            XWPFParagraph errorPara = document.createParagraph();
//                            errorPara.setAlignment(ParagraphAlignment.CENTER);
//                            XWPFRun errorRun = errorPara.createRun();
//                            errorRun.setText("图片插入失败: " + ex2.getMessage());
//                        }
//                    }
//                }
//            } else {
//                XWPFParagraph noImagePara = document.createParagraph();
//                noImagePara.setAlignment(ParagraphAlignment.CENTER);
//                XWPFRun noImageRun = noImagePara.createRun();
//                noImageRun.setText("无图片，路径: " + imagePath);
//            }
//
//            // 添加空行分隔下一个缺陷
//            document.createParagraph();
//        }
//
//        // 7. 保存Word文档
//        String reportPath = fileConfig.getFileReportPath() + "/" + taskName + ".docx";
//        try (FileOutputStream fos = new FileOutputStream(reportPath)) {
//            document.write(fos);
//            System.out.println("报告生成成功：" + reportPath);
//        } catch (IOException e) {
//            throw new RuntimeException("报告保存失败", e);
//        }
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
        String windFarmName = "云南高美风电场";

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

        // 设置默认字体
        if (document.getStyles() != null) {
            XWPFStyle style = document.getStyles().getStyle("Normal");
            if (style != null) {
                style.getCTStyle().getRPr().addNewRFonts().setAscii("宋体");
                style.getCTStyle().getRPr().addNewRFonts().setEastAsia("宋体");
            }
        }

        String projectPath = System.getProperty("user.dir");
        String logoPath = projectPath + File.separator + "file" + File.separator + "picture" + File.separator + "国家能源.png";
        log.info("文件路径");
        // ========== 第一页开始 ==========
        // 3. 添加第一页的内容（首页）
        // 先插入左上角logo图片
        try {
            if (new File(logoPath).exists()) {
                System.out.println("插入Logo图片: " + logoPath);

                // 创建一个段落用于放置logo
                XWPFParagraph logoPara = document.createParagraph();
                logoPara.setAlignment(ParagraphAlignment.LEFT);

                // 设置段落缩进，让图片靠左
                logoPara.setIndentationLeft(0);

                XWPFRun logoRun = logoPara.createRun();
                FileInputStream fis = new FileInputStream(logoPath);

                // 根据第二个word中的尺寸设置图片大小
                // 第二个word中Logo图片的尺寸是3.0416666666666665英寸宽，0.9888888888888889英寸高
                // 转换为厘米：1英寸 = 2.54厘米
                double widthCm = 3.0416666666666665 * 2.54;  // 约7.73厘米
                double heightCm = 0.9888888888888889 * 2.54; // 约2.51厘米

                // 使用厘米转EMU：1厘米 = 360000 EMU
                int widthEMU = (int)(widthCm * 360000);
                int heightEMU = (int)(heightCm * 360000);

                System.out.println("Logo图片目标尺寸: " + widthEMU + "x" + heightEMU + " EMU");
                System.out.println("Logo图片尺寸(厘米): " + widthCm + "x" + heightCm);

                // 使用addPicture方法插入图片
                logoRun.addPicture(
                        fis,
                        XWPFDocument.PICTURE_TYPE_PNG,
                        "国家能源.png",
                        widthEMU,   // 宽度
                        heightEMU   // 高度
                );
                fis.close();

                // 图片后面添加一个换行
                logoRun.addBreak();
                System.out.println("Logo图片插入成功");
            } else {
                System.out.println("Logo图片不存在，路径: " + logoPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Logo图片插入失败: " + e.getMessage());
        }

        // 添加空行，让标题和logo有一定间隔
        document.createParagraph().createRun().addBreak();

        // 添加风电场名称标题
        XWPFParagraph farmTitle = document.createParagraph();
        XWPFRun farmTitleRun = farmTitle.createRun();
        farmTitleRun.setText(windFarmName);
        farmTitleRun.setBold(true);
        farmTitleRun.setFontSize(18);
        farmTitleRun.setFontFamily("宋体");
        farmTitle.setAlignment(ParagraphAlignment.CENTER);
        farmTitle.setSpacingAfter(200); // 设置段后间距

        // 添加报告类型标题
        XWPFParagraph reportTitle = document.createParagraph();
        XWPFRun reportTitleRun = reportTitle.createRun();
        reportTitleRun.setText("无人机叶片智能巡检结果分析报告");
        reportTitleRun.setBold(true);
        reportTitleRun.setFontSize(16);
        reportTitleRun.setFontFamily("宋体");
        reportTitle.setAlignment(ParagraphAlignment.CENTER);
        reportTitle.setSpacingAfter(200);

        // 添加公司名称
        XWPFParagraph companyTitle = document.createParagraph();
        XWPFRun companyTitleRun = companyTitle.createRun();
        companyTitleRun.setText("东方电子股份有限公司");
        companyTitleRun.setBold(true);
        companyTitleRun.setFontSize(14);
        companyTitleRun.setFontFamily("宋体");
        companyTitle.setAlignment(ParagraphAlignment.CENTER);
        companyTitle.setSpacingAfter(300);

        // 添加日期
        XWPFParagraph datePara = document.createParagraph();
        XWPFRun dateRun = datePara.createRun();
        SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy年MM月dd日");
        String currentDate = dateSdf.format(new Date());
        dateRun.setText(currentDate);
        dateRun.setFontSize(12);
        dateRun.setFontFamily("宋体");
        datePara.setAlignment(ParagraphAlignment.CENTER);
        datePara.setSpacingAfter(400);

        // 添加空行
        document.createParagraph().createRun().addBreak();

        // 添加基本信息表格（风机编号和巡检地点）
        XWPFTable infoTable = document.createTable(2, 2);
        infoTable.setWidth("100%");

        // 设置表格样式
        infoTable.setCellMargins(100, 100, 100, 100); // 设置单元格边距

        XWPFTableRow row1 = infoTable.getRow(0);
        XWPFTableCell cell11 = row1.getCell(0);
        cell11.setText("风机编号");
        cell11.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        setCellFont(cell11, "宋体", 11, true);
        cell11.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

        XWPFTableCell cell12 = row1.getCell(1);
        cell12.setText(fanCode);
        cell12.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        setCellFont(cell12, "宋体", 11, false);
        cell12.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

        XWPFTableRow row2 = infoTable.getRow(1);
        XWPFTableCell cell21 = row2.getCell(0);
        cell21.setText("巡检地点");
        cell21.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        setCellFont(cell21, "宋体", 11, true);
        cell21.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

        XWPFTableCell cell22 = row2.getCell(1);
        cell22.setText(windFarmName);
        cell22.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        setCellFont(cell22, "宋体", 11, false);
        cell22.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

        // 添加空行
        document.createParagraph().createRun().addBreak();
        document.createParagraph().createRun().addBreak();

        // 添加编制单位等信息（左对齐）
        XWPFParagraph unitPara = document.createParagraph();
        XWPFRun unitRun = unitPara.createRun();
        unitRun.setText("编制单位：东方电子股份有限公司");
        unitRun.setFontSize(11);
        unitRun.setFontFamily("宋体");
        unitPara.setAlignment(ParagraphAlignment.LEFT);
        unitPara.setSpacingAfter(100);

        XWPFParagraph addressPara = document.createParagraph();
        XWPFRun addressRun = addressPara.createRun();
        addressRun.setText("地址：山东省烟台市芝罘区机场路2号");
        addressRun.setFontSize(11);
        addressRun.setFontFamily("宋体");
        addressPara.setAlignment(ParagraphAlignment.LEFT);
        addressPara.setSpacingAfter(100);

        XWPFParagraph contactPara = document.createParagraph();
        XWPFRun contactRun = contactPara.createRun();
        contactRun.setText("联系方式：0535-5520188");
        contactRun.setFontSize(11);
        contactRun.setFontFamily("宋体");
        contactPara.setAlignment(ParagraphAlignment.LEFT);
        contactPara.setSpacingAfter(200);

        // ========== 第一页结束，插入分页符 ==========
        insertPageBreak(document);

        // ========== 第二页开始 ==========
        // 添加注意事项标题
        XWPFParagraph noticeTitle = document.createParagraph();
        XWPFRun noticeTitleRun = noticeTitle.createRun();
        noticeTitleRun.setText("注意事项");
        noticeTitleRun.setBold(true);
        noticeTitleRun.setFontSize(14);
        noticeTitleRun.setFontFamily("宋体");
        noticeTitle.setAlignment(ParagraphAlignment.CENTER);
        noticeTitle.setSpacingAfter(300);

        // 添加注意事项内容
        XWPFParagraph notice1 = document.createParagraph();
        XWPFRun notice1Run = notice1.createRun();
        notice1Run.setText("1．风电场无人机叶片智能巡检项目针对风电机组叶片外部进行检测，叶片内部检测不在此巡检作业范围。");
        notice1Run.setFontSize(11);
        notice1Run.setFontFamily("宋体");
        notice1.setAlignment(ParagraphAlignment.LEFT);
        notice1.setSpacingAfter(100);
        notice1.setFirstLineIndent(400); // 首行缩进

        XWPFParagraph notice2 = document.createParagraph();
        XWPFRun notice2Run = notice2.createRun();
        notice2Run.setText("2．风电场无人机叶片巡检结果分析报告根据行业标准《NB/T 10593-2021风电场无人机叶片检测技术规范》进行编制，并提供相关处理建议。风电场在参照执行时，请结合现场实际情况进行。");
        notice2Run.setFontSize(11);
        notice2Run.setFontFamily("宋体");
        notice2.setAlignment(ParagraphAlignment.LEFT);
        notice2.setSpacingAfter(100);
        notice2.setFirstLineIndent(400);

        XWPFParagraph notice3 = document.createParagraph();
        XWPFRun notice3Run = notice3.createRun();
        notice3Run.setText("3.对报告若有异议，请在收到报告之日起一个月内向本公司提出，逾期不再受理。");
        notice3Run.setFontSize(11);
        notice3Run.setFontFamily("宋体");
        notice3.setAlignment(ParagraphAlignment.LEFT);
        notice3.setSpacingAfter(100);
        notice3.setFirstLineIndent(400);

        XWPFParagraph notice4 = document.createParagraph();
        XWPFRun notice4Run = notice4.createRun();
        notice4Run.setText("4．未经东方电子股份有限公司书面许可，部分复制、摘用或篡改本报告内容，引起法律纠纷，责任自负。");
        notice4Run.setFontSize(11);
        notice4Run.setFontFamily("宋体");
        notice4.setAlignment(ParagraphAlignment.LEFT);
        notice4.setSpacingAfter(200);
        notice4.setFirstLineIndent(400);

        // ========== 第二页结束，插入分页符 ==========
        insertPageBreak(document);

        // ========== 第三页开始 ==========
        // 4. 添加报告概述
        XWPFParagraph section1Title = document.createParagraph();
        XWPFRun section1Run = section1Title.createRun();
        section1Run.setText("1 报告概述");
        section1Run.setBold(true);
        section1Run.setFontSize(16);
        section1Run.setFontFamily("宋体");
        section1Title.setAlignment(ParagraphAlignment.CENTER);
        section1Title.setSpacingAfter(300);

        // 1.1 巡检概况
        XWPFParagraph subSection11 = document.createParagraph();
        XWPFRun subSection11Run = subSection11.createRun();
        subSection11Run.setText("1.1 巡检概况");
        subSection11Run.setBold(true);
        subSection11Run.setFontSize(14);
        subSection11Run.setFontFamily("宋体");
        subSection11.setAlignment(ParagraphAlignment.LEFT);
        subSection11.setSpacingAfter(200);

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
            setCellFont(row.getCell(0), "宋体", 11, true);
            row.getCell(0).setText(overviewData[i][0]);
            row.getCell(0).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            setCellFont(row.getCell(1), "宋体", 11, false);
            row.getCell(1).setText(overviewData[i][1]);
            row.getCell(1).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
        }

        // 添加主要缺陷项目
        XWPFParagraph defectSummary = document.createParagraph();
        XWPFRun defectSummaryRun = defectSummary.createRun();
        defectSummaryRun.setText("使用无人机检测叶片表面存在的主要缺陷项目如下：" + mostCommonDefectType);
        defectSummaryRun.setFontSize(11);
        defectSummaryRun.setFontFamily("宋体");
        defectSummary.setAlignment(ParagraphAlignment.LEFT);
        defectSummary.setSpacingAfter(200);

        // 1.2 无人机巡检叶片典型缺陷类型分类
        XWPFParagraph subSection12 = document.createParagraph();
        XWPFRun subSection12Run = subSection12.createRun();
        subSection12Run.setText("1.2无人机巡检叶片典型缺陷类型分类");
        subSection12Run.setBold(true);
        subSection12Run.setFontSize(14);
        subSection12Run.setFontFamily("宋体");
        subSection12.setAlignment(ParagraphAlignment.LEFT);
        subSection12.setSpacingAfter(200);

        // 创建缺陷类型分类表格
        XWPFTable defectTypeTable = document.createTable(9, 2);
        defectTypeTable.setWidth("100%");

        // 填充表头
        XWPFTableRow headerRow = defectTypeTable.getRow(0);
        setCellFont(headerRow.getCell(0), "宋体", 11, true);
        headerRow.getCell(0).setText("缺陷类型");
        headerRow.getCell(0).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

        setCellFont(headerRow.getCell(1), "宋体", 11, true);
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
            setCellFont(row.getCell(0), "宋体", 11, false);
            row.getCell(0).setText(defectTypeData[i][0]);
            row.getCell(0).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            setCellFont(row.getCell(1), "宋体", 11, false);
            row.getCell(1).setText(defectTypeData[i][1]);
            row.getCell(1).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
        }

        // 添加空行
        document.createParagraph().createRun().addBreak();

        // 1.3 巡检内容
        XWPFParagraph subSection13 = document.createParagraph();
        XWPFRun subSection13Run = subSection13.createRun();
        subSection13Run.setText("1.3 巡检内容");
        subSection13Run.setBold(true);
        subSection13Run.setFontSize(14);
        subSection13Run.setFontFamily("宋体");
        subSection13.setAlignment(ParagraphAlignment.LEFT);
        subSection13.setSpacingAfter(200);

        // 添加第一点巡检内容
        XWPFParagraph content1 = document.createParagraph();
        XWPFRun content1Run = content1.createRun();
        content1Run.setText("1）风机不停机巡检拍摄主要内容：分别拍摄风机三个叶片的迎风面、背风面、塔筒、轮毂、防雨裙；风机停机巡检拍摄主要内容：分别拍摄风机三个叶片的前缘、后缘、塔筒、轮毂、防雨裙。");
        content1Run.setFontSize(11);
        content1Run.setFontFamily("宋体");
        content1.setAlignment(ParagraphAlignment.LEFT);
        content1.setSpacingAfter(100);
        content1.setFirstLineIndent(400);

        // 添加第二点巡检内容
        XWPFParagraph content2 = document.createParagraph();
        XWPFRun content2Run = content2.createRun();
        content2Run.setText("2）不停机时，无需对风机进行干预（停机时，需对风机上锁，保证偏航和叶轮方位角恒定），无人机自动起飞，识别风机偏航角，并飞向风机正前方识别风机状态，自适应规划针对当前风机运行状态的巡检航线，在这个过程中依次拍摄：A、B、C叶片迎风面或前缘（用户可自行设置每叶片巡检段数）、迎风面塔筒（用户可自行设置塔筒巡检段数）、A、B、C叶片背风面或后缘（用户可自行设置每叶片巡检段数）、轮毂、背风面塔筒（用户可自行设置塔筒巡检段数）。");
        content2Run.setFontSize(11);
        content2Run.setFontFamily("宋体");
        content2.setAlignment(ParagraphAlignment.LEFT);
        content2.setSpacingAfter(200);
        content2.setFirstLineIndent(400);

        // ========== 第三页结束，插入分页符 ==========
        insertPageBreak(document);

        // ========== 第四页开始 ==========
        // 5. 巡检结果汇总
        XWPFParagraph resultSummaryTitle = document.createParagraph();
        XWPFRun resultSummaryRun = resultSummaryTitle.createRun();
        resultSummaryRun.setText("2.巡检结果汇总");
        resultSummaryRun.setBold(true);
        resultSummaryRun.setFontSize(16);
        resultSummaryRun.setFontFamily("宋体");
        resultSummaryTitle.setAlignment(ParagraphAlignment.CENTER);
        resultSummaryTitle.setSpacingAfter(300);

        // 任务描述
        XWPFParagraph taskDescription = document.createParagraph();
        XWPFRun taskDescriptionRun = taskDescription.createRun();
        taskDescriptionRun.setText("任务描述：本次巡检 1 台风机，共计 " + pointCount + "个巡检点位，其中迎风面 " +
                pointCount/2 + " 个点位，背风面 " + pointCount/2 + " 个点位。 识别缺陷 " + count + " 处。");
        taskDescriptionRun.setFontSize(11);
        taskDescriptionRun.setFontFamily("宋体");
        taskDescription.setAlignment(ParagraphAlignment.LEFT);
        taskDescription.setSpacingAfter(300);

        // 6. 添加缺陷详情（按第二个文档的格式）
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
                setCellFont(cell, "宋体", 10, true);
                cell.setText(defectHeaderTitles[j]);
                // 设置表头居中
                cell.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
            }

            // 数据行
            XWPFTableRow dataRow = defectTable.getRow(1);

            // 序号
            setCellFont(dataRow.getCell(0), "宋体", 10, false);
            dataRow.getCell(0).setText(String.valueOf(i + 1));
            dataRow.getCell(0).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            // 扇叶名称
            setCellFont(dataRow.getCell(1), "宋体", 10, false);
            dataRow.getCell(1).setText(defect.getFanCode());
            dataRow.getCell(1).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            // 扇叶部位
            setCellFont(dataRow.getCell(2), "宋体", 10, false);
            dataRow.getCell(2).setText(defect.getFanPart());
            dataRow.getCell(2).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            // 采集时间
            setCellFont(dataRow.getCell(3), "宋体", 10, false);
            dataRow.getCell(3).setText(defect.getAcquisitionTime());
            dataRow.getCell(3).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            // 缺陷主要类型
            setCellFont(dataRow.getCell(4), "宋体", 10, false);
            dataRow.getCell(4).setText(defect.getDefectType());
            dataRow.getCell(4).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            // 缺陷描述
            setCellFont(dataRow.getCell(5), "宋体", 10, false);
            dataRow.getCell(5).setText(defect.getDefectDescription());
            dataRow.getCell(5).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            // 添加空行
            document.createParagraph().createRun().addBreak();

            // 添加图片（调整大小适应页面）
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
    public void genNormalPatrolTaskWordNew(String reportId, String jobId) {
        // 1. 从数据库获取巡检任务、缺陷等信息
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
        String windFarmName = "河北华电220kV尹夏储能站";


        // 获取巡检点位信息
        WaylineJobDTO waylineJobDTO = waylineJobServiceimpl.entity2Dto(waylineJobEntity);
        Map map=new HashMap();
        map.put("waylineId",waylineJobEntity.getFileId());
        Integer pointCount = uniPointMapper2.selectListCount(map);
//        Integer pointCount = waylineJobDTO.getMediaCount();

        // 2. 创建Word文档
        XWPFDocument document = new XWPFDocument();

        // 设置默认字体
        if (document.getStyles() != null) {
            XWPFStyle style = document.getStyles().getStyle("Normal");
            if (style != null) {
                style.getCTStyle().getRPr().addNewRFonts().setAscii("宋体");
                style.getCTStyle().getRPr().addNewRFonts().setEastAsia("宋体");
            }
        }


        document.createParagraph().createRun().addBreak();

        // 添加风电场名称标题
        XWPFParagraph farmTitle = document.createParagraph();
        XWPFRun farmTitleRun = farmTitle.createRun();
        farmTitleRun.setText(windFarmName);
        farmTitleRun.setBold(true);
        farmTitleRun.setFontSize(18);
        farmTitleRun.setFontFamily("宋体");
        farmTitle.setAlignment(ParagraphAlignment.CENTER);
        farmTitle.setSpacingAfter(200); // 设置段后间距

        // 添加报告类型标题
        XWPFParagraph reportTitle = document.createParagraph();
        XWPFRun reportTitleRun = reportTitle.createRun();
        reportTitleRun.setText("无人机智能巡检结果分析报告");
        reportTitleRun.setBold(true);
        reportTitleRun.setFontSize(16);
        reportTitleRun.setFontFamily("宋体");
        reportTitle.setAlignment(ParagraphAlignment.CENTER);
        reportTitle.setSpacingAfter(200);

        // 添加公司名称
        XWPFParagraph companyTitle = document.createParagraph();
        XWPFRun companyTitleRun = companyTitle.createRun();
        companyTitleRun.setText("东方电子股份有限公司");
        companyTitleRun.setBold(true);
        companyTitleRun.setFontSize(14);
        companyTitleRun.setFontFamily("宋体");
        companyTitle.setAlignment(ParagraphAlignment.CENTER);
        companyTitle.setSpacingAfter(300);

        // 添加日期
        XWPFParagraph datePara = document.createParagraph();
        XWPFRun dateRun = datePara.createRun();
        SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy年MM月dd日");
        String currentDate = dateSdf.format(new Date());
        dateRun.setText(currentDate);
        dateRun.setFontSize(12);
        dateRun.setFontFamily("宋体");
        datePara.setAlignment(ParagraphAlignment.CENTER);
        datePara.setSpacingAfter(400);

        // 添加空行
        document.createParagraph().createRun().addBreak();

        // 添加基本信息表格（风机编号和巡检地点）
        XWPFTable infoTable = document.createTable(2, 2);
        infoTable.setWidth("100%");

        // 设置表格样式
        infoTable.setCellMargins(100, 100, 100, 100); // 设置单元格边距

        XWPFTableRow row1 = infoTable.getRow(0);
        XWPFTableCell cell11 = row1.getCell(0);
        cell11.setText("变电站名称");
        cell11.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        setCellFont(cell11, "宋体", 11, true);
        cell11.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

        XWPFTableCell cell12 = row1.getCell(1);
        cell12.setText("河北华电220kV尹夏储能站");
        cell12.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        setCellFont(cell12, "宋体", 11, false);
        cell12.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

        XWPFTableRow row2 = infoTable.getRow(1);
        XWPFTableCell cell21 = row2.getCell(0);
        cell21.setText("巡检地点");
        cell21.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        setCellFont(cell21, "宋体", 11, true);
        cell21.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

        XWPFTableCell cell22 = row2.getCell(1);
        cell22.setText("储能站园区内");
        cell22.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        setCellFont(cell22, "宋体", 11, false);
        cell22.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

        // 添加空行
        document.createParagraph().createRun().addBreak();
        document.createParagraph().createRun().addBreak();

        // 添加编制单位等信息（左对齐）
        XWPFParagraph unitPara = document.createParagraph();
        XWPFRun unitRun = unitPara.createRun();
        unitRun.setText("编制单位：东方电子股份有限公司");
        unitRun.setFontSize(11);
        unitRun.setFontFamily("宋体");
        unitPara.setAlignment(ParagraphAlignment.LEFT);
        unitPara.setSpacingAfter(100);

        XWPFParagraph addressPara = document.createParagraph();
        XWPFRun addressRun = addressPara.createRun();
        addressRun.setText("地址：山东省烟台市芝罘区机场路2号");
        addressRun.setFontSize(11);
        addressRun.setFontFamily("宋体");
        addressPara.setAlignment(ParagraphAlignment.LEFT);
        addressPara.setSpacingAfter(100);

        XWPFParagraph contactPara = document.createParagraph();
        XWPFRun contactRun = contactPara.createRun();
        contactRun.setText("联系方式：0535-5520188");
        contactRun.setFontSize(11);
        contactRun.setFontFamily("宋体");
        contactPara.setAlignment(ParagraphAlignment.LEFT);
        contactPara.setSpacingAfter(200);

        // ========== 第一页结束，插入分页符 ==========
        insertPageBreak(document);

        // ========== 第二页开始 ==========
        // 添加注意事项标题
        XWPFParagraph noticeTitle = document.createParagraph();
        XWPFRun noticeTitleRun = noticeTitle.createRun();
        noticeTitleRun.setText("注意事项");
        noticeTitleRun.setBold(true);
        noticeTitleRun.setFontSize(14);
        noticeTitleRun.setFontFamily("宋体");
        noticeTitle.setAlignment(ParagraphAlignment.CENTER);
        noticeTitle.setSpacingAfter(300);

        // 添加注意事项内容
        XWPFParagraph notice1 = document.createParagraph();
        XWPFRun notice1Run = notice1.createRun();
        notice1Run.setText("1．变电站无人机智能巡检项目主要针对站内变压器、断路器、隔离开关、绝缘子串及母线导线等室外裸露带电设备的外部可见部分进行检测，设备内部解体验证、地下电缆沟道巡检等不在此常规无人机巡检作业范围内。");
        notice1Run.setFontSize(11);
        notice1Run.setFontFamily("宋体");
        notice1.setAlignment(ParagraphAlignment.LEFT);
        notice1.setSpacingAfter(100);
        notice1.setFirstLineIndent(400); // 首行缩进

        XWPFParagraph notice2 = document.createParagraph();
        XWPFRun notice2Run = notice2.createRun();
        notice2Run.setText("2．变电站无人机巡检结果分析报告主要依据国家标准《GB/T 35697-2017 架空输电线路无人机巡检系统技术规范》以及国家电网公司企业标准《Q/GDW 11399-2021 变电站无人机巡检技术导则》等相关技术要求进行编制，并提供缺陷诊断。变电站运维单位在参照报告执行时，请结合设备历史状况、现场运行环境及安全规程等实际情况综合研判。");
        notice2Run.setFontSize(11);
        notice2Run.setFontFamily("宋体");
        notice2.setAlignment(ParagraphAlignment.LEFT);
        notice2.setSpacingAfter(100);
        notice2.setFirstLineIndent(400);

        XWPFParagraph notice3 = document.createParagraph();
        XWPFRun notice3Run = notice3.createRun();
        notice3Run.setText("3.对报告若有异议，请在收到报告之日起一个月内向本公司提出，逾期不再受理。");
        notice3Run.setFontSize(11);
        notice3Run.setFontFamily("宋体");
        notice3.setAlignment(ParagraphAlignment.LEFT);
        notice3.setSpacingAfter(100);
        notice3.setFirstLineIndent(400);

        XWPFParagraph notice4 = document.createParagraph();
        XWPFRun notice4Run = notice4.createRun();
        notice4Run.setText("4．未经东方电子股份有限公司书面许可，部分复制、摘用或篡改本报告内容，引起法律纠纷，责任自负。");
        notice4Run.setFontSize(11);
        notice4Run.setFontFamily("宋体");
        notice4.setAlignment(ParagraphAlignment.LEFT);
        notice4.setSpacingAfter(200);
        notice4.setFirstLineIndent(400);

        // ========== 第二页结束，插入分页符 ==========
        insertPageBreak(document);

        // ========== 第三页开始 ==========
        // 4. 添加报告概述
        XWPFParagraph section1Title = document.createParagraph();
        XWPFRun section1Run = section1Title.createRun();
        section1Run.setText("1 报告概述");
        section1Run.setBold(true);
        section1Run.setFontSize(16);
        section1Run.setFontFamily("宋体");
        section1Title.setAlignment(ParagraphAlignment.CENTER);
        section1Title.setSpacingAfter(300);

        // 1.1 巡检概况
        XWPFParagraph subSection11 = document.createParagraph();
        XWPFRun subSection11Run = subSection11.createRun();
        subSection11Run.setText("1.1 巡检概况");
        subSection11Run.setBold(true);
        subSection11Run.setFontSize(14);
        subSection11Run.setFontFamily("宋体");
        subSection11.setAlignment(ParagraphAlignment.LEFT);
        subSection11.setSpacingAfter(200);

        // 创建巡检概况表格
        XWPFTable overviewTable = document.createTable(4, 2);
        overviewTable.setWidth("100%");

        // 填充巡检概况表格
        String[][] overviewData = {
                {"委托单位", "河北华电石家庄裕华热电有限公司"},
                {"巡检时间", formattedTime},
                {"点位数量", String.valueOf(pointCount)},
                {"变电站名称", "河北华电220kV尹夏储能站"}
        };

        for (int i = 0; i < 4; i++) {
            XWPFTableRow row = overviewTable.getRow(i);
            setCellFont(row.getCell(0), "宋体", 11, true);
            row.getCell(0).setText(overviewData[i][0]);
            row.getCell(0).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            setCellFont(row.getCell(1), "宋体", 11, false);
            row.getCell(1).setText(overviewData[i][1]);
            row.getCell(1).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
        }


        // 在表格后添加一些间距
        XWPFParagraph afterTable = document.createParagraph();
        afterTable.setSpacingAfter(400);

        // 1.2 巡检内容
        XWPFParagraph subSection13 = document.createParagraph();
        XWPFRun subSection13Run = subSection13.createRun();
        subSection13Run.setText("1.2 巡检内容");
        subSection13Run.setBold(true);
        subSection13Run.setFontSize(14);
        subSection13Run.setFontFamily("宋体");
        subSection13.setAlignment(ParagraphAlignment.LEFT);
        subSection13.setSpacingAfter(200);

        // 添加第一点巡检内容
        XWPFParagraph content1 = document.createParagraph();
        XWPFRun content1Run = content1.createRun();
        content1Run.setText("1）变电站常规巡检拍摄主要内容：拍摄站内变压器、断路器、隔离开关、绝缘子串及母线导线等主要设备的外观、连接点与绝缘子。");
        content1Run.setFontSize(11);
        content1Run.setFontFamily("宋体");
        content1.setAlignment(ParagraphAlignment.LEFT);
        content1.setSpacingAfter(100);
        content1.setFirstLineIndent(400);

        // 添加第二点巡检内容
        XWPFParagraph content2 = document.createParagraph();
        XWPFRun content2Run = content2.createRun();
        content2Run.setText("2）常规巡检时，无人机自动起飞，基于预先规划好的巡检航线进行飞行巡检。在这个过程中，按最优路径依次拍摄相关预设点位（用户可自行设置各设备或区域的巡检精细度）。");
        content2Run.setFontSize(11);
        content2Run.setFontFamily("宋体");
        content2.setAlignment(ParagraphAlignment.LEFT);
        content2.setSpacingAfter(200);
        content2.setFirstLineIndent(400);

        // 1.3 智能分析结果对照表
        /** 1设备状态识别:
         * 序号	type属性数值	  定义	        识别结果值含义
         * 1	isolator	刀闸状态	        1代表分状态，2代表合状态，3代表分位异常状态，4代表合位异常状态
         * 2	switch	  开关/压板状态	   一般情况1代表分状态，2代表合状态，0代表预留; 可以另行约定业务含义，如1代表就地状态，2代表远方状态，0代表停用状态
         * 3	meter	    仪表读数	       具体仪表值读数（返回多个值以逗号分隔）
         * 4	infrared	红外温度	       最高温度，最低温度（以逗号分隔）
         * 5	sound	     声音	       1代表正常声音，2代表异常声音
         * 6	light	  指示灯、闪烁灯	   1代表灯灭，2代表灯亮，3代表绿灯（常）亮，4代表红灯（常） 亮，5代表绿灯闪烁，6代表红灯闪烁
         * 7	qrcode	    实物ID	       非空字符串
         **/

//        // 1.3 智能分析值对照表
//        XWPFParagraph subSection12 = document.createParagraph();
//        XWPFRun subSection12Run = subSection12.createRun();
//        subSection12Run.setText("1.3 智能分析结果对照表");
//        subSection12Run.setBold(true);
//        subSection12Run.setFontSize(14);
//        subSection12Run.setFontFamily("宋体");
//        subSection12.setAlignment(ParagraphAlignment.LEFT);
//        subSection12.setSpacingAfter(200);
//
//        // 创建表格
//        XWPFTable table = document.createTable(8, 4); // 8行4列（1行表头+7行数据）
//
//        // 设置表格列宽
//        CTTblWidth width = table.getCTTbl().addNewTblPr().addNewTblW();
//        width.setType(STTblWidth.DXA);
//        width.setW(BigInteger.valueOf(8500)); // 表格总宽度
//
//        // 设置表格样式（可选）
//        table.setCellMargins(100, 100, 100, 100); // 设置单元格边距
//
//        // 填充表头
//        XWPFTableRow headerRow = table.getRow(0);
//        headerRow.getCell(0).setText("序号");
//        headerRow.getCell(1).setText("type属性数值");
//        headerRow.getCell(2).setText("定义");
//        headerRow.getCell(3).setText("智能分析值含义");
//
//        // 设置表头加粗
//        for (int i = 0; i < 4; i++) {
//            XWPFParagraph para = headerRow.getCell(i).getParagraphs().get(0);
//            XWPFRun run = para.createRun();
//            run.setBold(true);
//            run.setFontSize(11);
//            run.setFontFamily("宋体");
//        }
//
//        // 填充数据行
//        // 第1行数据
//        fillTableRow(table, 1, "1", "isolator", "刀闸状态", "1代表分状态，2代表合状态，3代表分位异常状态，4代表合位异常状态");
//        // 第2行数据
//        fillTableRow(table, 2, "2", "switch", "开关/压板状态", "一般情况1代表分状态，2代表合状态，0代表预留; 可以另行约定业务含义，如1代表就地状态，2代表远方状态，0代表停用状态");
//        // 第3行数据
//        fillTableRow(table, 3, "3", "meter", "仪表读数", "具体仪表值读数（返回多个值以逗号分隔）");
//        // 第4行数据
//        fillTableRow(table, 4, "4", "infrared", "红外温度", "最高温度，最低温度（以逗号分隔）");
//        // 第5行数据
//        fillTableRow(table, 5, "5", "sound", "声音", "1代表正常声音，2代表异常声音");
//        // 第6行数据
//        fillTableRow(table, 6, "6", "light", "指示灯、闪烁灯", "1代表灯灭，2代表灯亮，3代表绿灯（常）亮，4代表红灯（常）亮，5代表绿灯闪烁，6代表红灯闪烁");
//        // 第7行数据
//        fillTableRow(table, 7, "7", "qrcode", "实物ID", "非空字符串");

        // ========== 第三页结束，插入分页符 ==========
        insertPageBreak(document);

        // ========== 第四页开始 ==========
        // 5. 巡检结果汇总
        XWPFParagraph resultSummaryTitle = document.createParagraph();
        XWPFRun resultSummaryRun = resultSummaryTitle.createRun();
        resultSummaryRun.setText("2.巡检结果汇总");
        resultSummaryRun.setBold(true);
        resultSummaryRun.setFontSize(16);
        resultSummaryRun.setFontFamily("宋体");
        resultSummaryTitle.setAlignment(ParagraphAlignment.CENTER);
        resultSummaryTitle.setSpacingAfter(300);

        // 任务描述
//        XWPFParagraph taskDescription = document.createParagraph();
//        XWPFRun taskDescriptionRun = taskDescription.createRun();
//        taskDescriptionRun.setText("任务描述：本次巡检整个变电站，共计 " + pointCount + "个巡检点位，识别缺陷 " + "-" + " 处。");
//        taskDescriptionRun.setFontSize(11);
//        taskDescriptionRun.setFontFamily("宋体");
//        taskDescription.setAlignment(ParagraphAlignment.LEFT);
//        taskDescription.setSpacingAfter(300);

        List<RecgPointsEntity> recgPointsEntities = recgPointsEntityMapper.selectList(new LambdaQueryWrapper<RecgPointsEntity>()
                .eq(RecgPointsEntity::getTaskPatrolledId, jobId));


        // 6. 添加缺陷详情（按第二个文档的格式）
        // 缺陷详情表格表头
        String[] defectHeaderTitles = {"序号", "点位名称", "识别结果", "数据结果", "识别大类", "识别子类", "智能分析值"};

        for (int i = 0; i < recgPointsEntities.size(); i++) {
            RecgPointsEntity recgPointsEntity = recgPointsEntities.get(i);

            // 创建缺陷信息表格（2行：表头行和数据行）
            XWPFTable defectTable = document.createTable(2, 7);
            defectTable.setWidth("100%");

            // 表头行
            XWPFTableRow headerRow1 = defectTable.getRow(0);
            for (int j = 0; j < defectHeaderTitles.length; j++) {
                XWPFTableCell cell = headerRow1.getCell(j);
                setCellFont(cell, "宋体", 10, true);
                cell.setText(defectHeaderTitles[j]);
                // 设置表头居中
                cell.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
            }

            // 数据行
            XWPFTableRow dataRow = defectTable.getRow(1);

            // 序号
            setCellFont(dataRow.getCell(0), "宋体", 10, false);
            dataRow.getCell(0).setText(String.valueOf(i + 1));
            dataRow.getCell(0).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            // 点位名称
            setCellFont(dataRow.getCell(1), "宋体", 10, false);
            dataRow.getCell(1).setText(recgPointsEntity.getPointName());
            dataRow.getCell(1).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            // 识别结果
            setCellFont(dataRow.getCell(2), "宋体", 10, false);
            dataRow.getCell(2).setText(recgPointsEntity.getPointValUnit());
            dataRow.getCell(2).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            // 数据结果
            String string = recgPointsEntity.getValid().toString();
            String dataResult ="无结果";
            if (string.equals("0")) {
                dataResult ="失败";
            }else if (string.equals("1")) {
                dataResult ="正常";
            }else if (string.equals("2")) {
                dataResult ="异常";
            }
            setCellFont(dataRow.getCell(3), "宋体", 10, false);
            dataRow.getCell(3).setText(dataResult);
            dataRow.getCell(3).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            UniPoint uniPoint = uniPointMapper2.selectOne(new LambdaQueryWrapper<UniPoint>().eq(UniPoint::getPointCode, recgPointsEntity.getPointCode()));
            String pointAnalyseType = uniPoint.getPointAnalyseType();
            Integer pointAnalyseCategory = uniPoint.getPointAnalyseCategory();
            String categoryDes = "-";
            // 根据分析类型获取对应的描述
            switch (pointAnalyseCategory) {
                case 1:
                    categoryDes = "设备状态类识别";
                    break;
                case 2:
                    categoryDes = "缺陷类识别";
                    break;
                case 3:
                    categoryDes = "判别类型";
                    break;
            }

            // 识别大类
            setCellFont(dataRow.getCell(4), "宋体", 10, false);
            dataRow.getCell(4).setText(categoryDes);
            dataRow.getCell(4).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            String pointAnalyseDes = "";
            if(pointAnalyseCategory==1){
                 pointAnalyseDes = sysDictDataMapper.selectOne(new LambdaQueryWrapper<SysDictDataEntity>().
                        eq(SysDictDataEntity::getDictType, "point_analyse_type")
                        .eq(SysDictDataEntity::getDictValue, uniPoint.getPointAnalyseType())).getDictLabel();
            }else if(pointAnalyseCategory==2){
                pointAnalyseDes = sysDictDataMapper.selectOne(new LambdaQueryWrapper<SysDictDataEntity>().
                        eq(SysDictDataEntity::getDictType, "qxsb_type")
                        .eq(SysDictDataEntity::getDictValue, uniPoint.getPointAnalyseType())).getDictLabel();
            }else if(pointAnalyseCategory==3){
                pointAnalyseDes = sysDictDataMapper.selectOne(new LambdaQueryWrapper<SysDictDataEntity>().
                        eq(SysDictDataEntity::getDictType, "point_analyse_type_pb")
                        .eq(SysDictDataEntity::getDictValue, uniPoint.getPointAnalyseType())).getDictLabel();
            }else{

            }
            // 识别子类
            setCellFont(dataRow.getCell(5), "宋体", 10, false);
            dataRow.getCell(5).setText(pointAnalyseDes);
            dataRow.getCell(5).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            String pointVal = recgPointsEntity.getPointVal();
            if (pointAnalyseCategory == 1) {
                switch (pointAnalyseType) {
                    case "isolator":
                        // 刀闸状态：1代表分状态，2代表合状态，3代表分位异常状态，4代表合位异常状态
                        pointAnalyseDes = "刀闸状态";
                        switch (pointVal) {
                            case "1":
                                pointAnalyseDes += "：分状态";
                                break;
                            case "2":
                                pointAnalyseDes += "：合状态";
                                break;
                            case "3":
                                pointAnalyseDes += "：分位异常状态";
                                break;
                            case "4":
                                pointAnalyseDes += "：合位异常状态";
                                break;
                            default:
                                pointAnalyseDes += "：未知状态(" + pointVal + ")";
                                break;
                        }
                        break;

                    case "switch":
                        // 开关/压板状态：一般情况1代表分状态，2代表合状态，0代表预留
                        pointAnalyseDes = "开关/压板状态";
                        switch (pointVal) {
                            case "1":
                                pointAnalyseDes += "：分状态";
                                break;
                            case "2":
                                pointAnalyseDes += "：合状态";
                                break;
                            case "0":
                                pointAnalyseDes += "：预留";
                                break;
                            default:
                                // 或者检查是否有其他约定业务含义
                                pointAnalyseDes += "：状态(" + pointVal + ")";
                                break;
                        }
                        break;

                    case "meter":
                        // 仪表读数：具体仪表值读数（返回多个值以逗号分隔）
                        pointAnalyseDes = "仪表读数";
                        if (pointVal != null && !pointVal.trim().isEmpty()) {
                            pointAnalyseDes += "：" + pointVal;
                        } else {
                            pointAnalyseDes += "：无读数";
                        }
                        break;

                    case "infrared":
                        // 红外温度：最高温度，最低温度（以逗号分隔）
                        pointAnalyseDes = "红外温度";
                        if (pointVal != null && !pointVal.trim().isEmpty()) {
                            pointAnalyseDes += "：" + pointVal;
                        } else {
                            pointAnalyseDes += "：无温度数据";
                        }
                        break;

                    case "sound":
                        // 声音：1代表正常声音，2代表异常声音
                        pointAnalyseDes = "声音";
                        switch (pointVal) {
                            case "1":
                                pointAnalyseDes += "：正常声音";
                                break;
                            case "2":
                                pointAnalyseDes += "：异常声音";
                                break;
                            default:
                                pointAnalyseDes += "：未知状态(" + pointVal + ")";
                                break;
                        }
                        break;

                    case "light":
                        // 指示灯、闪烁灯：1代表灯灭，2代表灯亮，3代表绿灯（常）亮，4代表红灯（常）亮，5代表绿灯闪烁，6代表红灯闪烁
                        pointAnalyseDes = "指示灯、闪烁灯";
                        switch (pointVal) {
                            case "1":
                                pointAnalyseDes += "：灯灭";
                                break;
                            case "2":
                                pointAnalyseDes += "：灯亮";
                                break;
                            case "3":
                                pointAnalyseDes += "：绿灯（常）亮";
                                break;
                            case "4":
                                pointAnalyseDes += "：红灯（常）亮";
                                break;
                            case "5":
                                pointAnalyseDes += "：绿灯闪烁";
                                break;
                            case "6":
                                pointAnalyseDes += "：红灯闪烁";
                                break;
                            default:
                                pointAnalyseDes += "：未知状态(" + pointVal + ")";
                                break;
                        }
                        break;

                    case "qrcode":
                        // 实物ID：非空字符串
                        pointAnalyseDes = "实物ID";
                        if (pointVal != null && !pointVal.trim().isEmpty()) {
                            pointAnalyseDes += "：" + pointVal;
                        } else {
                            pointAnalyseDes += "：无ID";
                        }
                        break;

                    default:
                        pointAnalyseDes = "未知类型";
                        break;
                }
            }else if(pointAnalyseCategory==2){
                if(pointVal.equals("1")){
                    pointAnalyseDes = "有缺陷";
                }else if(pointVal.equals("0")){
                    pointAnalyseDes = "无缺陷";
                }
            }

            // 智能分析结果
            setCellFont(dataRow.getCell(6), "宋体", 10, false);
            dataRow.getCell(6).setText(pointAnalyseDes);
            dataRow.getCell(6).getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);

            // 添加空行
            document.createParagraph().createRun().addBreak();

            RecgFileEntity recgFileEntity = recgFileEntityMapper.selectOne(new LambdaQueryWrapper<RecgFileEntity>()
                    .eq(RecgFileEntity::getTaskPatrolledId, jobId)
                    .eq(RecgFileEntity::getPresetNo, recgPointsEntity.getPresetNo())
                    .eq(RecgFileEntity::getPicType,recgPointsEntity.getPicType()));

//            String imagePath1 = recgFileEntity.getRecgFilePath();
//            String imagePath = fileConfig.getRecfilePath()+imagePath1;
            String imagePath = recgFileEntity.getFilePath();
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

    // 辅助方法：填充表格行
    private void fillTableRow(XWPFTable table, int rowIndex, String seq, String type, String definition, String meaning) {
        XWPFTableRow row = table.getRow(rowIndex);
        row.getCell(0).setText(seq);
        row.getCell(1).setText(type);
        row.getCell(2).setText(definition);
        row.getCell(3).setText(meaning);

        // 设置单元格字体
        for (int i = 0; i < 4; i++) {
            XWPFParagraph para = row.getCell(i).getParagraphs().get(0);
            XWPFRun run = para.createRun();
            run.setFontSize(11);
            run.setFontFamily("宋体");
        }
    }

    // 辅助方法：插入分页符
    private void insertPageBreak(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.addBreak(BreakType.PAGE);
    }

    // 辅助方法：设置单元格字体
    private void setCellFont(XWPFTableCell cell, String fontFamily, int fontSize, boolean bold) {
        for (XWPFParagraph paragraph : cell.getParagraphs()) {
            for (XWPFRun run : paragraph.getRuns()) {
                run.setFontFamily(fontFamily);
                run.setFontSize(fontSize);
                run.setBold(bold);
            }
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
