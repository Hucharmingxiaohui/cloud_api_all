package com.df.server.service.his.impl;

import com.df.framework.config.FileConfig;
import com.df.framework.utils.PageUtils;
import com.df.framework.vo.PageVO;
import com.df.server.dto.HisUniTaskItemPoints.HisPointsHandleDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisUniTaskItemPointsPageDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisUniTaskItemPointsParamsDTO;
import com.df.server.entity.his.HisUniTaskItemFileEntity;
import com.df.server.entity.his.HisUniTaskItemPointsEntity;
import com.df.server.entity.uni.UniTaskPlanEntity;
import com.df.server.mapper.his.HisUniTaskItemPointsMapper;
import com.df.server.service.his.HisUniTaskItemAlarmService;
import com.df.server.service.his.HisUniTaskItemPointsService;
import com.df.server.service.his.HisUniTaskService;
import com.df.server.service.uni.UniPointThresholdService;
import com.df.server.service.uni.impl.UniXmlDataToDBService;
import com.df.server.vo.UniPoint.UniPointVO;
import com.df.server.vo.his.HisUniTaskItemPointsVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DecimalFormat;
import java.util.*;

/**
 * @author 平善闯
 * @date 2025-04-22 15:37
 */
@Service
@Slf4j
public class HisUniTaskItemPointsServiceImpl implements HisUniTaskItemPointsService {

    @Autowired
    private HisUniTaskItemPointsMapper baseMapper;
    @Autowired
    private UniXmlDataToDBService uniXmlDataToDBService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Value("${mybatis-plus.configuration-properties.hdb}")
    private String hdbName;
    @Autowired
    private UniPointThresholdService uniPointThresholdService;
    @Autowired
    private FileConfig fileConfig;
    @Autowired
    private HisUniTaskService hisUniTaskService;
    @Autowired
    private HisUniTaskItemAlarmService hisUniTaskItemAlarmService;

    @Override
    public PageVO<HisUniTaskItemPointsVO> getHisPoints(HisUniTaskItemPointsPageDTO hisUniPointsDTO) {
        List<HisUniTaskItemPointsVO> list = baseMapper.getPageList(hisUniPointsDTO);
        for (HisUniTaskItemPointsVO vo : list) {

            Date confirmTime = vo.getConfirmTime();
            String status = "待确认";
            if (confirmTime != null) {
                status = "已确认";
            }
            vo.setStatus(status);

            vo.setConclusion(getHisPointConclusion(vo));

            vo.setFilePathUrl(fileConfig.getFileVisitUrl(vo.getFilePath()));
            vo.setRecgFilePathUrl(fileConfig.getFileVisitUrl(vo.getRecgFilePath()));

            vo.setPointVal(StringUtils.isBlank(vo.getPointVal()) ? "-" : vo.getPointVal());
        }
        Integer total = baseMapper.getPageTotal(hisUniPointsDTO);
        return PageUtils.returnPageVOExtend(hisUniPointsDTO, list, total);
    }

    private static String getHisPointConclusion(HisUniTaskItemPointsVO vo) {
        String conclusion = vo.getRunTime() != null ? "执行中" : "待巡视";
        Integer valid = vo.getValid();
        if (valid != null) {
            switch (valid) {
                case 0:
                    conclusion = "失败";
                    break;
                case 1:
                    conclusion = "正常";
                    break;
                case 2:
                    conclusion = "异常";
                    break;
            }
        }
        if (vo.getIsAlarm() != null) {
            int is_alarm = vo.getIsAlarm();
            if (is_alarm == 1) {
                if (vo.getConclusion() != null) {
                    conclusion = vo.getConclusion();
                } else {
                    conclusion = "异常";
                }
            }
        }
        return conclusion;
    }

    @Override
    public HisUniTaskItemPointsEntity getLastItems(String subCode, String pointCode) {
        return baseMapper.getLastItems(subCode, pointCode);
    }

    @Override
    public void createHisPoint(UniTaskPlanEntity uniTaskPlanEntity, String taskPatrolledId) {
        String subCode = uniTaskPlanEntity.getSubCode();
        String planNo = uniTaskPlanEntity.getPlanNo();
        String planName = uniTaskPlanEntity.getPlanName();
        //UniPointPageDTO pageDTO = new UniPointPageDTO();
        //pageDTO.setSubCode(subCode);
        List<UniPointVO> planPoints = baseMapper.listPointsToHisTask(planNo);
        List<HisUniTaskItemPointsEntity> addList = new ArrayList<>();
        List<HisUniTaskItemFileEntity> addFileList = new ArrayList<>();
        for (UniPointVO point : planPoints) {
            HisUniTaskItemPointsEntity hisPoint = new HisUniTaskItemPointsEntity();
            hisPoint.setSubCode(subCode);
            hisPoint.setPlanNo(planNo);
            hisPoint.setRequestId(UUID.randomUUID().toString().replace("-", ""));
            hisPoint.setTaskPatrolledId(taskPatrolledId);
            hisPoint.setTaskName(planName);
            hisPoint.setTaskCode(planNo);
            hisPoint.setPointCode(point.getPointCode());
            hisPoint.setPointName(point.getPointName());
            hisPoint.setPatroldeviceCode(point.getRobotCode());
            hisPoint.setPatroldeviceName(point.getRobotName());
            hisPoint.setDataType(point.getPointType());
            hisPoint.setPresetNo(point.getRobotPos());
            hisPoint.setRecognitionType(point.getRecognitionTypeList());
            hisPoint.setIsFinished(0);
            hisPoint.setValueType(0);
            addList.add(hisPoint);

            HisUniTaskItemFileEntity file = new HisUniTaskItemFileEntity();
            file.setRequestId(hisPoint.getRequestId());
            file.setFileType(point.getSaveTypeList());
            addFileList.add(file);
        }
        saveHisPointListExecuteBatch(addList);
        log.info("执行任务 巡检结果数据 创建成功：{}", uniTaskPlanEntity.getPlanName());
        saveHisFileExecuteBatch(addFileList);
        log.info("执行任务 图片数据 创建成功：{}", uniTaskPlanEntity.getPlanName());
    }

    @Override
    public List<String> listTaskPatroldeviceCode(String waiteTaskPatrolledId) {
        return baseMapper.listTaskPatroldeviceCode(waiteTaskPatrolledId);
    }

    /**
     * 统计任务告警数量
     *
     * @param taskPatrolledId
     * @return
     */
    @Override
    public Integer getStatisticsPointAlarmNum(String taskPatrolledId) {
        return baseMapper.getStatisticsPointAlarmNum(taskPatrolledId);
    }

    /**
     * 统计任务各个结果数量
     *
     * @param taskPatrolledId
     * @param valid
     * @return
     */
    @Override
    public Integer getStatisticsPointNum(String taskPatrolledId, int valid) {
        return baseMapper.getStatisticsPointNum(taskPatrolledId, valid);
    }

    /**
     * 计算任务进度百分比
     *
     * @param taskPatrolledId
     * @param pointNum        总点位数量
     * @return
     */
    @Override
    public String getStatisticsPointFinishNum(String taskPatrolledId, Integer pointNum) {
        int finishNum = baseMapper.countFinishNum(taskPatrolledId);
        float progressFloat = (finishNum / (float) pointNum) * 100;
        DecimalFormat df = new DecimalFormat("0");//格式化小数
        return df.format(progressFloat);
    }

    /**
     * 查询点位详情
     *
     * @param paramsDTO "taskPatrolledId", "pointCode"
     * @return
     */
    @Override
    public HisUniTaskItemPointsVO getHisPointDetail(HisUniTaskItemPointsParamsDTO paramsDTO) {
        HisUniTaskItemPointsVO vo = baseMapper.getHisPointDetail(paramsDTO);

        Integer manualHand = vo.getManualHand();
        String status = "待确认";
        if (manualHand != null && manualHand != 0) {
            status = "已确认";
        }
        vo.setStatus(status);

        String conclusion = "执行中";
        Integer valid = vo.getValid();
        if (valid != null) {
            switch (valid) {
                case 0:
                    conclusion = "失败";
                    break;
                case 1:
                    conclusion = "正常";
                    break;
                case 2:
                    conclusion = "异常";
                    break;
            }
        }
        if (vo.getIsAlarm() != null) {
            int is_alarm = vo.getIsAlarm();
            if (is_alarm == 1) {
                if (vo.getConclusion() != null) {
                    conclusion = vo.getConclusion();
                } else {
                    conclusion = "异常";
                }
            }
        }
        vo.setConclusion(conclusion);
        vo.setAlarmThreshold(uniPointThresholdService.getPointThresholdDes(vo.getPointCode()));
        vo.setFilePathUrl(fileConfig.getFileVisitUrl(vo.getFilePath()));
        vo.setRecgFilePathUrl(fileConfig.getFileVisitUrl(vo.getRecgFilePath()));
        return vo;
    }

    /**
     * 人工修正
     *
     * @param params "taskPatrolledId", "pointCode","setVal"
     */
    @Override
    public void abnormalHandle(HisPointsHandleDTO params) {
        baseMapper.abnormalHandle(params);
    }

    /**
     * 结果确认
     *
     * @param params 结果确认 一键确认只传taskPatrolledId，单个结果确认 加pointCode
     */
    @Override
    public void confirm(HisPointsHandleDTO params) {
        baseMapper.confirm(params.getTaskPatrolledId(), params.getPointCode());
        int unConfirmNum = baseMapper.getUnConfirmNum(params.getTaskPatrolledId());
        if (unConfirmNum == 0) {
            hisUniTaskService.confirmTask(params);
        }
        //一键确认时，告警确认
        if (StringUtils.isBlank(params.getPointCode())) {
            hisUniTaskItemAlarmService.confirmAll(params.getTaskPatrolledId());
        }
    }

    /**
     * 告警确认"taskPatrolledId", "pointCode", "confirmResult"， "alarmConfirmComment";
     *
     * @param params
     */
    @Override
    public void confirmAlarm(HisPointsHandleDTO params) {
        hisUniTaskItemAlarmService.confirmAlarm(params);
    }

    @Override
    public int countFinishNum(String taskPatrolledId) {
        return baseMapper.countFinishNum(taskPatrolledId);
    }

    @Override
    public List<Integer> listTaskRobotPosList(String waiteTaskPatrolledId) {
        return baseMapper.listTaskRobotPosList(waiteTaskPatrolledId);
    }

    public void saveHisPointListExecuteBatch(List<HisUniTaskItemPointsEntity> addList) {
        Connection conn = DataSourceUtils.getConnection(Objects.requireNonNull(jdbcTemplate.getDataSource()));
        String sql = "insert into " + hdbName + ".df_his_uni_task_item_points " +
                "(request_id," +
                "task_patrolled_id," +
                "plan_no," +
                "point_code," +
                "point_name," +
                "task_code," +
                "task_name," +
                "sub_code," +
                "patroldevice_code," +
                "patroldevice_name," +
                "data_type," +
                "value_type," +
                "recognition_type," +
                "is_finished," +
                "preset_no)" +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int batchCount = 0;
            for (HisUniTaskItemPointsEntity entity : addList) {
                // 使用工具类简化参数设置
                uniXmlDataToDBService.setStringSafe(pstmt, 1, entity.getRequestId());
                uniXmlDataToDBService.setStringSafe(pstmt, 2, entity.getTaskPatrolledId());
                uniXmlDataToDBService.setStringSafe(pstmt, 3, entity.getPlanNo());
                uniXmlDataToDBService.setStringSafe(pstmt, 4, entity.getPointCode());
                uniXmlDataToDBService.setStringSafe(pstmt, 5, entity.getPointName());
                uniXmlDataToDBService.setStringSafe(pstmt, 6, entity.getTaskCode());
                uniXmlDataToDBService.setStringSafe(pstmt, 7, entity.getTaskName());
                uniXmlDataToDBService.setStringSafe(pstmt, 8, entity.getSubCode());
                uniXmlDataToDBService.setStringSafe(pstmt, 9, entity.getPatroldeviceCode());
                uniXmlDataToDBService.setStringSafe(pstmt, 10, entity.getPatroldeviceName());
                uniXmlDataToDBService.setSafe(pstmt, 11, entity.getDataType(), Types.INTEGER);
                uniXmlDataToDBService.setSafe(pstmt, 12, entity.getValueType(), Types.INTEGER);
                uniXmlDataToDBService.setSafe(pstmt, 13, entity.getRecognitionType(), Types.INTEGER);
                uniXmlDataToDBService.setSafe(pstmt, 14, entity.getIsFinished(), Types.INTEGER);
                uniXmlDataToDBService.setStringSafe(pstmt, 15, entity.getPresetNo());
                pstmt.addBatch();
                if (++batchCount % 1000 == 0) {
                    uniXmlDataToDBService.executeBatch(pstmt, conn, batchCount);
                }
            }
            // 处理剩余批次
            uniXmlDataToDBService.executeBatch(pstmt, conn, batchCount);
            conn.commit();
        } catch (SQLException e) {
            log.error("Database error", e);
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
        } finally {
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
        }
    }

    public void saveHisFileExecuteBatch(List<HisUniTaskItemFileEntity> addFileList) {
        Connection conn = DataSourceUtils.getConnection(Objects.requireNonNull(jdbcTemplate.getDataSource()));
        String sql = "insert into " + hdbName + ". df_his_uni_task_item_file " +
                "(request_id," +
                "file_type) " +
                "VALUES (?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int batchCount = 0;
            for (HisUniTaskItemFileEntity fileEntity : addFileList) {
                // 使用工具类简化参数设置
                uniXmlDataToDBService.setStringSafe(pstmt, 1, fileEntity.getRequestId());
                uniXmlDataToDBService.setStringSafe(pstmt, 2, fileEntity.getFileType());
                pstmt.addBatch();
                if (++batchCount % 1000 == 0) {
                    uniXmlDataToDBService.executeBatch(pstmt, conn, batchCount);
                }
            }
            // 处理剩余批次
            uniXmlDataToDBService.executeBatch(pstmt, conn, batchCount);
            conn.commit();
        } catch (SQLException e) {
            log.error("Database error", e);
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
        } finally {
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
        }
    }
}
