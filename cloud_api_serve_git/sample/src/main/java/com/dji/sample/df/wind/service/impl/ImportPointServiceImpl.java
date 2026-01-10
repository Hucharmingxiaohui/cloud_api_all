package com.dji.sample.df.wind.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.server.dto.uniPoint.UniPointImportExcel;
import com.dji.sample.center.dao.UniPointMapper2;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.wind.model.entity.UniPointImportExcel2;
import com.dji.sample.df.wind.service.ImportPointService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class ImportPointServiceImpl implements ImportPointService {

    @Autowired
    UniPointMapper2 uniPointMapper2;

    @Value("${importPoint.subName}")
    String subName;

    @Value("${importPoint.systemCode}")
    String systemCode;

    @Override
    public void importPoint(UniPointImportExcel2 point) {
        UniPoint uniPoint = new UniPoint();

        // 基本字段赋值
         uniPoint.setPointCode(point.getPointCode());
         uniPoint.setPointName(point.getPointName());
         uniPoint.setSubCode(point.getSubCode());
         uniPoint.setAreaName(point.getAreaName());
         uniPoint.setBayName(point.getBayName());
         uniPoint.setComponentName(point.getComponentName());
         uniPoint.setDeviceName(point.getDeviceName());
         uniPoint.setPointDes(point.getPointInfo());
         uniPoint.setMapPos(point.getMapPos());
         uniPoint.setUpperValue(point.getUpperValue());
         uniPoint.setLowerValue(point.getLowerValue());
         uniPoint.setTaskType(point.getTaskType());
         uniPoint.setTaskSubType(point.getTaskSubType());
         uniPoint.setPointAnalyseCategory(Integer.parseInt(point.getPointAnalyseCategory()));
         uniPoint.setPointAnalyseType(point.getPointAnalyseType());
         uniPoint.setWaylineId(point.getWaylineId());
         uniPoint.setWaylinePos(point.getWaylinePos());
         uniPoint.setWaylinePointPos(point.getWaylinePointPos());

        // 需要类型转换的字段
        // 设备类型转换（String -> Long）
                if (StringUtils.isNotBlank(point.getDeviceType())) {
                    try {
                        uniPoint.setDeviceType(Long.parseLong(point.getDeviceType()));
                    } catch (NumberFormatException e) {
                        // 处理转换异常，可以记录日志或设置默认值
                        uniPoint.setDeviceType(null);
                    }
                }

        // 表计类型转换（String -> Integer）
                if (StringUtils.isNotBlank(point.getMeterType())) {
                    try {
                        uniPoint.setMeterType(Integer.parseInt(point.getMeterType()));
                    } catch (NumberFormatException e) {
                        uniPoint.setMeterType(null);
                    }
                }

        // 外观类型转换（String -> Integer）
                if (StringUtils.isNotBlank(point.getAppearanceType())) {
                    try {
                        uniPoint.setAppearanceType(Integer.parseInt(point.getAppearanceType()));
                    } catch (NumberFormatException e) {
                        uniPoint.setAppearanceType(null);
                    }
                }

        // 保存类型列表（直接赋值，都是字符串）
                uniPoint.setSaveTypeList(point.getSaveTypeList());

        // 识别类型列表（直接赋值）
                uniPoint.setRecognitionTypeList(point.getRecognitionTypeList());

        // 相位（直接赋值）
                uniPoint.setPhase(point.getPhase());

        // 重要等级转换（String -> Integer）
                if (StringUtils.isNotBlank(point.getLevel())) {
                    try {
                        uniPoint.setLevel(Integer.parseInt(point.getLevel()));
                    } catch (NumberFormatException e) {
                        uniPoint.setLevel(null);
                    }
                }

        // 是否实物识别转换（String -> Integer）
                if (StringUtils.isNotBlank(point.getIsObj())) {
                    try {
                        uniPoint.setIsObj(Integer.parseInt(point.getIsObj()));
                    } catch (NumberFormatException e) {
                        uniPoint.setIsObj(null);
                    }
                }

        // 点位类型转换（需要映射）
                if (StringUtils.isNotBlank(point.getPointType())) {
                    switch (point.getPointType().toLowerCase()) {
                        case "camera":
                        case "video":
                            uniPoint.setPointType(3); // 视频点位
                            break;
                        case "robot":
                            uniPoint.setPointType(2); // 机器人点位
                            break;
                        case "uav":
                        case "voice":
                        case "online":
                        default:
                            uniPoint.setPointType(4); // 都有或其他
                            break;
                    }
                }

        // 其他可能需要的字段（根据实际业务需求）
        // 如果有需要可以设置创建时间和更新时间
                uniPoint.setCreateTime(new Date());
                uniPoint.setUpdateTime(new Date());

        // 设置默认值
                uniPoint.setIsWhitelist(0); // 默认不加入白名单
                uniPoint.setIsFocus(0); // 默认未关注
                uniPoint.setIsRedundancy(0); // 默认无冗余
                uniPoint.setPatrolWay(0); // 默认智能巡视

        // 注意：以下字段在UniPointImportExcel2中没有对应字段，需要从其他地方获取或留空
         uniPoint.setSubName(subName); // 需要从其他表查询
         uniPoint.setSysCode(systemCode); // 系统编码
         uniPoint.setComponentId(UUID.randomUUID().toString()); // 部件ID
         uniPoint.setMaterialId(UUID.randomUUID().toString()); // 实物ID
         uniPoint.setBayId(UUID.randomUUID().toString()); // 间隔ID
         uniPoint.setDeviceId(UUID.randomUUID().toString()); // 设备ID
         uniPoint.setMapfileId(null); // 地图文件ID
         uniPoint.setAreaId(UUID.randomUUID().toString()); // 区域ID
         uniPoint.setVideoPos(null); // 视频预置位
         uniPoint.setLabelAttri(null); // 标签属性
         uniPoint.setBaseImagePath(null); // 基准图路径
        LambdaQueryWrapper<UniPoint> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UniPoint::getPointName, uniPoint.getPointName());

        UniPoint existingPoint = uniPointMapper2.selectOne(queryWrapper);

        if (existingPoint != null) {
            // 如果存在，设置ID进行更新
            uniPoint.setId(existingPoint.getId());
            uniPointMapper2.updateById(uniPoint);
        } else {
            // 不存在则插入
            uniPointMapper2.insert(uniPoint);
        }
    }
}
