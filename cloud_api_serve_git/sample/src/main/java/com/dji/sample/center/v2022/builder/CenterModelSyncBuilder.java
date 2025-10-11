package com.dji.sample.center.v2022.builder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.app.AppContext;
import com.dji.sample.center.config.CenterFtpsNormalConfig;
import com.dji.sample.center.config.CenterNormalConfig;
import com.dji.sample.center.utils.StringUtils;
import com.dji.sample.center.utils.ftp.FtpUtils;
import com.dji.sample.center.v2022.command.model.PatrolUavModelItem;
import com.dji.sample.center.v2022.command.model.PatrolUavPointModelData;
import com.dji.sample.center.v2022.command.model.PatrolUavPointModelItem;
import com.dji.sample.center.v2022.enums.DeviceTypeEnum;
import com.dji.sample.center.v2022.tool.BaseItem;
import com.dji.sample.center.v2022.tool.CenterXmlTool;
import com.dji.sample.df.electricInspectionDf.dao.PubSubstationDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubSubstationDfEntity;
import com.dji.sample.df.manageDf.dao.IWaylinePointMapper;
import com.dji.sample.df.manageDf.model.entity.WaylinePointEntity;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 上级模型文件同步
 */
@Slf4j
public class CenterModelSyncBuilder {

    private CenterFtpsNormalConfig centerFtpsNormalConfig = AppContext.getBean(CenterFtpsNormalConfig.class);
    private IWaylinePointMapper iWaylinePointMapper = AppContext.getBean(IWaylinePointMapper.class);
    private CenterNormalConfig centerNormalConfig = AppContext.getBean(CenterNormalConfig.class);
    private PubSubstationDfMapper pubSubstationDfMapper = AppContext.getBean(PubSubstationDfMapper.class);

    private static CenterModelSyncBuilder instance = null;

    public static CenterModelSyncBuilder getInstance() {
        if (instance == null) {
            instance = new CenterModelSyncBuilder();
        }
        return instance;
    }

    /**
     * 巡视主机模型
     *
     * @param subCode
     * @return
     */
    public List<BaseItem> genPatrolUavFile(String subCode) {
        List<BaseItem> items = new ArrayList<>();
        List<PatrolUavPointModelItem> data = new ArrayList<>();
        List<WaylinePointEntity> list = iWaylinePointMapper.selectAllData(subCode);
        List<PubSubstationDfEntity> pubSubstationDfEntities = pubSubstationDfMapper.selectList(
                new LambdaQueryWrapper<PubSubstationDfEntity>()
                        .eq(PubSubstationDfEntity::getSubCode, subCode)
        );
        System.out.println( pubSubstationDfEntities.get(0).getSubName());
        String subName = pubSubstationDfEntities.get(0).getSubName();
        if (list != null && list.size() > 0) {
            for (WaylinePointEntity entity : list) {
                PatrolUavPointModelItem item = new PatrolUavPointModelItem();
                item.setStation_code(subCode);
                item.setStation_name(subName);
                item.setArea_id(Optional.ofNullable(entity.getAreaId()).orElse(""));
                item.setArea_name(Optional.ofNullable(entity.getAreaName()).orElse(""));
                item.setData_type("2");
                item.setLower_value("");
                item.setUpper_value("");
                item.setVideo_pos("");
                item.setDevice_id(Optional.ofNullable(entity.getPointCode()).orElse(""));
                item.setDevice_name(Optional.ofNullable(entity.getPointName()).orElse(""));
                item.setComponent_id(Optional.ofNullable(entity.getComponentId()).orElse(""));
                item.setComponent_name(Optional.ofNullable(entity.getComponentName()).orElse(""));
                item.setBay_id(Optional.ofNullable(entity.getBayId()).orElse(""));
                item.setBay_name(Optional.ofNullable(entity.getBayName()).orElse(""));
                item.setMain_device_id(Optional.ofNullable(entity.getDeviceId()).orElse(""));
                item.setMain_device_name(Optional.ofNullable(entity.getDeviceName()).orElse(""));
                item.setDevice_type(deviceTypeNum(entity.getDeviceType()));
                //无人机航拍不巡视表计，暂时设置为空
                item.setMeter_type("");
                //无人机点位外观类型，暂时设置为空
                item.setAppearance_type("");
                //现场不需要分析，暂时设置为空
                item.setRecognition_type_list(Optional.ofNullable(entity.getPointAnalyseType()).orElse(""));
                item.setPhase(Optional.ofNullable(entity.getPhase()).orElse(""));
                item.setDevice_info("");
                if (entity.getTemType() == null) {
                    item.setSave_type_list("2");
                } else {
                    item.setSave_type_list("1");
                }
                data.add(item);
            }

            //文件规范：变电站编码/Model/alarm_threshold_巡视主机编码.xml
            String fileName = String.format("%s/Model/device_model_%s.xml", subCode, centerNormalConfig.getStationCode());
            //生成模型文件
            PatrolUavPointModelData modelData = new PatrolUavPointModelData();
            modelData.item = data;
            this.genModelFile(fileName, modelData);
            //组织响应消息体
            PatrolUavModelItem patrolUavModelItem = new PatrolUavModelItem();
            patrolUavModelItem.setDevice_file_path(fileName);
            items.add(patrolUavModelItem);
        }
        return items;
    }

    /**
     * 上传文件到巡视上级FTP
     *
     * @param fileName
     */
    public void uploadModelFile(String fileName) {
        String modelSavePath = centerFtpsNormalConfig.getFileSavePath();
        String localFile = String.format("%s/%s", modelSavePath, fileName);
        String destName = new File(localFile).getName();
        String destDir = fileName.substring(0, fileName.length() - destName.length());
        FtpUtils.getInstance().uploadToCenterNormal(localFile, destDir, destName);
    }

    /**
     * 生成模型文件公共方法
     *
     * @param fileName
     * @param t
     */
    protected <T> void genModelFile(String fileName, T t) {
        String modelSavePath = centerFtpsNormalConfig.getFileSavePath();
        String fullName = String.format("%s/%s", modelSavePath, fileName);
        File file = new File(fullName);
        File dir = file.getParentFile();
        if (!dir.exists()) {// 判断目录是否存在
            dir.mkdirs();
        }
        CenterXmlTool.toFile(fullName, t);
    }

    public String deviceTypeNum(String deviceName) {
        if (StringUtils.isEmpty(deviceName)) {
            return "";
        }
        for (DeviceTypeEnum item : DeviceTypeEnum.values()) {
            if (item.getDes().equals(deviceName)) {
                return item.getTypeNum().toString();
            }
        }
        return "";
    }

}
