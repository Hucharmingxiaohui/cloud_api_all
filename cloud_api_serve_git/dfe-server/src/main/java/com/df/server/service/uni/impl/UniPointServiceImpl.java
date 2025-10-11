package com.df.server.service.uni.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.framework.config.FileConfig;
import com.df.framework.exception.FastException;
import com.df.framework.redis.RedisKeys;
import com.df.framework.redis.RedisUtils;
import com.df.framework.utils.*;
import com.df.framework.vo.PageVO;
import com.df.framework.vo.Tree;
import com.df.server.dto.uniPoint.*;
import com.df.server.entity.sys.SysDictDataEntity;
import com.df.server.entity.uni.*;
import com.df.server.mapper.uni.UniPointMapper;
import com.df.server.service.sys.SysDictDataService;
import com.df.server.service.uni.*;
import com.df.server.utils.PointDataTypeUtils;
import com.df.server.vo.UniPatrolSystem.UniPatrolSystemVO;
import com.df.server.vo.UniPoint.UniPointVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service("uniPointService")
public class UniPointServiceImpl extends ServiceImpl<UniPointMapper, UniPointEntity> implements UniPointService {

    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private PubSubstationService uniSubstationService;
    @Autowired
    private UniPatrolSystemService uniPatrolSystemService;
    @Autowired
    private UniAreaService uniAreaService;
    @Autowired
    private UniBayService uniBayService;
    @Autowired
    private UniDeviceService uniDeviceService;
    @Autowired
    private UniComponentService uniComponentService;
    @Autowired
    private SysDictDataService sysDictDataService;
    @Autowired
    private FileConfig fileConfig;


    @Override
    public List<Tree> pointTree(UniPointPageDTO paramsDto) {
        String subCode = paramsDto.getSubCode();
        String pointCode = paramsDto.getPointCode();
        String pointName = paramsDto.getPointName();
        if (StringUtils.isNotBlank(subCode) && StringUtils.isBlank(pointCode)
                && StringUtils.isBlank(pointName)) {
            Object o = redisUtils.get(RedisKeys.getPointTreeKey(subCode));
            if (o != null) {
                String string = o.toString();
                if (StringUtils.isNotBlank(string)) {
                    List<Tree> tree = JSON.parseObject(string, new TypeReference<List<Tree>>() {
                    });
                    return tree;
                }
            }
        }
        return baseMapper.pointTree(paramsDto);
    }

    @Override
    public List<UniPointImportExcel> listPointExport(UniPointPageDTO dto) {
        List<UniPointImportExcel> uniPointImportExcels = baseMapper.listPointExport(dto);
        for (UniPointImportExcel uniPointImportExcel : uniPointImportExcels) {
            uniPointImportExcel.setPointType(PointDataTypeUtils.judgeIntDataType(Integer.parseInt(uniPointImportExcel.getPointType())).get(0));
        }
        return uniPointImportExcels;
    }

    @Override
    public void addPoint(UniPointAddDTO addDTO) {
        UniPointEntity uniPointEntity = ConvertUtils.toBean(addDTO, UniPointEntity.class);
        checkPoint(uniPointEntity);
        uniPointEntity.setVideoPos(null);
        uniPointEntity.setBaseImagePath(null);
        this.save(uniPointEntity);
    }

    @Override
    public UniPointEntity getPointByCode(String subCode, String pointCode) {
        Optional<UniPointEntity> uniPointEntity = this.lambdaQuery().eq(UniPointEntity::getSubCode, subCode).eq(UniPointEntity::getPointCode, pointCode).oneOpt();
        return uniPointEntity.orElse(null);
    }

    @Override
    public void modifyPoint(UniPointUpdateDTO updateDTO) {
        UniPointEntity uniPointEntity = ConvertUtils.toBean(updateDTO, UniPointEntity.class);
        checkPoint(uniPointEntity);
        uniPointEntity.setVideoPos(null);
        uniPointEntity.setBaseImagePath(null);
        this.updateById(uniPointEntity);
    }

    @Override
    public JSONArray getPointACTTypeByReg() {
        List<SysDictDataEntity> recognitionTypeList = sysDictDataService.listDictEntityByDiceType("recognition_type");
        JSONArray array = new JSONArray();
        recognitionTypeList.forEach(regTypeObj -> {
            String regType = regTypeObj.getDictValue();
            JSONObject result = new JSONObject();
            result.put("regType", regType);
            result.put("pointAnalyseType", RecognitionTypeUtils.getAnalyseTypeByRegTypeAry(regType));
            result.put("pointAnalyseCategory", RecognitionTypeUtils.getCategoryByRegTypeAry(regType));
            result.put("taskType", RecognitionTypeUtils.getTaskTypeByRegTypeAry(regType));
            array.add(result);
        });
        return array;
    }

    @Override
    public List<Tree> pointLevelTree(UniTreeDTO dto) {
        return baseMapper.pointLevelTree(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importPoint(UniPointImportExcel pointExcel) {
        String operate = pointExcel.getOperate();
        String subCode = pointExcel.getSubCode();
        String pointCode = pointExcel.getPointCode();
        if ("2".equals(operate)) {
            this.lambdaUpdate().eq(UniPointEntity::getSubCode, subCode).eq(UniPointEntity::getPointCode, pointCode).remove();
            return;
        }
        String patrolHostCode = pointExcel.getSysCode();
        if (StringUtils.isBlank(patrolHostCode)) {
            patrolHostCode = uniPatrolSystemService.getVideoSysCode();
        }
        //获取直接接入型变电站所属的区域巡视主机系统编码
        if (StringUtils.isBlank(patrolHostCode)) {
            log.error("[配置管理-点位标准库-导入点位文件]同步过程中，获取系统编码异常，变电站编码:{}", subCode);
            throw new FastException("点位系统编码信息获取异常");
        }
        pointExcel.setSysCode(patrolHostCode);

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
        UniPointEntity point = pointImportDeal(pointExcel, area, bay, device, component);
        //--------------------5.df_uni_point表新增或更新记录 END--------------------


        //--------------------6.df_uni_point_video_pos表新增或更新记录--------------------
        //对导入的点位摄像机信息进行处理
        videoPosImportDeal(pointExcel);
        //--------------------6.df_uni_point_video_pos表新增或更新记录 END--------------------
    }

    /**
     * 对导入的部件信息进行处理
     *
     * @param pointExcel 表格中的每行数据
     * @param device     设备信息
     */
    private UniComponentEntity componentImportDeal(UniPointImportExcel pointExcel, UniDeviceEntity device) {
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
            log.error("[配置管理-点位标准库-导入点位文件]同步过程中，df_uni_component表同步异常，部件信息:[{}]，异常:[{}]", pointExcel, e.getMessage());
            throw new FastException("点位部件信息同步异常");
        }
    }

    /**
     * 对导入的设备信息进行处理
     *
     * @param pointExcel 表格中的每行数据
     * @param bay        间隔信息
     */
    private UniDeviceEntity deviceImportDeal(UniPointImportExcel pointExcel, UniBayEntity bay) {
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
            log.error("[配置管理-点位标准库-导入点位文件]同步过程中，df_uni_device表同步异常，设备信息:[{}]，异常:[{}]", bay, e.getMessage());
            throw new FastException("点位设备信息同步异常");
        }
    }

    /**
     * 对导入的间隔信息进行处理
     *
     * @param pointExcel 表格中的每行数据
     * @param area       区域信息
     */
    private UniBayEntity bayImportDeal(UniPointImportExcel pointExcel, UniAreaEntity area) {
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
            log.error("[配置管理-点位标准库-导入点位文件]同步过程中，df_uni_bay表同步异常，间隔信息:[{}]，异常:[{}]", pointExcel, e.getMessage());
            throw new FastException("点位间隔信息同步异常");
        }
    }

    /**
     * 对导入的区域信息进行处理
     *
     * @param pointExcel 表格中的每行数据
     */
    private UniAreaEntity areaImportDeal(UniPointImportExcel pointExcel) {
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
            log.error("[配置管理-点位标准库-导入点位文件]同步过程中，df_uni_area表同步异常，区域信息:[{}]，异常:[{}]", pointExcel, e.getMessage());
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
    private UniPointEntity pointImportDeal(UniPointImportExcel pointExcel,
                                           UniAreaEntity area,
                                           UniBayEntity bay,
                                           UniDeviceEntity device,
                                           UniComponentEntity component) {
        try {
            String subCode = pointExcel.getSubCode();
            String pointCode = pointExcel.getPointCode();
            String sysCode = pointExcel.getSysCode();
            if (StringUtils.isBlank(pointCode)) {
                pointExcel.setPointCode(UUID.randomUUID().toString().replaceAll("-", ""));
                pointCode = pointExcel.getPointCode();
            }
            UniPointEntity point = this.getPointByCode(subCode, pointCode);
            if (point == null) {
                point = new UniPointEntity();
                point.setPointCode(pointCode);
            }

            point.setSysCode(sysCode);
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
            this.saveOrUpdate(point);
            return point;
        } catch (Exception e) {
            log.error(
                    "[配置管理-点位标准库-导入点位文件]同步过程中，df_uni_point表同步异常，点位信息:[{}]，异常:[{}]", pointExcel, e);
            throw new FastException("点位信息同步异常");
        }
    }

    /**
     * 对导入的点位摄像机信息进行处理
     *
     * @param pointExcel 表格中的每行数据
     */
    private void videoPosImportDeal(UniPointImportExcel pointExcel) {
        try {
            String subCode = pointExcel.getSubCode();
            String pointCode = pointExcel.getPointCode();
            String robotCode = pointExcel.getRobotCode();
            String devicePos = pointExcel.getRobotPos();
            String waylinePos = pointExcel.getWaylinePos();
            String waylinePointPos = pointExcel.getWaylinePointPos();
            log.info("点位检查后导入，机器人：[{}],预置位号：[{}]", robotCode, devicePos);
            if (StringUtils.isBlank(robotCode) || StringUtils.isBlank(devicePos)) {
                return;
            }
            Long parsePos = 0L;
            try {
                parsePos = Long.parseLong(pointExcel.getRobotPos());
            } catch (NumberFormatException e) {
                return;
            }
            PointVideoPosDTO videoPosDTO = new PointVideoPosDTO();
            videoPosDTO.setRobot_code(robotCode);
            videoPosDTO.setRobot_pos(parsePos.toString());
            List<PointVideoPosDTO> pointVideoPosList = new ArrayList();
            pointVideoPosList.add(videoPosDTO);
            String videoPos = JSONArray.toJSONString(pointVideoPosList);
            baseMapper.clearRobotPos(devicePos, robotCode);
            baseMapper.clearUavPos(waylinePos, waylinePointPos);
            baseMapper.updatePointRobotPos(subCode, pointCode, videoPos, devicePos, robotCode);
            baseMapper.updatePointUavPos(subCode, pointCode,waylinePos, waylinePointPos);
        } catch (Exception e) {
            log.error("[配置管理-点位标准库-导入点位文件]同步过程中，机器人预置位号异常", e);
            throw new FastException("点位摄像机信息同步异常");
        }
    }

    private void checkPoint(UniPointEntity uniPointEntity) {
        Integer id = uniPointEntity.getId();
        Integer pointType = uniPointEntity.getPointType();
        String subCode = uniPointEntity.getSubCode();
        String pointCode = uniPointEntity.getPointCode();
        String areaId = uniPointEntity.getAreaId();
        String bayId = uniPointEntity.getBayId();
        String deviceId = uniPointEntity.getDeviceId();
        String componentId = uniPointEntity.getComponentId();
        Integer deviceType = uniPointEntity.getDeviceType();
        String sysCode = uniPointEntity.getSysCode();
        PubSubstationEntity substation = uniSubstationService.getStationBySubCode(subCode);
        if (substation == null) {
            throw new FastException("变电站编码有误，未找到变电站信息");
        }
        if (id == null) {
            if (StringUtils.isBlank(pointCode)) {
                uniPointEntity.setPointCode(UUID.randomUUID().toString().replace("-", ""));
            } else {
                UniPointEntity oldPoint = this.getPointByCode(subCode, pointCode);
                if (oldPoint != null) {
                    throw new FastException("点位编码重复");
                }
            }
        } else {
            if (StringUtils.isBlank(pointCode)) {
                throw new FastException("缺少点位编码");
            }
            UniPointEntity oldPoint = this.getPointByCode(subCode, pointCode);
            if (!oldPoint.getId().equals(id)) {
                throw new FastException("点位编码重复");
            }
        }
        UniAreaEntity area = uniAreaService.getByAreaId(subCode, areaId);
        if (area == null) {
            throw new FastException("区域编码有误，未找到区域信息");
        }
        UniBayEntity bay = uniBayService.getByBayId(subCode, bayId);
        if (bay == null) {
            throw new FastException("间隔编码有误，未找到间隔信息");
        }
        if (!bay.getAreaId().equals(areaId)) {
            throw new FastException("间隔编码有误，非对应区域下的间隔");
        }
        UniDeviceEntity device = uniDeviceService.getByDeviceId(subCode, deviceId);
        if (device == null) {
            throw new FastException("设备编码有误，未找到设备信息");
        }
        if (!device.getBayId().equals(bayId)) {
            throw new FastException("设备编码有误，非对应间隔下的设备");
        }
        UniComponentEntity component = uniComponentService.getByComponentId(subCode, componentId);
        if (component == null) {
            throw new FastException("部件编码有误，未找到部件信息");
        }
        if (!component.getDeviceId().equals(deviceId)) {
            throw new FastException("部件编码有误，非对应设备下的部件");
        }
        if (deviceType != null && device.getDeviceType() != null && !deviceType.equals(device.getDeviceType())) {
            throw new FastException("点位设备类型没有与主设备类型对应");
        }
        List<UniPatrolSystemVO> uniPatrolSystemVOS = uniPatrolSystemService.listPatrolSysBySysCode(sysCode);
        if (uniPatrolSystemVOS.size() == 0) {
            throw new FastException("巡视系统编码有误");
        }
        if (pointType == null && pointType != 2) {
            throw new FastException("点位类型错误，该系统仅限机器人");
        }
    }

    @Override
    public PageVO<UniPointVO> customPage(UniPointPageDTO pageDTO) {
        List<UniPointVO> list = baseMapper.getPageList(pageDTO);
        for (int i = 0; i < list.size(); i++) {
            UniPointVO uniPointVO = list.get(i);
            uniPointVO.setBaseImagePathVisitUrl(fileConfig.getFileVisitUrl(uniPointVO.getBaseImagePath()));
        }
        Integer total = baseMapper.getPageTotal(pageDTO);
        return PageUtils.returnPageVOExtend(pageDTO, list, total);
    }

}
