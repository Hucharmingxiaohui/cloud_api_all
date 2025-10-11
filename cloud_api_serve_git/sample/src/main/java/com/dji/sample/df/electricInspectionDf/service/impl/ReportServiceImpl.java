package com.dji.sample.df.electricInspectionDf.service.impl;


import cn.afterturn.easypoi.entity.ImageEntity;
import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.data.HyperlinkTextRenderData;
import com.deepoove.poi.data.Pictures;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.df.framework.config.FileConfig;
import com.df.framework.exception.FastException;
import com.df.framework.utils.DateUtils;
import com.df.framework.utils.ZipUtils;
import com.df.framework.utils.file.FileUtils;
import com.df.server.dto.HisUniTask.HisUniTaskParamsDTO;
import com.df.server.dto.HisUniTask.TaskReportDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisUniTaskItemFileReport;
import com.df.server.entity.his.HisUniTaskReportEntity;
import com.df.server.entity.uni.PubSubstationEntity;
import com.df.server.entity.uni.UniRobotEntity;
import com.df.server.mapper.his.HisUniTaskItemPointsMapper;
import com.df.server.mapper.his.HisUniTaskReportMapper;
import com.df.server.mapper.uni.PubSubstationMapper;
import com.df.server.service.uni.UniRobotService;
import com.df.server.utils.DictUtils;
import com.df.server.vo.his.TaskReportNewVO;
import com.dji.sample.df.electricInspectionDf.service.ReportService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    //巡视报告生成目录
    public static final String reportRelativeDir = "report";
    @Autowired
    private IWaylineJobMapper waylineJobMapper;
    @Autowired
    private PubSubstationMapper substationMapper;
    @Autowired
    private HisUniTaskItemPointsMapper hisUniTaskItemPointsMapper;
    @Autowired
    private FileConfig fileConfig;
    @Autowired
    private UniRobotService uniRobotService;
    @Autowired
    private HisUniTaskReportMapper hisUniTaskReportMapper;


    @Override
    public TaskReportDTO lookReport(HisUniTaskParamsDTO params) {
        String taskPatrolledId = params.getTaskPatrolledId();
        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                .eq(WaylineJobEntity::getJobId, taskPatrolledId));
        PubSubstationEntity oneStation = substationMapper.getOneStation();
        TaskReportDTO taskReportDTO = new TaskReportDTO();
        taskReportDTO.setSubName(oneStation.getSubName());
        taskReportDTO.setLevel(oneStation.getVlevel() + "");
//      日期待验证
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd");
        String planBeginTime = simpleDateFormat.format(new Date(waylineJobEntity.getBeginTime()));
        String beginTime = simpleDateFormat.format(new Date(waylineJobEntity.getExecuteTime()));
        String endTime = simpleDateFormat.format(new Date(waylineJobEntity.getCompletedTime()));
        taskReportDTO.setDay(planBeginTime);
        taskReportDTO.setSubType(oneStation.getSubType());
        taskReportDTO.setPlanName(waylineJobEntity.getName());
//      无人机没有环境信息
//        taskReportDTO.setEnvInfo(getEnvInfo(taskPatrolledId));
//      todo 确认功能待做
//      taskReportDTO.setConfirmTime(DateUtils.parseDateToStr(waylineJobEntity.getConfirmDate()));
        taskReportDTO.setConfirmUser("");
        taskReportDTO.setStartTime(beginTime);
        taskReportDTO.setEndTime(endTime);

        List<TaskReportDTO.PointDTO> abnormalPoints = new ArrayList<>();
        List<TaskReportDTO.PointDTO> unconfirmedPoints = new ArrayList<>();
        List<TaskReportDTO.PointDTO> normalPoints = new ArrayList<>();

        List<HisUniTaskItemFileReport> pointAndFiles = hisUniTaskItemPointsMapper.selectPointAndFile(taskPatrolledId);
        for (HisUniTaskItemFileReport pointAndFile : pointAndFiles) {
            TaskReportDTO.PointDTO pointDTO = createPointDTO(pointAndFile);
            if (pointAndFile.getConfirmTime() == null) {
                unconfirmedPoints.add(pointDTO);
            } else if (pointAndFile.getValid() != null && pointAndFile.getValid().intValue() == 1) {
                normalPoints.add(pointDTO);
            } else {
                abnormalPoints.add(pointDTO);
            }
        }
        taskReportDTO.setAbnormalPoints(abnormalPoints);
        taskReportDTO.setUnconfirmPoints(unconfirmedPoints);
        taskReportDTO.setNormalPoints(normalPoints);
        int detectedPoints = hisUniTaskItemPointsMapper.countFinishNum(taskPatrolledId);
        String des = String.format("总点位%d个，已检点位%s个，未检点位%d个，正常点位%d个，异常点位：%d个, 待人工确认点位%d个。"
                , waylineJobEntity.getAllPointCnt(), detectedPoints, waylineJobEntity.getAllPointCnt() - detectedPoints
                , normalPoints.size(), abnormalPoints.size(), unconfirmedPoints.size()
        );
        taskReportDTO.setDescription(des);
        if (abnormalPoints.size() > 0) {
            String exceptionResult = String.format("存在 %d 个异常点位，注意定期观察", abnormalPoints.size());
            taskReportDTO.setResult(exceptionResult);
        } else if (unconfirmedPoints.size() > 0) {
            String exceptionResult = String.format("待人工确认点位 %d 个，请尽快确认", unconfirmedPoints.size());
            taskReportDTO.setResult(exceptionResult);
        } else {
            taskReportDTO.setResult("未发现异常点位");
        }
        taskReportDTO.setReport_confirm_status("");
        return taskReportDTO;
    }

    @Override
    public void genPatrolTaskWordNew(Integer reportId, Integer imageNeed, String fileType) {
        HisUniTaskReportEntity reportEntity = hisUniTaskReportMapper.getById(reportId);
        String taskPatrolledId = reportEntity.getTaskPatrolledId();
        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                .eq(WaylineJobEntity::getJobId, taskPatrolledId));
//      名字用jobid
        String filename = String.format("巡视任务报告_%s_%s", waylineJobEntity.getJobId(), DateUtils.getNowDateTimeStrSimple());
        String zipFolder = String.format("%s/%s/%s", fileConfig.getFileSavePath(), reportRelativeDir, filename);
        String zipFullName = zipFolder + ".zip";
        String zipImageFolder = String.format("%s/%s", zipFolder, "images");

        File image = new File(zipImageFolder);
        boolean mkdirs1 = image.mkdirs();
        if (!mkdirs1) {
            return;
        }

        TaskReportNewVO taskReportNewDTO = null;
        try {
            taskReportNewDTO = genTaskReportContentNew(waylineJobEntity, zipImageFolder, imageNeed, fileType);
        } catch (Exception e) {
            FileUtils.deleteFolders(zipFolder);
            throw e;
        }

        if (taskReportNewDTO == null) {
            throw new FastException(
                    String.format("【生成巡视报告切面】报告数据获取失败，不继续生成巡视报告，巡视任务ID：%s", taskPatrolledId)
            );
        }

        // 文件类型如果为1，生成word版本的巡视报告
        if ("1".equals(fileType)) {
            createWordFile(filename, zipFolder, taskReportNewDTO);
        }
        // 文件类型如果为2，生成excel版本的巡视报告
        if ("2".equals(fileType)) {
            createExcelFile(filename, zipFolder, taskReportNewDTO);
        }

        //把zipFolder 打压缩包 zipFullName
        try (FileOutputStream fos = new FileOutputStream(zipFullName)) {
            ZipUtils.toZip(zipFolder, fos, true);
            FileUtils.deleteFolders(zipFolder);
        } catch (Exception e) {
            throw new FastException(
                    String.format("【生成巡视报告切面】生成压缩包失败，不继续生成巡视报告，巡视任务ID：%s", e.getMessage())
            );
        }

        //3. upsert df_his_uni_task_report
        reportEntity.setWordFile(String.format("%s/%s.zip", reportRelativeDir, filename));
        hisUniTaskReportMapper.updateFile(reportEntity);
    }

    private void createWordFile(String filename, String zipFolder, TaskReportNewVO taskReportNewDTO) {
        List<TaskReportNewVO> taskReportNewVOList = splitNormalTaskReport(taskReportNewDTO);
        for (int i = 1; i <= taskReportNewVOList.size(); i++) {
            String fileName = String.format("%s/%s.doc", zipFolder, filename + "报告" + i);
            String ftlFileName;
            // 插件列表,可以去官网查看，有列循环，还有行循环，这里是行循环实例
            LoopRowTableRenderPolicy policy = new LoopRowTableRenderPolicy();
            Configure config;
            if (i == 1) {
                ftlFileName = fileConfig.getTaskReportTempFile();
                //这里可以指定一个config类，用来指定一些规则，也可以改变模板中{{}}的这种格式
                config = Configure.builder()
                        .bind("abnormalPoints", policy)
                        .bind("unconfirmPoints", policy)
                        .bind("normalPoints", policy)
                        .build();
            } else {
                ftlFileName = fileConfig.getTaskReportMoreTempFile();
                //这里可以指定一个config类，用来指定一些规则，也可以改变模板中{{}}的这种格式
                config = Configure.builder()
                        .bind("normalPoints", policy)
                        .build();
            }

            XWPFTemplate compile = null;
            try {
                compile = XWPFTemplate.compile(ftlFileName, config);
                compile.render(taskReportNewVOList.get(i - 1));
                compile.writeToFile(fileName);
                compile.close();
            } catch (Exception e) {
                e.printStackTrace();
                throw new FastException(
                        String.format("【生成巡视报告切面】生成word文件失败，不继续生成巡视报告，巡视任务ID：%s", e.getMessage())
                );
            } finally {
                if (compile != null) {
                    try {
                        compile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private void createExcelFile(String filename, String zipFolder, TaskReportNewVO taskReportNewDTO) {
        List<TaskReportNewVO> taskReportNewVOList = this.splitNormalTaskReport(taskReportNewDTO);
        for (int i = 1; i <= taskReportNewVOList.size(); i++) {
            String fileName = String.format("%s/%s.xls", zipFolder, filename + "报告" + i);
            Map<String, Object> map = new HashMap<>();
            Workbook sheets;
            if (i == 1) {
                TemplateExportParams templateExportParams = new TemplateExportParams(
                        fileConfig.getTaskReportExcelTempFile());
                map.put("data", taskReportNewDTO);
                map.put("unConfirmPoints", taskReportNewDTO.getUnconfirmPoints());
                map.put("abnormalPoints", taskReportNewDTO.getAbnormalPoints());
                map.put("normalPoints", taskReportNewDTO.getNormalPoints());
                sheets = ExcelExportUtil.exportExcel(templateExportParams, map);
            } else {
                TemplateExportParams templateExportParams = new TemplateExportParams(
                        fileConfig.getTaskReportExcelMoreTempFile());
                map.put("normalPoints", taskReportNewVOList.get(i - 1).getNormalPoints());
                sheets = ExcelExportUtil.exportExcel(templateExportParams, map);
            }
            FileOutputStream fileFos = null;
            try {
                fileFos = new FileOutputStream(fileName, false);
                sheets.write(fileFos);
            } catch (Exception e) {
                throw new FastException(
                        String.format("【生成巡视报告切面】生成excel文件失败，不继续生成巡视报告，巡视任务ID：%s", e.getMessage())
                );
            } finally {
                try {
                    if (Objects.nonNull(fileFos)) {
                        fileFos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<TaskReportNewVO> splitNormalTaskReport(TaskReportNewVO taskReportNewDTO) {
        List<TaskReportNewVO> taskReportNewVOS = new ArrayList<>();

        List<TaskReportNewVO.PointDTO> normalPoints = taskReportNewDTO.getNormalPoints();
        int reportExportMaxDataSize = 3000;
        int taskReportNewVOSize = Math.min(normalPoints.size(), reportExportMaxDataSize);

        //现场测试发现有可能存在正常点数为0的情况，需要做特殊处理
        if (normalPoints.size() == 0) {
            taskReportNewVOS.add(taskReportNewDTO);
        } else {
            for (int i = 0; i < normalPoints.size(); i += taskReportNewVOSize) {
                if (i == 0) {
                    //第一份添加报告头部、异常点位和失败点位等信息
                    taskReportNewDTO.setNormalPoints(normalPoints.subList(i, Math.min(i + taskReportNewVOSize, normalPoints.size())));
                    taskReportNewVOS.add(taskReportNewDTO);
                } else {
                    //数据中正常点位树添加
                    TaskReportNewVO taskReportNewVO = new TaskReportNewVO();
                    taskReportNewVO.setNormalPoints(normalPoints.subList(i, Math.min(i + taskReportNewVOSize, normalPoints.size())));
                    taskReportNewVOS.add(taskReportNewVO);
                }

            }
        }

        return taskReportNewVOS;
    }

    private TaskReportNewVO genTaskReportContentNew(WaylineJobEntity waylineJobEntity, String zipImageFolder, Integer imageNeed, String fileType) {
        String taskPatrolledId = waylineJobEntity.getJobId();
        PubSubstationEntity oneStation = substationMapper.getOneStation();
        TaskReportNewVO taskReportDTO = new TaskReportNewVO();
        taskReportDTO.setSubName(oneStation.getSubName());
        taskReportDTO.setLevel(oneStation.getVlevel() + "");
//      日期待验证
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd");
        String planBeginTime = simpleDateFormat.format(new Date(waylineJobEntity.getBeginTime()));
        String beginTime = simpleDateFormat.format(new Date(waylineJobEntity.getExecuteTime()));
        String endTime = simpleDateFormat.format(new Date(waylineJobEntity.getCompletedTime()));
        taskReportDTO.setDay(planBeginTime);
        taskReportDTO.setSubType(oneStation.getSubType());
        taskReportDTO.setPlanName(waylineJobEntity.getName());
//      无人机没有环境信息
//        taskReportDTO.setEnvInfo(getEnvInfo(taskPatrolledId));
//      todo 确认功能待做
//      taskReportDTO.setConfirmTime(DateUtils.parseDateToStr(waylineJobEntity.getConfirmDate()));
        taskReportDTO.setConfirmUser("");
        taskReportDTO.setStartTime(beginTime);
        taskReportDTO.setEndTime(endTime);

        List<TaskReportNewVO.PointDTO> abnormalPoints = new ArrayList<>();
        List<TaskReportNewVO.PointDTO> unconfirmedPoints = new ArrayList<>();
        List<TaskReportNewVO.PointDTO> normalPoints = new ArrayList<>();

        List<HisUniTaskItemFileReport> pointAndFiles = hisUniTaskItemPointsMapper.selectPointAndFile(taskPatrolledId);
        int unconfirmedPointsNo = 1;
        int normalPointsNo = 1;
        int abnormalPointsNo = 1;

        for (HisUniTaskItemFileReport pointAndFile : pointAndFiles) {
            TaskReportNewVO.PointDTO ret = new TaskReportNewVO.PointDTO();
            ret.setAreaName(pointAndFile.getAreaName());
            ret.setBayName(pointAndFile.getBayName());
            ret.setDeviceName(pointAndFile.getDeviceName());
            ret.setComponentName(pointAndFile.getComponentName());
            ret.setPointName(pointAndFile.getPointName());
            ret.setLevel(DictUtils.getDictLabel("patrol_point_level", pointAndFile.getLevel()));

            ret.setSource("无人机");
            ret.setTime(DateUtils.parseDateToStr(pointAndFile.getRunTime()));
            ret.setResult(pointAndFile.getPointValUnit());
            ret.setStatus(getStatusStr(pointAndFile));
            ret.setSetVal(pointAndFile.getSetVal());
            createPictureRenderData(pointAndFile, ret, zipImageFolder, imageNeed, fileType);

            if (pointAndFile.getConfirmTime() == null) {
                ret.setNo(unconfirmedPointsNo);
                unconfirmedPoints.add(ret);
                unconfirmedPointsNo++;
            } else if (pointAndFile.getValid() != null && pointAndFile.getValid() == 1) {
                ret.setNo(normalPointsNo);
                normalPoints.add(ret);
                normalPointsNo++;
            } else {
                ret.setNo(abnormalPointsNo);
                abnormalPoints.add(ret);
                abnormalPointsNo++;
            }
        }

        taskReportDTO.setAbnormalPoints(abnormalPoints);
        taskReportDTO.setUnconfirmPoints(unconfirmedPoints);
        taskReportDTO.setNormalPoints(normalPoints);
        int detectedPoints = hisUniTaskItemPointsMapper.countFinishNum(taskPatrolledId);
        String des = String.format("总点位%d个，已检点位%s个，未检点位%d个，正常点位%d个，异常点位：%d个, 待人工确认点位%d个。"
                , waylineJobEntity.getAllPointCnt(), detectedPoints, waylineJobEntity.getAllPointCnt() - detectedPoints
                , normalPoints.size(), abnormalPoints.size(), unconfirmedPoints.size()
        );
        taskReportDTO.setDescription(des);
        if (abnormalPoints.size() > 0) {
            String exceptionResult = String.format("存在 %d 个异常点位，注意定期观察", abnormalPoints.size());
            taskReportDTO.setResult(exceptionResult);
        } else if (unconfirmedPoints.size() > 0) {
            String exceptionResult = String.format("待人工确认点位 %d 个，请尽快确认", unconfirmedPoints.size());
            taskReportDTO.setResult(exceptionResult);
        } else {
            taskReportDTO.setResult("未发现异常点位");
        }
        return taskReportDTO;
    }

    private String getEnvInfo(String taskPatrolledId) {
        UniRobotEntity uniRobot = uniRobotService.lambdaQuery().one();
        if (uniRobot == null) {
            return "";
        }
        return String.format("温度%s，湿度%s，风速%s，雨量%s，风向%s，气压%s",
                uniRobot.getEnvTemp(), uniRobot.getEnvHumi(), uniRobot.getEnvWindSpeed(),
                uniRobot.getEnvRain(), uniRobot.getEnvWindDirection(), uniRobot.getEnvPressure());
    }

    private TaskReportDTO.PointDTO createPointDTO(HisUniTaskItemFileReport pointAndFile) {
        TaskReportDTO.PointDTO ret = new TaskReportDTO.PointDTO();

        ret.setAreaName(pointAndFile.getAreaName());
        ret.setBayName(pointAndFile.getBayName());
        ret.setDeviceName(pointAndFile.getDeviceName());
        ret.setComponentName(pointAndFile.getComponentName());
        ret.setPointName(pointAndFile.getPointName());
        //巡视点位结果
        TaskReportDTO.PointResultDTO resultDTO = createPointResultDTO(pointAndFile, false);
        ret.addResult(resultDTO);
        return ret;
    }

    private TaskReportDTO.PointResultDTO createPointResultDTO(HisUniTaskItemFileReport pointAndFile, boolean needBase64) {
        TaskReportDTO.PointResultDTO resultDTO = new TaskReportDTO.PointResultDTO();
        resultDTO.setSource("无人机");
        resultDTO.setTime(DateUtils.parseDateToStr(pointAndFile.getRunTime()));
        resultDTO.setResult(pointAndFile.getPointValUnit());
        resultDTO.setStatus(getStatusStr(pointAndFile));
        resultDTO.addPic(createPicDTO(pointAndFile, needBase64));
        return resultDTO;
    }

    private String getStatusStr(HisUniTaskItemFileReport pointAndFile) {
        Integer valid = pointAndFile.getValid();
        if (pointAndFile.getConfirmTime() == null) {
            return "待人工确认";
        } else if (valid != null && valid == 1) {
            return "正常";
        } else {
            return "异常";
        }
    }

    private TaskReportDTO.PicDTO createPicDTO(HisUniTaskItemFileReport pointAndFile, boolean needBase64) {
        //优先使用识别后图片
        String filePath = StringUtils.isEmpty(pointAndFile.getRecgFilePath()) ?
                pointAndFile.getFilePath() : pointAndFile.getRecgFilePath();
        String picUrl = fileConfig.getFileVisitUrl(filePath);
        return new TaskReportDTO.PicDTO("", "", picUrl);
    }

    private void createPictureRenderData(HisUniTaskItemFileReport pointAndFile, TaskReportNewVO.PointDTO ret, String zipImageFolder, Integer imageNeed, String fileType) {
        //默认图片
        String defaultImage = "ftl" + File.separator + "default.png";

        String recg_file_path = checkFilePath(pointAndFile.getRecgFilePath());
        String file_path = checkFilePath(pointAndFile.getFilePath());
        String recg_file_path_press = checkFilePath(pointAndFile.getRecgFilePathPress());
        String file_path_other = checkFilePath(pointAndFile.getFilePathOther());
        String file_path_other_press = checkFilePath(pointAndFile.getFilePathOtherPress());

        //红外图谱
        boolean isRedPic = false;
        //声音文件户或者视频
        boolean isVoiceOrVideo = false;
        if (pointAndFile.getFileType() != null) {
            if (pointAndFile.getFileType().equals("1")) {
                isRedPic = true;
            }
            if (pointAndFile.getFileType().equals("3") || pointAndFile.getFileType().equals("4")) {
                isVoiceOrVideo = true;
            }
        }

        /*1、取word的图片，将图片放入word模板*/
        String wordImageSource = StringUtils.isBlank(recg_file_path_press) ? recg_file_path : recg_file_path_press;
        wordImageSource = StringUtils.isBlank(wordImageSource) ? file_path : wordImageSource;
        wordImageSource = StringUtils.isBlank(wordImageSource) ? defaultImage : wordImageSource;
        //没有分析图片且文件类型不为空
        if (StringUtils.isBlank(recg_file_path) && isVoiceOrVideo) {
            //如果是声音或者红外,用默认图片
            wordImageSource = defaultImage;
        }
        //TODO 20240514 PSC 图片路径修改后，取出图片
//        String wordImageSource = String.format("%s/%s", fileConfig.getFileSavePath(), wordImageSource);
        wordImageSource = processFiles(wordImageSource, fileConfig);
        try {
            if (isImage(wordImageSource)) {
                if ("1".equals(fileType)) {
                    //将文件放入word
                    ret.setImage(Pictures.ofLocal(wordImageSource).size(95, 53).create());
                }
                if ("2".equals(fileType)) {
                    //excel中的图片使用原始图
                    ret.setExcelPicUrl(this.imgFormatting(wordImageSource, 95, 53));
                  /*  String excelImageSource = StringUtils.isBlank(recg_file_path) ? file_path : recg_file_path;
                    excelImageSource = StringUtils.isBlank(excelImageSource) ? defaultImage : excelImageSource;
                    excelImageSource = processFiles(excelImageSource, fileConfig);
                    ret.setExcelPicUrl(this.imgFormatting(excelImageSource, 95, 53));*/
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if ("1".equals(fileType)) {
                //将文件放入word
                ret.setImage(Pictures.ofLocal(defaultImage).size(95, 53).create());
            }
            if ("2".equals(fileType)) {
                ret.setExcelPicUrl(this.imgFormatting(defaultImage, 95, 53));
            }
            log.error("【生成巡视报告将图片放入word模板异常，" +
                    "改用默认图片】，错误：{}，文件地址，{}", e.getMessage(), wordImageSource);
        }

        /*2、取images文件夹的图片*/
        String relativePathPic = StringUtils.isBlank(recg_file_path) ? file_path : recg_file_path;
        relativePathPic = StringUtils.isBlank(relativePathPic) ? defaultImage : relativePathPic;
        //没有分析图片
        if (StringUtils.isBlank(recg_file_path) && isVoiceOrVideo) {
            relativePathPic = defaultImage;
        }
        //最后准备复制的图片最终路径
        //Todo 保留原先 2024/05/14 psc,多硬盘检查文件
//        String sourceImageFullPath = String.format("%s/%s", fileConfig.getFileSavePath(), relativePathPic);
        String sourceImageFullPath = processFiles(relativePathPic, fileConfig);

//        // todo 如果要导出的文件类型是excel，额外增加excel图片的地址解析
//        if ("2".equals(fileType)) {
//            ret.setExcelPicUrl(this.imgFormatting(sourceImageFullPath, 95, 53));
//        }

        /*3、将images图片复制准备压缩*/
        //取出文件名字
        String separator = File.separator;
        String picName = relativePathPic.substring(relativePathPic.lastIndexOf(separator));
        if (!picName.startsWith(separator)) {
            picName = "/" + picName;
        } else {
            picName = picName.replace(separator, "/");
        }

        if (imageNeed == null || imageNeed == 1) {
            ret.setPicUrl(new HyperlinkTextRenderData("images" + picName, "images" + picName));
            String targetImageFullPath = zipImageFolder + picName;
            //零拷贝
            try (FileInputStream is = new FileInputStream(sourceImageFullPath);
                 FileChannel in = is.getChannel();
                 FileOutputStream os = new FileOutputStream(targetImageFullPath);
                 FileChannel out = os.getChannel()) {
                in.transferTo(0, in.size(), out);
            } catch (Exception e) {
                log.error("【生成巡视报告复制文件异常】，错误：{}，文件地址，{}", e.getMessage(), sourceImageFullPath);
            }
        }
        if (imageNeed == 2 && (pointAndFile.getValid() == 2)) {
            ret.setPicUrl(new HyperlinkTextRenderData("images" + picName, "images" + picName));
            String targetImageFullPath = zipImageFolder + picName;
            //零拷贝
            try (FileInputStream is = new FileInputStream(sourceImageFullPath);
                 FileChannel in = is.getChannel();
                 FileOutputStream os = new FileOutputStream(targetImageFullPath);
                 FileChannel out = os.getChannel()) {
                in.transferTo(0, in.size(), out);
            } catch (Exception e) {
                log.error("【生成巡视报告复制文件异常】，错误：{}，文件地址，{}", e.getMessage(), sourceImageFullPath);
            }
        }


        if (isRedPic) {
            String wordImageOtherSource = StringUtils.isBlank(file_path_other_press) ? file_path_other : file_path_other_press;
            wordImageOtherSource = StringUtils.isBlank(wordImageOtherSource) ? defaultImage : wordImageOtherSource;
            //todo psc 20240514 多硬盘检查文件
//            wordImageOtherSource = String.format("%s/%s", fileConfig.getFileSavePath(), wordImageOtherSource);
            wordImageOtherSource = processFiles(wordImageOtherSource, fileConfig);
            try {
                if (isImage(wordImageOtherSource)) {
                    if ("1".equals(fileType)) {
                        //将文件放入word
                        ret.setImageOtherPress(Pictures.ofLocal(wordImageOtherSource).size(95, 53).create());
                    }
                    if ("2".equals(fileType)) {
                        String excelImageOtherSource = StringUtils.isBlank(file_path_other) ? defaultImage : file_path_other;
                        ret.setExcelPicUrl(this.imgFormatting(excelImageOtherSource, 95, 53));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if ("1".equals(fileType)) {
                    //将文件放入word
                    ret.setImageOtherPress(Pictures.ofLocal(defaultImage).size(95, 53).create());
                }
                if ("2".equals(fileType)) {
                    ret.setExcelPicUrl(this.imgFormatting(defaultImage, 95, 53));
                }
                log.error("【生成巡视报告将图片放入word模板异常，" +
                        "改用默认图片】，错误：{}，文件地址，{}", e.getMessage(), wordImageOtherSource);
            }

            String picOtherPress = StringUtils.isBlank(file_path_other) ? defaultImage : file_path_other;
            //最后准备复制的图片最终路径
            //todo psc 20240514 多硬盘检查文件
//            String picOtherPressFullPath = String.format("%s/%s", fileConfig.getFileSavePath(), picOtherPress);
            String picOtherPressFullPath = processFiles(picOtherPress, fileConfig);
            /*3、将images图片复制准备压缩*/
            //取出文件名字
            String picOtherName = picOtherPress.substring(picOtherPress.lastIndexOf(File.separator));
            if (!picOtherName.startsWith(File.separator)) {
                picOtherName = File.separator + picOtherName;
            }
            ret.setPicOtherPress(new HyperlinkTextRenderData("images" + picOtherName, "images" + picOtherName));
            if (imageNeed != 3) {
                String targetImageFullPath = zipImageFolder + picOtherName;
                //零拷贝
                try (FileInputStream is = new FileInputStream(picOtherPressFullPath);
                     FileChannel in = is.getChannel();
                     FileOutputStream os = new FileOutputStream(targetImageFullPath);
                     FileChannel out = os.getChannel()) {
                    in.transferTo(0, in.size(), out);
                } catch (Exception e) {
                    log.error("【生成巡视报告复制文件异常】，错误：{}，文件地址，{}", e.getMessage(), picOtherPressFullPath);
                }
            }
        }
    }

    private String processFiles(String originalPath, FileConfig fileConfig) {
        // 构建完整的文件路径
        String fullFilePath = String.format("%s/%s", fileConfig.getFileSavePath(), originalPath);
        //检查原先文件是否存在，如果存在将不检查备份文件
        File mainFile = new File(fullFilePath);
        if (mainFile.exists() && mainFile.isFile()) {
            return mainFile.getAbsolutePath();
        }
        // 操作备份路径
        String bakPath = "";
        String[] items = bakPath.split(",");
        // 如果备份路径中未找到文件，检查主文件路径

        // 迭代备份路径列表，并检查文件是否存在
        for (String bakItem : items) {
            String trimmedPath = bakItem.trim();
            if (!trimmedPath.isEmpty()) {
                // 构建备份文件路径
                String bakFilePath = String.format("%s/%s", trimmedPath, originalPath);
                // 检查文件是否存在
                File file = new File(bakFilePath);
                if (file.exists() && file.isFile()) {
                    return file.getAbsolutePath();
                }
            }
        }

        return fullFilePath;
    }

    private String checkFilePath(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return filePath;
        }
        if (filePath.contains(" ")) {
            log.error("【生成巡视报告文件异常】，文件地址，{}", filePath);
            return "";
        }
        if (filePath.endsWith(File.separator)) {
            log.error("【生成巡视报告文件异常】，文件地址，{}", filePath);
            return "";
        }
        return filePath;
    }

    private boolean isImage(String path) {
        boolean isImageFilePath = false;
        String filePath = path.toLowerCase();
        if (filePath.endsWith(".emf")) isImageFilePath = true;
        else if (filePath.endsWith(".wmf")) isImageFilePath = true;
        else if (filePath.endsWith(".pict")) isImageFilePath = true;
        else if (filePath.endsWith(".jpeg") || filePath.endsWith(".jpg")) isImageFilePath = true;
        else if (filePath.endsWith(".png")) isImageFilePath = true;
        else if (filePath.endsWith(".dib")) isImageFilePath = true;
        else if (filePath.endsWith(".gif")) isImageFilePath = true;
        else if (filePath.endsWith(".tiff")) isImageFilePath = true;
        else if (filePath.endsWith(".eps")) isImageFilePath = true;
        else if (filePath.endsWith(".bmp")) isImageFilePath = true;
        else if (filePath.endsWith(".wpg")) isImageFilePath = true;
        else {
            throw new IllegalArgumentException(
                    "非图片文件！: " + filePath + ". Expected emf|wmf|pict|jpeg|png|dib|gif|tiff|eps|bmp|wpg");
        }
        return isImageFilePath;
    }

    private ImageEntity imgFormatting(String imgPath, int width, int height) {
        //设置图片
        ImageEntity image = new ImageEntity(imgPath, width, height);
        //表格外添加简单图片
        image.setType(ImageEntity.URL);
        return image;
    }

}
