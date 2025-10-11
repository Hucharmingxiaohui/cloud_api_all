package com.df.server.service.his.impl;

import cn.afterturn.easypoi.entity.ImageEntity;
import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.data.HyperlinkTextRenderData;
import com.deepoove.poi.data.Pictures;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.df.framework.config.FileConfig;
import com.df.framework.exception.FastException;
import com.df.framework.thread.CustomExecutorFactory;
import com.df.framework.utils.DateUtils;
import com.df.framework.utils.PageUtils;
import com.df.framework.utils.ZipUtils;
import com.df.framework.utils.file.FileUtils;
import com.df.framework.vo.PageVO;
import com.df.server.dto.HisUniTask.HisUniTaskPageDTO;
import com.df.server.dto.HisUniTask.HisUniTaskParamsDTO;
import com.df.server.dto.HisUniTask.TaskReportDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisPointsHandleDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisUniTaskItemFileReport;
import com.df.server.entity.his.HisUniTaskEntity;
import com.df.server.entity.his.HisUniTaskReportEntity;
import com.df.server.entity.uni.PubSubstationEntity;
import com.df.server.entity.uni.UniRobotEntity;
import com.df.server.entity.uni.UniTaskPlanEntity;
import com.df.server.mapper.his.HisUniTaskItemPointsMapper;
import com.df.server.mapper.his.HisUniTaskMapper;
import com.df.server.mapper.his.HisUniTaskReportMapper;
import com.df.server.mapper.uni.PubSubstationMapper;
import com.df.server.patrol.control.RobotControlService;
import com.df.server.patrol.control.sendDto.RobotCommandAPIDTO;
import com.df.server.patrol.control.sendDto.RobotParamsConstant;
import com.df.server.service.his.HisUniTaskItemPointsService;
import com.df.server.service.his.HisUniTaskService;
import com.df.server.service.uni.UniRobotService;
import com.df.server.service.uni.UniTaskPlanItemPointService;
import com.df.server.service.uni.UniTaskPlanService;
import com.df.server.utils.DictUtils;
import com.df.server.vo.his.HisUniTaskVO;
import com.df.server.vo.his.TaskReportNewVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * @author 平善闯
 * @date 2025-04-22 13:06
 */
@Slf4j
@Service
public class HisUniTaskServiceImpl implements HisUniTaskService {
    //巡视报告生成目录
    public static final String reportRelativeDir = "report";
    @Autowired
    private HisUniTaskMapper baseMapper;
    @Autowired
    private UniTaskPlanService uniTaskPlanService;
    @Autowired
    private UniTaskPlanItemPointService uniTaskPlanItemPointService;
    @Autowired
    private HisUniTaskItemPointsService hisUniTaskItemPointsService;
    @Autowired
    private RobotControlService robotControlService;
    @Autowired
    private PubSubstationMapper substationMapper;
    @Autowired
    private UniRobotService uniRobotService;
    @Autowired
    private HisUniTaskItemPointsMapper hisUniTaskItemPointsMapper;
    @Autowired
    private FileConfig fileConfig;
    @Autowired
    private HisUniTaskReportMapper hisUniTaskReportMapper;


    @Override
    public PageVO<HisUniTaskVO> getPageList(HisUniTaskPageDTO pageDTO) {
        List<HisUniTaskVO> list = baseMapper.getPageList(pageDTO);
        Integer total = baseMapper.getPageTotal(pageDTO);
        return PageUtils.returnPageVOExtend(pageDTO, list, total);
    }

    /**
     * 任务执行
     *
     * @param uniTaskPlanEntity
     * @param immediately
     */
    @Override
    public void executePlan(UniTaskPlanEntity uniTaskPlanEntity, int immediately) {
        /*List<HisUniTaskEntity> unFinishedTaskList = baseMapper.getUnFinishedTask();
        boolean unFinishedTask = !unFinishedTaskList.isEmpty();
        if (unFinishedTask) {
            throw new FastException("当前有未完成的任务 " + unFinishedTaskList.stream().map(HisUniTaskEntity::getTaskName).collect(Collectors.toList()));
        }*/
        String taskPatrolledId = "";
        if (immediately == 1) {
            //手动执行
            taskPatrolledId = uniTaskPlanEntity.getPlanNo() + "_" + DateUtils.getNowDateTimeStrSimple();
        } else {
            //自动执行
            taskPatrolledId = genTaskPatrolledId(uniTaskPlanEntity);
        }
        //创建历史任务
        createHisTask(uniTaskPlanEntity, taskPatrolledId);
        String finalTaskPatrolledId = taskPatrolledId;
        CustomExecutorFactory.dbHandlepool.execute(() -> {
            //创建点位历史记录
            try {
                hisUniTaskItemPointsService.createHisPoint(uniTaskPlanEntity, finalTaskPatrolledId);
            } catch (Exception e) {
                log.error("创建点位历史记录 ", e);
            }
        });
    }

    /**
     * 查询待执行任务 run_state = 5 最新一个
     *
     * @return
     */
    @Override
    public HisUniTaskEntity scanWaitExecuteTask() {
        return baseMapper.scanWaitExecuteTask();
    }

    /**
     * 更新任务执行中
     *
     * @param waiteTaskPatrolledId
     */
    @Override
    public void startTask(String waiteTaskPatrolledId) {
        baseMapper.startTask(waiteTaskPatrolledId);
    }

    /**
     * 统计任务进度等信息
     *
     * @param taskPatrolledId
     */
    @Async
    @Override
    public void updateProgressAndPointNum(String taskPatrolledId) {
        synchronized (this) {
            HisUniTaskEntity hisUniTask = baseMapper.getByTaskPatrolledId(taskPatrolledId);
            Integer pointNum = hisUniTask.getAllPointCnt();//任务总点位数
            Integer warnNum = 0;
            Integer failNum = 0;
            Integer normalNum = 0;
            Integer exceptionNum = 0;
            String vTaskProgress = "0";
            Integer runstate = null;
            //告警点位数
            warnNum = hisUniTaskItemPointsService.getStatisticsPointAlarmNum(taskPatrolledId);
            //失败点位数
            failNum = hisUniTaskItemPointsService.getStatisticsPointNum(taskPatrolledId, 0);
            //正常点位数
            normalNum = hisUniTaskItemPointsService.getStatisticsPointNum(taskPatrolledId, 1);
            //异常点位数
            exceptionNum = hisUniTaskItemPointsService.getStatisticsPointNum(taskPatrolledId, 2);
            //巡视进度
            vTaskProgress = hisUniTaskItemPointsService.getStatisticsPointFinishNum(taskPatrolledId, pointNum);
            if ("100".equals(vTaskProgress)) {
                runstate = 1;
            }
            baseMapper.updateProgressAndPointNum(warnNum, failNum, normalNum, exceptionNum, vTaskProgress, runstate, hisUniTask.getId());
        }
    }

    /**
     * 更新任务暂停
     *
     * @param taskPatrolledId
     */
    @Override
    public void pauseTask(String taskPatrolledId) {
        HisUniTaskEntity hisUniTask = baseMapper.getByTaskPatrolledId(taskPatrolledId);
        if (hisUniTask == null || hisUniTask.getRunState() != 2) {
            throw new FastException("任务非执行中，不能暂停");
        }
        try {
            robotControlService.sendCustomCommand(new RobotCommandAPIDTO(RobotParamsConstant.TASK_PAUSE));
        } catch (Exception e) {
            log.error("", e);
        }
        baseMapper.pauseTask(taskPatrolledId);
        log.info("任务暂停，任务：{}", hisUniTask.getTaskName());
    }

    /**
     * 更新任务执行中
     *
     * @param taskPatrolledId
     */
    @Override
    public void recoverTask(String taskPatrolledId) {
        HisUniTaskEntity hisUniTask = baseMapper.getByTaskPatrolledId(taskPatrolledId);
        if (hisUniTask == null || hisUniTask.getRunState() != 3) {
            throw new FastException("任务非暂停中，不能恢复");
        }
        try {
            robotControlService.sendCustomCommand(new RobotCommandAPIDTO(RobotParamsConstant.TASK_RECOVER));
        } catch (Exception e) {
            log.error("", e);
        }
        baseMapper.recoverTask(taskPatrolledId);
        log.info("任务恢复，任务：{}", hisUniTask.getTaskName());
    }

    /**
     * 更新状态终止任务
     *
     * @param taskPatrolledId
     */
    @Override
    public void stopTask(String taskPatrolledId) {
        HisUniTaskEntity hisUniTask = baseMapper.getByTaskPatrolledId(taskPatrolledId);
        if (hisUniTask == null || hisUniTask.getRunState() == 1 || hisUniTask.getRunState() == 4) {
            throw new FastException("任务已经停止");
        }
        try {
            robotControlService.sendCustomCommand(new RobotCommandAPIDTO(RobotParamsConstant.TASK_STOP));
        } catch (Exception e) {
            log.error("", e);
        }
        baseMapper.stopTask(taskPatrolledId);
        log.info("任务终止，任务：{}", hisUniTask.getTaskName());
    }

    /**
     * 查询任务详情
     *
     * @param taskPatrolledId
     * @return
     */
    @Override
    public HisUniTaskVO getInfo(String taskPatrolledId) {
        HisUniTaskVO info = baseMapper.getInfo(taskPatrolledId);
        int finishNum = hisUniTaskItemPointsService.countFinishNum(taskPatrolledId);
        info.setUnFinishedPointCnt(info.getAllPointCnt() - finishNum);
        return info;
    }

    /**
     * 任务确认完毕
     *
     * @param params
     */
    @Override
    public void confirmTask(HisPointsHandleDTO params) {
        baseMapper.confirmTask(params);
    }

    /**
     * 查看任务报告
     *
     * @param params
     * @return
     */
    @Override
    public TaskReportDTO lookReport(HisUniTaskParamsDTO params) {
        String taskPatrolledId = params.getTaskPatrolledId();
        HisUniTaskEntity hisUniTask = baseMapper.getByTaskPatrolledId(taskPatrolledId);
        PubSubstationEntity oneStation = substationMapper.getOneStation();
        TaskReportDTO taskReportDTO = new TaskReportDTO();
        taskReportDTO.setSubName(oneStation.getSubName());
        taskReportDTO.setLevel(oneStation.getVlevel() + "");
        taskReportDTO.setDay(DateUtils.parseDateToStr(hisUniTask.getPlanStartTime()));
        taskReportDTO.setSubType(oneStation.getSubType());
        taskReportDTO.setPlanName(hisUniTask.getTaskName());
        taskReportDTO.setEnvInfo(getEnvInfo(taskPatrolledId));
        taskReportDTO.setConfirmTime(DateUtils.parseDateToStr(hisUniTask.getConfirmDate()));
        taskReportDTO.setConfirmUser("");
        taskReportDTO.setStartTime(DateUtils.parseDateToStr(hisUniTask.getStartTime()));
        taskReportDTO.setEndTime(DateUtils.parseDateToStr(hisUniTask.getEndTime()));

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
                , hisUniTask.getAllPointCnt(), detectedPoints, hisUniTask.getAllPointCnt() - detectedPoints
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

    /**
     * 生成巡视报告
     *
     * @param reportId
     * @param imageNeed
     * @param fileType
     */
    @Override
    public void genPatrolTaskWordNew(Integer reportId, Integer imageNeed, String fileType) {
        HisUniTaskReportEntity reportEntity = hisUniTaskReportMapper.getById(reportId);
        String taskPatrolledId = reportEntity.getTaskPatrolledId();
        HisUniTaskEntity hisUniTask = baseMapper.getByTaskPatrolledId(taskPatrolledId);
        String filename = String.format("巡视任务报告_%s_%s", hisUniTask.getTaskCode(), DateUtils.getNowDateTimeStrSimple());
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
            taskReportNewDTO = genTaskReportContentNew(hisUniTask, zipImageFolder, imageNeed, fileType);
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

    /**
     * 查询最新一个正在执行的任务
     *
     * @return
     */
    @Override
    public HisUniTaskVO getRunningTask() {
        HisUniTaskVO runningTask = baseMapper.getRunningTask();
        if (runningTask == null) {
            return null;
        }
        int finishNum = hisUniTaskItemPointsService.countFinishNum(runningTask.getTaskPatrolledId());
        runningTask.setUnFinishedPointCnt(runningTask.getAllPointCnt() - finishNum);
        return runningTask;
    }

    @Override
    public HisUniTaskVO getShowTask() {
        //先取执行中或者暂停中任务，有则仅有一个
        HisUniTaskVO showTask = baseMapper.getRunningTask();
        if (showTask == null) {
            //如果没有，获取最后一条任务
            showTask = baseMapper.getLastTask();
        }
        if (showTask == null) {
            return null;
        }
        int finishNum = hisUniTaskItemPointsService.countFinishNum(showTask.getTaskPatrolledId());
        showTask.setUnFinishedPointCnt(showTask.getAllPointCnt() - finishNum);
        return showTask;
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

    /**
     * 将数据分割成多个批次
     *
     * @param taskReportNewDTO 原始数据
     * @return 分割后的数据批次列表
     */
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

    private TaskReportNewVO genTaskReportContentNew(HisUniTaskEntity hisUniTask, String zipImageFolder, Integer imageNeed, String fileType) {
        String taskPatrolledId = hisUniTask.getTaskPatrolledId();
        PubSubstationEntity oneStation = substationMapper.getOneStation();
        TaskReportNewVO taskReportDTO = new TaskReportNewVO();
        taskReportDTO.setSubName(oneStation.getSubName());
        taskReportDTO.setLevel(oneStation.getVlevel() + "");
        taskReportDTO.setDay(DateUtils.parseDateToStr(hisUniTask.getPlanStartTime()));
        taskReportDTO.setSubType(oneStation.getSubType());
        taskReportDTO.setPlanName(hisUniTask.getTaskName());
        taskReportDTO.setEnvInfo(getEnvInfo(taskPatrolledId));
        taskReportDTO.setConfirmTime(DateUtils.parseDateToStr(hisUniTask.getConfirmDate()));
        taskReportDTO.setConfirmUser("");
        taskReportDTO.setStartTime(DateUtils.parseDateToStr(hisUniTask.getStartTime()));
        taskReportDTO.setEndTime(DateUtils.parseDateToStr(hisUniTask.getEndTime()));

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

            ret.setSource("机器人");
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
                , hisUniTask.getAllPointCnt(), detectedPoints, hisUniTask.getAllPointCnt() - detectedPoints
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

    /**
     * 检查多路径下文件
     *
     * @param originalPath 相对路径
     * @param fileConfig   配置文件fileConfig
     * @return {@link String} 文件存在路径
     */
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
        resultDTO.setSource("机器人");
        resultDTO.setTime(DateUtils.parseDateToStr(pointAndFile.getRunTime()));
        resultDTO.setResult(pointAndFile.getPointValUnit());
        resultDTO.setStatus(getStatusStr(pointAndFile));
        resultDTO.addPic(createPicDTO(pointAndFile, needBase64));
        return resultDTO;
    }

    private TaskReportDTO.PicDTO createPicDTO(HisUniTaskItemFileReport pointAndFile, boolean needBase64) {
        //优先使用识别后图片
        String filePath = StringUtils.isEmpty(pointAndFile.getRecgFilePath()) ?
                pointAndFile.getFilePath() : pointAndFile.getRecgFilePath();
        String picUrl = fileConfig.getFileVisitUrl(filePath);
        return new TaskReportDTO.PicDTO("", "", picUrl);
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

    private String getEnvInfo(String taskPatrolledId) {
        UniRobotEntity uniRobot = uniRobotService.lambdaQuery().one();
        if (uniRobot == null) {
            return "";
        }
        return String.format("温度%s，湿度%s，风速%s，雨量%s，风向%s，气压%s",
                uniRobot.getEnvTemp(), uniRobot.getEnvHumi(), uniRobot.getEnvWindSpeed(),
                uniRobot.getEnvRain(), uniRobot.getEnvWindDirection(), uniRobot.getEnvPressure());
    }

    private synchronized String genTaskPatrolledId(UniTaskPlanEntity plan) {
        Integer executeType = plan.getExecuteType();
        if (executeType == null) {
            throw new FastException("需要执行的任务的 executeType 为空");
        }
        if (executeType == 2) {
            Date fixedStartTime = plan.getFixedStartTime();
            if (fixedStartTime == null) {
                throw new FastException("需要执行的任务的 fixedStartTime 为空");
            }
            String subCode = plan.getSubCode();
            String planCode = plan.getPlanNo();
            return planCode + "_" +
                    DateUtils.parseDateToStr(fixedStartTime, DateUtils.T_DATA_TIME_SIMPLE);
        }
        if (executeType == 3) {
            if (plan.getCycleType() != null) {
                Date cycleExecuteTime = plan.getCycleExecuteTime();
                if (cycleExecuteTime == null) {
                    throw new FastException("需要执行的任务的 cycleExecuteTime 为空");
                }
                String subCode = plan.getSubCode();
                String planCode = plan.getPlanNo();
                String taskPatrolledId = planCode + "_" +
                        DateUtils.parseDateToStr(new Date(), DateUtils.T_DATA_SIMPLE) +
                        DateUtils.parseDateToStr(cycleExecuteTime, DateUtils.T_TIME_SIMPLE);
                return taskPatrolledId;
            }
            if (StringUtils.isNotBlank(plan.getIntervalType())) {
                Date intervalExecuteTime = plan.getIntervalExecuteTime();
                if (intervalExecuteTime == null) {
                    throw new FastException("需要执行的任务的 intervalExecuteTime 为空");
                }
                String subCode = plan.getSubCode();
                String planCode = plan.getPlanNo();
                String taskPatrolledId = planCode + "_" +
                        DateUtils.parseDateToStr(new Date(), DateUtils.T_DATA_SIMPLE_INTERVAL) +
                        DateUtils.parseDateToStr(intervalExecuteTime, DateUtils.T_TIME_SIMPLE_INTERVAL);
                return taskPatrolledId;
            }
        }
        throw new FastException("需要执行的任务的 周期参数 非法");
    }

    private void createHisTask(UniTaskPlanEntity uniTaskPlanEntity, String taskPatrolledId) {
        HisUniTaskEntity hisUniTaskEntity = new HisUniTaskEntity();
        hisUniTaskEntity.setSubCode(uniTaskPlanEntity.getSubCode());
        hisUniTaskEntity.setPlanNo(uniTaskPlanEntity.getPlanNo());
        hisUniTaskEntity.setTaskCode(uniTaskPlanEntity.getPlanNo());
        hisUniTaskEntity.setTaskName(uniTaskPlanEntity.getPlanName());
        hisUniTaskEntity.setTaskPatrolledId(taskPatrolledId);
        hisUniTaskEntity.setPlanType(uniTaskPlanEntity.getPlanType());
        hisUniTaskEntity.setTaskType(uniTaskPlanEntity.getTaskType());
        hisUniTaskEntity.setTaskSubType(uniTaskPlanEntity.getTaskSubType());
        String timestr = taskPatrolledId.substring(taskPatrolledId.lastIndexOf("_") + 1);
        hisUniTaskEntity.setPlanStartTime(DateUtils.parseDate(timestr, DateUtils.T_DATA_TIME_SIMPLE));
        hisUniTaskEntity.setRunState(5);
        hisUniTaskEntity.setTaskProgress("0");
        hisUniTaskEntity.setAllPointCnt(uniTaskPlanItemPointService.countPlanPointsNum(uniTaskPlanEntity.getPlanNo()));
        baseMapper.insertHisUniTask(hisUniTaskEntity);
        uniTaskPlanService.updatePlanLastExecuteTime(uniTaskPlanEntity.getPlanNo(), hisUniTaskEntity.getPlanStartTime());
        log.info("执行任务创建成功：{}", hisUniTaskEntity.getTaskName());
    }

    /**
     * 对所有文件路径做有效性判断，但不对文件做验证
     */
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

    /**
     * 图片格式化，Word导出图片格式
     *
     * @param imgPath 图片路径
     */
    private ImageEntity imgFormatting(String imgPath, int width, int height) {
        //设置图片
        ImageEntity image = new ImageEntity(imgPath, width, height);
        //表格外添加简单图片
        image.setType(ImageEntity.URL);
        return image;
    }
}
