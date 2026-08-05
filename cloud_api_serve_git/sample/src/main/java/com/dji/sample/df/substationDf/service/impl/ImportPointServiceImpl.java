package com.dji.sample.df.substationDf.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.framework.exception.FastException;
import com.df.framework.utils.CustomStringUtils;
import com.df.framework.utils.RecognitionTypeUtils;
import com.df.server.entity.uni.*;
import com.df.server.service.uni.*;
import com.df.server.utils.PointDataTypeUtils;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.commonDf.util.PageUtil;
import com.dji.sample.df.substationDf.model.entity.UniPointImportExcel2;
import com.dji.sample.df.substationDf.service.ImportPointService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ImportPointServiceImpl extends ServiceImpl<UniPointMapper2, UniPoint> implements ImportPointService {

    @Autowired
    UniPointMapper2 uniPointMapper2;

    @Value("${importPoint.subName}")
    String subName;

    @Value("${importPoint.systemCode}")
    String systemCode;

    @Autowired
    private UniAreaService uniAreaService;
    @Autowired
    private UniBayService uniBayService;
    @Autowired
    private UniDeviceService uniDeviceService;
    @Autowired
    private UniComponentService uniComponentService;


//    @Override
//    public void importPoint(UniPointImportExcel2 point) {
//        UniPoint uniPoint = new UniPoint();
//
//        // 基本字段赋值
//         uniPoint.setPointCode(point.getPointCode());
//         uniPoint.setPointName(point.getPointName());
//         uniPoint.setSubCode(point.getSubCode());
//         uniPoint.setAreaName(point.getAreaName());
//         uniPoint.setBayName(point.getBayName());
//         uniPoint.setComponentName(point.getComponentName());
//         uniPoint.setDeviceName(point.getDeviceName());
//         uniPoint.setPointDes(point.getPointInfo());
//         uniPoint.setMapPos(point.getMapPos());
//         uniPoint.setUpperValue(point.getUpperValue());
//         uniPoint.setLowerValue(point.getLowerValue());
//         uniPoint.setTaskType(point.getTaskType());
//         uniPoint.setTaskSubType(point.getTaskSubType());
//         uniPoint.setPointAnalyseCategory(Integer.parseInt(point.getPointAnalyseCategory()));
//         uniPoint.setPointAnalyseType(point.getPointAnalyseType());
//         uniPoint.setWaylineId(point.getWaylineId());
//         uniPoint.setPicType(Integer.parseInt(point.getPicType()));
//         uniPoint.setWaylinePointPos(point.getWaylinePointPos());
//
//        // 需要类型转换的字段
//        // 设备类型转换（String -> Long）
//                if (StringUtils.isNotBlank(point.getDeviceType())) {
//                    try {
//                        uniPoint.setDeviceType(Long.parseLong(point.getDeviceType()));
//                    } catch (NumberFormatException e) {
//                        // 处理转换异常，可以记录日志或设置默认值
//                        uniPoint.setDeviceType(null);
//                    }
//                }
//
//        // 表计类型转换（String -> Integer）
//                if (StringUtils.isNotBlank(point.getMeterType())) {
//                    try {
//                        uniPoint.setMeterType(Integer.parseInt(point.getMeterType()));
//                    } catch (NumberFormatException e) {
//                        uniPoint.setMeterType(null);
//                    }
//                }
//
//        // 外观类型转换（String -> Integer）
//                if (StringUtils.isNotBlank(point.getAppearanceType())) {
//                    try {
//                        uniPoint.setAppearanceType(Integer.parseInt(point.getAppearanceType()));
//                    } catch (NumberFormatException e) {
//                        uniPoint.setAppearanceType(null);
//                    }
//                }
//
//        // 保存类型列表（直接赋值，都是字符串）
//                uniPoint.setSaveTypeList(point.getSaveTypeList());
//
//        // 识别类型列表（直接赋值）
//                uniPoint.setRecognitionTypeList(point.getRecognitionTypeList());
//
//        // 相位（直接赋值）
//                uniPoint.setPhase(point.getPhase());
//
//        // 重要等级转换（String -> Integer）
//                if (StringUtils.isNotBlank(point.getLevel())) {
//                    try {
//                        uniPoint.setLevel(Integer.parseInt(point.getLevel()));
//                    } catch (NumberFormatException e) {
//                        uniPoint.setLevel(null);
//                    }
//                }
//
//        // 是否实物识别转换（String -> Integer）
//                if (StringUtils.isNotBlank(point.getIsObj())) {
//                    try {
//                        uniPoint.setIsObj(Integer.parseInt(point.getIsObj()));
//                    } catch (NumberFormatException e) {
//                        uniPoint.setIsObj(null);
//                    }
//                }
//
//        // 点位类型转换（需要映射）
//                if (StringUtils.isNotBlank(point.getPointType())) {
//                    switch (point.getPointType().toLowerCase()) {
//                        case "camera":
//                        case "video":
//                            uniPoint.setPointType(3); // 视频点位
//                            break;
//                        case "robot":
//                            uniPoint.setPointType(2); // 机器人点位
//                            break;
//                        case "uav":
//                        case "voice":
//                        case "online":
//                        default:
//                            uniPoint.setPointType(4); // 都有或其他
//                            break;
//                    }
//                }
//
//        // 其他可能需要的字段（根据实际业务需求）
//        // 如果有需要可以设置创建时间和更新时间
//                uniPoint.setCreateTime(new Date());
//                uniPoint.setUpdateTime(new Date());
//
//        // 设置默认值
//                uniPoint.setIsWhitelist(0); // 默认不加入白名单
//                uniPoint.setIsFocus(0); // 默认未关注
//                uniPoint.setIsRedundancy(0); // 默认无冗余
//                uniPoint.setPatrolWay(0); // 默认智能巡视
//
//        // 注意：以下字段在UniPointImportExcel2中没有对应字段，需要从其他地方获取或留空
//         uniPoint.setSubName(subName); // 需要从其他表查询
//         uniPoint.setSysCode(systemCode); // 系统编码
//         uniPoint.setComponentId(UUID.randomUUID().toString()); // 部件ID
//         uniPoint.setMaterialId(UUID.randomUUID().toString()); // 实物ID
//         uniPoint.setBayId(UUID.randomUUID().toString()); // 间隔ID
//         uniPoint.setDeviceId(UUID.randomUUID().toString()); // 设备ID
//         uniPoint.setMapfileId(null); // 地图文件ID
//         uniPoint.setAreaId(UUID.randomUUID().toString()); // 区域ID
//         uniPoint.setVideoPos(null); // 视频预置位
//         uniPoint.setLabelAttri(null); // 标签属性
//         uniPoint.setBaseImagePath(null); // 基准图路径
//        LambdaQueryWrapper<UniPoint> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(UniPoint::getPointName, uniPoint.getPointName()).eq(UniPoint::getPicType, uniPoint.getPicType());
//
//        UniPoint existingPoint = uniPointMapper2.selectOne(queryWrapper);
//
//        if (existingPoint != null) {
//            // 如果存在，设置ID进行更新
//            uniPoint.setId(existingPoint.getId());
//            uniPointMapper2.updateById(uniPoint);
//        } else {
//            // 不存在则插入
//            uniPointMapper2.insert(uniPoint);
//        }
//    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importPoint(UniPointImportExcel2 pointExcel)  {
        String operate = pointExcel.getOperate();
        String subCode = pointExcel.getSubCode();
        String pointCode = pointExcel.getPointCode();
        if ("2".equals(operate)) {
            this.lambdaUpdate().eq(UniPoint::getSubCode, subCode).eq(UniPoint::getPointCode, pointCode).remove();
            return;
        }

        //--------------------1.df_uni_area表新增或更新记录--------------------
        //对导入的区域信息进行处理
        UniAreaEntity area = areaImportDeal(pointExcel);
        //--------------------1.df_uni_area表新增或更新记录 End--------------------


        //--------------------2.df_uni_bay表新增或更新记录-------------------------
        //对导入的间隔信息进行处理
        UniBayEntity bay = bayImportDeal(pointExcel, area);
        //--------------------2.df_uni_bay表新增或更新记录 End-------------------------


        //--------------------3.df_uni_device表新增或更新记录--------------------
        //对导入的设备信息进行处理
        UniDeviceEntity device = deviceImportDeal(pointExcel, bay);
        //--------------------3.df_uni_device表新增或更新记录 End--------------------


        //--------------------4.df_uni_component表新增或更新记录--------------------
        //对导入的部件信息进行处理
        UniComponentEntity component = componentImportDeal(pointExcel, device);
        //--------------------4.df_uni_component表新增或更新记录 End--------------------


        //--------------------5.df_uni_point表新增或更新记录--------------------
        //对导入的点位信息进行处理
        UniPoint point = pointImportDeal(pointExcel, area, bay, device, component);
        //--------------------5.df_uni_point表新增或更新记录 END--------------------
    }

    /**
     * 对导入的部件信息进行处理
     *
     * @param pointExcel 表格中的每行数据
     * @param device     设备信息
     */
    private UniComponentEntity componentImportDeal(UniPointImportExcel2 pointExcel, UniDeviceEntity device) {
        try {
            String subCode = pointExcel.getSubCode();
            String componentName = pointExcel.getComponentName();
            String deviceId = device.getDeviceId();

            UniComponentEntity component = uniComponentService.getByName(subCode, deviceId, componentName);
            if (component == null) {
                component = new UniComponentEntity();
                component.setSubCode(subCode);
                component.setComponentName(componentName);
                component.setDeviceId(deviceId);
                component.setComponentId(UUID.randomUUID().toString());
                uniComponentService.save(component);
            }
            return component;
        } catch (Exception e) {
            log.error("");
            throw new FastException("点位部件信息同步异常");
        }
    }

    /**
     * 对导入的设备信息进行处理
     *
     * @param pointExcel 表格中的每行数据
     * @param bay        间隔信息
     */
    private UniDeviceEntity deviceImportDeal(UniPointImportExcel2 pointExcel, UniBayEntity bay) {
        try {
            String subCode = pointExcel.getSubCode();
            String deviceName = pointExcel.getDeviceName();
            String deviceType = pointExcel.getDeviceType();
            String bayId = bay.getBayId();
            UniDeviceEntity deviceEntity = uniDeviceService.getByName(subCode, bayId, deviceName);
            if (deviceEntity == null) {
                deviceEntity = new UniDeviceEntity();
                deviceEntity.setDeviceId(UUID.randomUUID().toString());
                deviceEntity.setSubCode(subCode);
                deviceEntity.setBayId(bayId);
                deviceEntity.setDeviceName(deviceName);
            }
            deviceEntity.setDeviceType(CustomStringUtils.isPositiveInteger(deviceType) ? Integer.parseInt(deviceType) : null);
            uniDeviceService.saveOrUpdate(deviceEntity);
            return deviceEntity;
        } catch (Exception e) {
            log.error("");
            throw new FastException("点位设备信息同步异常");
        }
    }

    /**
     * 对导入的间隔信息进行处理
     *
     * @param pointExcel 表格中的每行数据
     * @param area       区域信息
     */
    private UniBayEntity bayImportDeal(UniPointImportExcel2 pointExcel, UniAreaEntity area) {
        try {
            String subCode = pointExcel.getSubCode();
            String areaId = area.getAreaId();
            String bayName = pointExcel.getBayName();

            UniBayEntity bayEntity = uniBayService.getByName(subCode, areaId, bayName);
            if (bayEntity == null) {
                bayEntity = new UniBayEntity();
                bayEntity.setBayId(UUID.randomUUID().toString());
                bayEntity.setSubCode(subCode);
                bayEntity.setBayName(bayName);
                bayEntity.setAreaId(areaId);
                uniBayService.save(bayEntity);
            }
            return bayEntity;
        } catch (Exception e) {
            log.error("");
            throw new FastException("点位间隔信息同步异常");
        }
    }

    /**
     * 对导入的区域信息进行处理
     *
     * @param pointExcel 表格中的每行数据
     */
    private UniAreaEntity areaImportDeal(UniPointImportExcel2 pointExcel) {
        try {
            UniAreaEntity area = uniAreaService.getByName(pointExcel.getAreaName(), pointExcel.getSubCode());
            if (area == null) {
                area = new UniAreaEntity();
                area.setAreaId(UUID.randomUUID().toString());
                area.setAreaName(pointExcel.getAreaName());
                area.setSubCode(pointExcel.getSubCode());
                uniAreaService.save(area);
            }
            return area;
        } catch (Exception e) {
            log.error("");
            throw new FastException("点位区域信息同步异常");
        }
    }

    /**
     * 对导入的点位信息进行处理
     *
     * @param pointExcel 表格中的每行数据
     * @param area       区域信息
     * @param bay        间隔信息
     * @param device     设备信息
     * @param component  部件信息
     */
    private UniPoint pointImportDeal(UniPointImportExcel2 pointExcel,
                                           UniAreaEntity area,
                                           UniBayEntity bay,
                                           UniDeviceEntity device,
                                           UniComponentEntity component) {
        try {
            String subCode = pointExcel.getSubCode();
            String pointCode = pointExcel.getPointCode();
            if (StringUtils.isBlank(pointCode)) {
                pointExcel.setPointCode(UUID.randomUUID().toString().replaceAll("-", ""));
                pointCode = pointExcel.getPointCode();
            }
            UniPoint point = this.getPointByCode(subCode, pointCode);
            if (point == null) {
                point = new UniPoint();
                point.setPointCode(pointCode);
            }
            point.setSubName(subName); // 需要从其他表查询
            point.setSysCode(systemCode);
            point.setSubCode(subCode);
            point.setAreaId(area.getAreaId());
            point.setAreaName(pointExcel.getAreaName());
            point.setBayId(bay.getBayId());
            point.setBayName(pointExcel.getBayName());
            point.setComponentId(component.getComponentId());
            point.setComponentName(pointExcel.getComponentName());
            point.setDeviceId(device.getDeviceId());
            point.setDeviceName(pointExcel.getDeviceName());
            if (CustomStringUtils.isPositiveInteger(pointExcel.getDeviceType())) {
                point.setDeviceType(Integer.valueOf(pointExcel.getDeviceType()));
            }
            point.setPointName(pointExcel.getPointName());
            //获取点位类型枚举列表
            List<String> pointTypeList = Arrays.asList(StringUtils.split(pointExcel.getPointType(), ","));
            int pointType = PointDataTypeUtils.judgeDataTypeList(pointTypeList);
            point.setPointType(pointType);
            if (CustomStringUtils.isPositiveInteger(pointExcel.getMeterType())) {
                point.setMeterType(Integer.valueOf(pointExcel.getMeterType()));
            }
            point.setSaveTypeList(pointExcel.getSaveTypeList());
            point.setRecognitionTypeList(pointExcel.getRecognitionTypeList());
            /* 入库识别类型recognition_type_list
             * 并根据对recognition_type_list智能分析识别大类point_analyse_category和智能分析子类point_analyse_type设置默认关联
             * 对应关系如下：
             * 识别类型  | 智能分析识别大类 | 智能分析子类
             * 表计读取------设备状态识别----仪表读数
             * 位置状态识别---设备状态识别----开关/压板状态
             * 设备外观查看---缺陷类别识别----17类缺陷
             */
            if (!StringUtils.isEmpty(pointExcel.getRecognitionTypeList())
                    && StringUtils.isBlank(pointExcel.getPointAnalyseCategory())
                    && StringUtils.isBlank(pointExcel.getPointAnalyseType())) {
                String recognitionTypeList = pointExcel.getRecognitionTypeList();
                List<String> recgList = Arrays.asList(recognitionTypeList.split(","));
                if (recgList.size() > 0) {
                    //只考虑recognition_type_list一个值的情况
                    String type = recgList.get(0);

                    point.setPointAnalyseCategory(
                            RecognitionTypeUtils.getCategoryByRegTypeAry(type));
                    point.setPointAnalyseType(
                            RecognitionTypeUtils.getAnalyseTypeByRegTypeAry(type));
                }
            }
            if (CustomStringUtils.isPositiveInteger(pointExcel.getPointAnalyseCategory())
                    && StringUtils.isNotBlank(pointExcel.getPointAnalyseType())) {
                point.setPointAnalyseCategory(Integer.parseInt(pointExcel.getPointAnalyseCategory()));
                point.setPointAnalyseType(pointExcel.getPointAnalyseType());
            }
            if (!StringUtils.isEmpty(pointExcel.getPhase())) {
                point.setPhase(pointExcel.getPhase());
            }
            if (CustomStringUtils.isPositiveInteger(pointExcel.getLevel())
                    && pointExcel.getLevel().matches("^[1-3]*$")) {
                point.setLevel(Integer.parseInt(pointExcel.getLevel()));
            }
            point.setPointDes(pointExcel.getPointInfo());
            point.setMapPos(pointExcel.getMapPos());
            point.setUpperValue(pointExcel.getUpperValue());
            point.setLowerValue(pointExcel.getLowerValue());
            point.setTaskSubType(pointExcel.getTaskSubType());
            point.setTaskType(pointExcel.getTaskType());
            point.setAppearanceType(Optional.ofNullable(pointExcel.getAppearanceType()).map(Integer::valueOf).orElse(null));
            //point.set(Optional.ofNullable(pointExcel.getIs_obj()).map(Integer::valueOf).orElse(null));

            /*String pointCode = dfUniPointMapper.selectDfUniPointByName(pointExcel.getPoint_name(), component.getComponent_id(), pointExcel.getSub_code());
            if (StringUtil.isEmpty(pointCode)) {
                point.setPoint_code(UUID.randomUUID().toString());
            } else {
                point.setPoint_code(pointCode);
            }*/
            point.setWaylineId(pointExcel.getWaylineId());
            point.setPicType(Integer.parseInt(pointExcel.getPicType()));
            point.setWaylinePointPos(pointExcel.getWaylinePointPos());
            this.saveOrUpdate(point);
            return point;
        } catch (Exception e) {
            log.error("");
            throw new FastException("点位信息同步异常");
        }
    }

    @Override
    public UniPoint getPointByCode(String subCode, String pointCode) {
        Optional<UniPoint> uniPointEntity = this.lambdaQuery().eq(UniPoint::getSubCode, subCode).eq(UniPoint::getPointCode, pointCode).oneOpt();
        return uniPointEntity.orElse(null);
    }



    @Override
    public Map<String, Object> selectList(Map map) {
        PageUtil.setPageArgs(map);
        List<UniPoint> uniPoints = uniPointMapper2.selectListByMap(map);
        int count = uniPointMapper2.selectListCount(map);
        Map result = new HashMap();
        Map pagination = new HashMap();
        pagination.put("page",Integer.parseInt(map.get("page").toString()));
        pagination.put("pageSize",Integer.parseInt(map.get("pageSize").toString()));
        pagination.put("total",count);
        result.put("list", uniPoints);
        result.put("pagination", pagination);
        return result;
    }

    @Override
    public int batchDelete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
         return uniPointMapper2.deleteBatchIds(ids);
    }
}
