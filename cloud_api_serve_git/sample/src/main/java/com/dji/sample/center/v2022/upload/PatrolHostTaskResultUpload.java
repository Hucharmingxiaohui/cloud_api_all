package com.dji.sample.center.v2022.upload;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.app.AppContext;
import com.dji.sample.center.config.CenterFtpsNormalConfig;
import com.dji.sample.center.dao.ICenterToUavPlanMapper;
import com.dji.sample.center.dao.IUniRepairAreaMapper;
import com.dji.sample.center.model.entity.CenterToUavPlanEntity;
import com.dji.sample.center.model.entity.UniRepairAreaEntity;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.ftp.FtpUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.PatrolResultItem;
import com.dji.sample.center.v2022.enums.RecognitionTypeEnum;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import com.dji.sample.df.manageDf.dao.IUniTaskPlanMapper;
import com.dji.sample.df.manageDf.model.entity.UniTaskPlanEntity;
import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointCount.PointCount;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 巡视任务点位结果上送
 *
 * @author 连勇超
 * @Time 2022/4/6 21:01
 */
@Slf4j
public class PatrolHostTaskResultUpload {

    private PatrolHostTaskResultUpload() {

    }

    private static class PatrolHostTaskResultUploadHolder {
        public static final PatrolHostTaskResultUpload hoder = new PatrolHostTaskResultUpload();
    }

    public static PatrolHostTaskResultUpload getSingletonHandler() {
        return PatrolHostTaskResultUploadHolder.hoder;
    }

    private CenterFtpsNormalConfig fileConfig = AppContext.getBean(CenterFtpsNormalConfig.class);
    private PatrolHostSocketClient patrolHostSocketClient = AppContext.getBean(PatrolHostSocketClient.class);
    private IUniRepairAreaMapper iUniRepairAreaMapper = AppContext.getBean(IUniRepairAreaMapper.class);
    private ICenterToUavPlanMapper iCenterToUavPlanMapper = AppContext.getBean(ICenterToUavPlanMapper.class);
    private IUniTaskPlanMapper iUniTaskPlanMapper = AppContext.getBean(IUniTaskPlanMapper.class);

    public void uploadToCenter(List<PointCount> list, String uavTaskPatrolledId, String subCode, String deviceName, String deviceCode) {
        if (list != null && list.size() > 0) {
            List<UniRepairAreaEntity> repairs = iUniRepairAreaMapper.selectList(
                    new LambdaQueryWrapper<UniRepairAreaEntity>()
                            .eq(UniRepairAreaEntity::getSubCode, subCode)
                            .le(UniRepairAreaEntity::getStartTime, DateUtils.getNowDate())
                            .ge(UniRepairAreaEntity::getEndTime, DateUtils.getNowDate())
            );
            CenterToUavPlanEntity center = iCenterToUavPlanMapper.selectOne(
                    new LambdaQueryWrapper<CenterToUavPlanEntity>()
                            .eq(CenterToUavPlanEntity::getSubCode, subCode)
                            .eq(CenterToUavPlanEntity::getUavPlanCode, uavTaskPatrolledId)
            );
            String taskPatrolledId = center.getCenterTaskPatrolledId();

            //找到当前变电站的检修区域，找到检修点位
            List<String> repairPoints = repairPoints(repairs);
            for (PointCount point : list) {
                if (repairPoints == null || repairPoints.size() == 0 || !repairPoints.contains(point.getPubWaylinePointDfEntity().getPointCode())) {
                    //上传ftp数据
                    String destDir = fileConfig.getFileSavePath() + "/" + subCode + "/" + taskPatrolledId + "/";
                    String localFile = point.getMediaFileDTOS().get(0).getFilePath();
                    String destName = new File(localFile).getName();
                    FtpUtils.getInstance().uploadToCenterNormal(localFile, destDir, destName);
                    //推送点位报文
                    PatrolHostCommand commandData = patrolHostSocketClient.getBaseCommand("61", "", subCode);
                    commandData.addItem(genItem(point, center, taskPatrolledId, subCode, deviceName, deviceCode, String.format("%s/%s", destDir, destName)));
                    patrolHostSocketClient.sendCommand(commandData, PatrolResultItem.class);
                }
            }
        }
    }

    /**
     * 处理检修区域点位
     */
    public List<String> repairPoints(List<UniRepairAreaEntity> repairs) {
        if (repairs == null || repairs.size() == 0) {
            return null;
        }
        List<String> list = new ArrayList<>();
        for (UniRepairAreaEntity entity : repairs) {
            String pointStr = entity.getPointList();
            if (pointStr != null && pointStr.length() > 0) {
                List<String> points = Arrays.asList(pointStr.split(","));
                list.addAll(points);
            }
        }
        return list;
    }

    public PatrolResultItem genItem(PointCount point, CenterToUavPlanEntity center, String taskPatrolledId, String subCode, String deviceName, String deviceCode, String centerFilePath) {
        UniTaskPlanEntity plan = iUniTaskPlanMapper.selectOne(
                new LambdaQueryWrapper<UniTaskPlanEntity>()
                        .eq(UniTaskPlanEntity::getSubCode, subCode)
                        .eq(UniTaskPlanEntity::getPlanNo, center.getCenterPlanCode())
        );


        PatrolResultItem item = new PatrolResultItem();
        item.setPatroldevice_name(deviceName);
        item.setPatroldevice_code(deviceCode);
        item.setTask_name(plan.getPlanName());
        item.setTask_code(plan.getPlanNo());
        item.setDevice_name(point.getPubWaylinePointDfEntity().getPointName());
        item.setDevice_id(point.getPubWaylinePointDfEntity().getPointCode());
        item.setValue("");
        item.setUnit("");
        item.setValue_unit("");
        item.setTime(DateUtils.getNowDateTimeStr());
        for (RecognitionTypeEnum typeEnum : RecognitionTypeEnum.values()) {
            if (typeEnum.getDes().equals(deviceName)) {
                item.setRecognition_type(point.getPubWaylinePointDfEntity().getPointAnalyseType());
            }
        }
        if (point.getPubWaylinePointDfEntity().getTemType() == null) {
            item.setFile_type("2");
        } else {
            item.setFile_type("1");
        }
        item.setFile_path(centerFilePath);
        item.setRectangle("");
        item.setTask_patrolled_id(taskPatrolledId);
        item.setObj_id("");
        item.setValid("1");
        return item;
    }
}
