package com.df.server.service.uni.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.framework.config.FileConfig;
import com.df.framework.utils.CustomStringUtils;
import com.df.framework.utils.PageUtils;
import com.df.framework.vo.PageVO;
import com.df.server.dto.uniPoint.CheckParams;
import com.df.server.dto.uniPoint.UniPointExcelImportErrorDTO;
import com.df.server.dto.uniPoint.UniPointImportExcel;
import com.df.server.entity.uni.UniPointExcelEntity;
import com.df.server.entity.uni.UniPointExcelImportErrorEntity;
import com.df.server.entity.uni.UniRobotEntity;
import com.df.server.mapper.uni.UniPointExcelImportErrorMapper;
import com.df.server.service.sys.SysDictDataService;
import com.df.server.service.uni.*;
import com.df.server.utils.ExcelUtil;
import com.df.server.vo.UniPatrolSystem.UniPatrolSystemVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service("uniPointExcelImportErrorService")
public class UniPointExcelImportErrorServiceImpl extends ServiceImpl<UniPointExcelImportErrorMapper, UniPointExcelImportErrorEntity> implements UniPointExcelImportErrorService {

    private static final ConcurrentLinkedQueue<UniPointExcelEntity> ExcelQueue = new ConcurrentLinkedQueue<>();

    private static final Pattern zhibiao = Pattern.compile("\\t|\n|\r");


    private static final Pattern specialp = Pattern.compile("[`$%^&*+={}'<>~!！￥…‘；;”“’。，?？]");

    private AtomicBoolean isCheckRunning = new AtomicBoolean(false);

    private static CheckParams checkParams = new CheckParams();

    private static final List<String> pointTypeList = Arrays.asList("camera", "robot", "uav", "voice", "online");


    private static ConcurrentHashMap<String, Set<String>> robotPosMap = new ConcurrentHashMap<>();


    @Autowired
    private FileConfig fileConfig;
    @Autowired
    private PubSubstationService uniSubstationService;
    @Autowired
    @Lazy
    private UniPointExcelService uniPointExcelService;
    @Autowired
    private SysDictDataService sysDictDataService;
    @Autowired
    private UniPatrolSystemService uniPatrolSystemService;
    @Autowired
    private UniRobotService uniRobotService;

    @Override
    public void addNewCheckExcel(UniPointExcelEntity excel) {
        excel.setImportStatus(1);//检查中
        uniPointExcelService.updateById(excel);
        ExcelQueue.add(excel);
    }

    @Override
    public PageVO<UniPointExcelImportErrorEntity> getPage(UniPointExcelImportErrorDTO dto) {
        List<UniPointExcelImportErrorEntity> data = baseMapper.listError(dto);
        int total = baseMapper.listErrorTotal(dto);
        return PageUtils.returnPageVOExtend(dto, data, total);
    }

    @Override
    public List<UniPointExcelImportErrorEntity> listError(UniPointExcelImportErrorDTO dto) {
        return baseMapper.listError(dto);
    }

    @Override
    public void clearErrorInfo() {
        baseMapper.clearErrorInfo();
    }

    @Scheduled(fixedDelay = 3 * 1000L)
    public void executeCheckExcel() {
        //没有正在执行生成时才去获取一个请求进入生成流程
        if (!isCheckRunning.get()) {
            UniPointExcelEntity excel = ExcelQueue.poll();
            if (excel != null) {
                isCheckRunning.set(true);
                try {
                    checkErr(excel);
                } catch (Exception e) {
                    e.printStackTrace();

                    UniPointExcelImportErrorEntity uniPointExcelImportError = new UniPointExcelImportErrorEntity();
                    uniPointExcelImportError.setErrorFileId(excel.getId());
                    uniPointExcelImportError.setErrorTime(new Date());
                    uniPointExcelImportError.setErrorType(1);
                    uniPointExcelImportError.setErrorReason("检查文件失败，请检查上传的模版是否有误");
                    uniPointExcelImportError.setErrorRow(-1);
                    this.save(uniPointExcelImportError);
                    excel.setImportStatus(2);//未通过
                    uniPointExcelService.updateById(excel);
                }
            }
            isCheckRunning.set(false);
        }
    }

    private void checkErr(UniPointExcelEntity excel) throws Exception {
        baseMapper.clearErrorInfo();
        String filePath = excel.getFilePath();
        File file = new File(fileConfig.getFileSavePath() + "/" + filePath);
        if (!file.exists()) {
            UniPointExcelImportErrorEntity uniPointExcelImportError = new UniPointExcelImportErrorEntity();
            uniPointExcelImportError.setErrorFileId(excel.getId());
            uniPointExcelImportError.setErrorTime(new Date());
            uniPointExcelImportError.setErrorType(1);
            uniPointExcelImportError.setErrorReason("文件不存在");
            uniPointExcelImportError.setErrorRow(-1);
            this.save(uniPointExcelImportError);

            excel.setImportStatus(2);//未通过
            uniPointExcelService.updateById(excel);
            return;
        }

       /* String patrolHostCode = uniPatrolSystemService.getVideoSysCode();
        if (StringUtils.isBlank(patrolHostCode)) {
            UniPointExcelImportErrorEntity uniPointExcelImportError = new UniPointExcelImportErrorEntity();
            uniPointExcelImportError.setErrorFileId(excel.getId());
            uniPointExcelImportError.setErrorTime(new Date());
            uniPointExcelImportError.setErrorType(1);
            uniPointExcelImportError.setErrorReason("请检查巡检系统管理，video视频系统有且只能有一个");
            uniPointExcelImportError.setErrorRow(-1);
            this.save(uniPointExcelImportError);

            excel.setImportStatus(2);//未通过
            uniPointExcelService.updateById(excel);
            return;
        }*/


        ExcelUtil<UniPointImportExcel> util = new ExcelUtil<>(UniPointImportExcel.class);
        List<UniPointImportExcel> points = util.importExcel(Files.newInputStream(Paths.get(fileConfig.getFileSavePath() + "/" + excel.getFilePath())));
        List<UniPointExcelImportErrorEntity> allerr = new ArrayList<>();

        if (points.size() > 0) {
            initCheckParams();
        }
        robotPosMap.clear();
        int allcount = 0;
        for (int i = 0; i < points.size(); i++) {
            UniPointImportExcel point = points.get(i);
            point.setRowNum(i + 2);
            point.setExcelId(excel.getId());
            if (StringUtils.isBlank(point.getPointName())
                    && StringUtils.isBlank(point.getAreaName())
                    && StringUtils.isBlank(point.getBayName())
                    && StringUtils.isBlank(point.getDeviceName())
                    && StringUtils.isBlank(point.getComponentName())) {
                continue;
            }
            allcount++;

            List<UniPointExcelImportErrorEntity> errors = checkPointErr(point);
            if (errors.isEmpty()) {
                continue;
            }

            List<String> collect = errors.stream().map(UniPointExcelImportErrorEntity::getErrorReason).collect(Collectors.toList());
            int n = 1;
            ListIterator<String> iterator = collect.listIterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                next = n + "、" + next;
                iterator.set(next);
                n++;
            }
            UniPointExcelImportErrorEntity checkError = createCheckError(point, StringUtils.join(collect, ";"));
            allerr.add(checkError);
        }
        if (allerr.size() > 0) {
            excel.setPointAllCount(allcount);
            excel.setImportStatus(2);//没通过
            this.saveBatch(allerr);
            uniPointExcelService.updateById(excel);
        } else {
            excel.setPointAllCount(allcount);
            excel.setImportStatus(3);//通过
            uniPointExcelService.updateById(excel);
        }
    }

    private void initCheckParams() {
        List<String> subCodeList = uniSubstationService.listAllSubCode();
        checkParams.setSubCodeList(subCodeList);

        List<String> deviceTypeValues = sysDictDataService.listDictValueByType("device_type");
        checkParams.setDeviceTypeValues(deviceTypeValues);

        List<String> meterTypeValues = sysDictDataService.listDictValueByType("meter_type");
        checkParams.setMeterTypeValues(meterTypeValues);

        List<String> appearanceTypeValues = sysDictDataService.listDictValueByType("appearance_type");
        checkParams.setAppearanceTypeValues(appearanceTypeValues);

        List<String> saveTypeListValues = sysDictDataService.listDictValueByType("save_type");
        checkParams.setSaveTypeListValues(saveTypeListValues);

        List<String> recognitionTypeListValues = sysDictDataService.listDictValueByType("recognition_type");
        checkParams.setRecognitionTypeListValues(recognitionTypeListValues);

        List<String> phaseValues = sysDictDataService.listDictValueByType("phase");
        checkParams.setPhaseValues(phaseValues);

        List<String> taskTypeValues = sysDictDataService.listDictValueByType("task_type");
        checkParams.setTaskTypeValues(taskTypeValues);

        List<String> taskSubTypeValues2 = sysDictDataService.listDictValueByType("task_sub_type_2");
        List<String> taskSubTypeValues3 = sysDictDataService.listDictValueByType("task_sub_type_3");
        List<String> taskSubTypeValues5 = sysDictDataService.listDictValueByType("task_sub_type_5");
        checkParams.setTaskSubTypeValues2(taskSubTypeValues2);
        checkParams.setTaskSubTypeValues3(taskSubTypeValues3);
        checkParams.setTaskSubTypeValues5(taskSubTypeValues5);


        List<String> levelValues = sysDictDataService.listDictValueByType("patrol_point_level");
        checkParams.setLevelValues(levelValues);

        List<String> pointAnalyseCategoryValues = sysDictDataService.listDictValueByType("point_analyse_category");
        checkParams.setPointAnalyseCategoryValues(pointAnalyseCategoryValues);

        List<String> pointAnalyseTypeValues = sysDictDataService.listDictValueByType("point_analyse_type");
        checkParams.setPointAnalyseTypeValues(pointAnalyseTypeValues);

        List<String> pointAnalyseTypePbValues = sysDictDataService.listDictValueByType("point_analyse_type_pb");
        checkParams.setPointAnalyseTypePbValues(pointAnalyseTypePbValues);

        List<String> qxsbTypeValues = sysDictDataService.listDictValueByType("qxsb_type");
        checkParams.setQxsbTypeValues(qxsbTypeValues);


        List<String> deviceMainType = sysDictDataService.listDictValueByType("device_main_type");
        checkParams.setDeviceMainTypeValues(deviceMainType);

    }

    private List<UniPointExcelImportErrorEntity> checkPointErr(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        errors.addAll(checkSubCode(point));
        errors.addAll(checkSysCode(point));
        errors.addAll(checkAreaName(point));
        errors.addAll(checkBayName(point));
        errors.addAll(checkDeviceName(point));
        errors.addAll(checkDeviceType(point));//int
        errors.addAll(checkComponentName(point));
        errors.addAll(checkPointName(point));
        errors.addAll(checkPointType(point));//int
        errors.addAll(checkMeterType(point));//int
        errors.addAll(checkAppearanceType(point));//int
        errors.addAll(checkSaveTypeList(point));//str
        errors.addAll(checkRecognitionTypeList(point));
        errors.addAll(checkLevel(point));
        errors.addAll(checkPhase(point));
        errors.addAll(checkPointDes(point));
        //errors.addAll(checkMapPos(point));
        errors.addAll(checkUpperValue(point));
        errors.addAll(checkLowerValue(point));
        errors.addAll(checkTaskType(point));
        errors.addAll(checkTaskSubType(point));
        errors.addAll(checkIsObj(point));
        errors.addAll(checkRobotRelation(point));
        errors.addAll(checkDevicePos(point));
        errors.addAll(checkPointAnalyseCategory(point));
        errors.addAll(checkPointAnalyseType(point));
        //errors.addAll(checkIsSet(point));
        //errors.addAll(checkMonitorTimes(point));
        //errors.addAll(checkIsOneKeyPoint(point));
        //errors.addAll(checkIsKeepWatchPoint(point));
        //errors.addAll(checkDeviceMainType(point));
        return errors;
    }

    /*private Collection<? extends UniPointExcelImportErrorEntity> checkDeviceMainType(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String deviceMainType = point.getDeviceMainType();
        if (StringUtils.isBlank(deviceMainType)) {
            return errors;
        }
        if (!CustomStringUtils.isPositiveInteger(deviceMainType)) {
            errors.add(createCheckError(point, "设备主类型应为数字"));
            return errors;
        }
        if (!checkParams.getDeviceMainTypeValues().contains(deviceMainType)) {
            errors.add(createCheckError(point, "设备主类型取值错误"));
            return errors;
        }
        return errors;
    }*/

    /*private Collection<? extends UniPointExcelImportErrorEntity> checkIsKeepWatchPoint(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String isKeepWatchPoint = point.getIsKeepWatchPoint();
        if (StringUtils.isBlank(isKeepWatchPoint)) {
            return errors;
        }
        if (!CustomStringUtils.isPositiveInteger(isKeepWatchPoint)) {
            errors.add(createCheckError(point, "是否守望点位取值为数字"));
            return errors;
        }
        int i = Integer.parseInt(isKeepWatchPoint);
        if (i < 0 || i > 1) {
            errors.add(createCheckError(point, "是否守望点位取值错误   0：否，1：是"));
        }
        return errors;
    }*/

    /*private Collection<? extends UniPointExcelImportErrorEntity> checkIsOneKeyPoint(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String isOneKeyPoint = point.getIsOneKeyPoint();
        if (StringUtils.isBlank(isOneKeyPoint)) {
            return errors;
        }
        if (!CustomStringUtils.isPositiveInteger(isOneKeyPoint)) {
            errors.add(createCheckError(point, "是否一键顺控取值应为数字"));
            return errors;
        }
        int i = Integer.parseInt(isOneKeyPoint);
        if (i < 0 || i > 1) {
            errors.add(createCheckError(point, "是否一键顺控取值错误   0：否，1：是"));
        }
        return errors;
    }*/

    /*private Collection<? extends UniPointExcelImportErrorEntity> checkMonitorTimes(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String monitorTimes = point.getMonitorTimes();
        if (StringUtils.isBlank(monitorTimes)) {
            return errors;
        }
        if (!CustomStringUtils.isPositiveInteger(monitorTimes)) {
            errors.add(createCheckError(point, "静默监视频次取值应为数字"));
            return errors;
        }
        int i = Integer.parseInt(monitorTimes);
        if (i < 0) {
            errors.add(createCheckError(point, "静默监视频次 应大于1分钟"));
        }
        return errors;
    }*/

    /*private Collection<? extends UniPointExcelImportErrorEntity> checkIsSet(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String isSet = point.getIsSet();
        if (StringUtils.isBlank(isSet)) {
            return errors;
        }
        if (!CustomStringUtils.isPositiveInteger(isSet)) {
            errors.add(createCheckError(point, "点位是否设置取值应为数字"));
            return errors;
        }
        int i = Integer.parseInt(isSet);
        if (i < 0 || i > 1) {
            errors.add(createCheckError(point, "点位是否设置取值错误   0：未设置，1：已设置"));
        }
        return errors;
    }*/

    private Collection<? extends UniPointExcelImportErrorEntity> checkSysCode(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String sysCode = point.getSysCode();
        if (StringUtils.isBlank(sysCode)) {
            sysCode = uniPatrolSystemService.getVideoSysCode();
            if (sysCode != null) {
                errors.add(createCheckError(point, "系统编码未配置，将默认使用视频系统编码：" + sysCode));
            }
        } else {
            List<UniPatrolSystemVO> uniPatrolSystemVOS = uniPatrolSystemService.listPatrolSysBySysCode(sysCode);
            if (uniPatrolSystemVOS.size() == 0) {
                errors.add(createCheckError(point, "系统编码不存在（必须整改！不能导入！）"));
                return errors;
            }
        }
        if (StringUtils.isBlank(sysCode)) {
            errors.add(createCheckError(point, "请检查系统编码（必须整改！不能导入！）"));
            return errors;
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkPointAnalyseType(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String analyseSubType = point.getPointAnalyseType();
        if (StringUtils.isBlank(analyseSubType) || especialWord(analyseSubType)) {
            errors.add(createCheckError(point, "智能分析子类为空或者有特殊字符"));
            return errors;
        }

        String[] split = analyseSubType.split(",");

        List<String> excelAnalyseTypeValues = new ArrayList<>();
        List<String> excelPointAnalyseTypePbValues = new ArrayList<>();
        List<String> excelQxsbTypeValues = new ArrayList<>();

        List<String> analyseTypeValues = checkParams.getPointAnalyseTypeValues();
        List<String> pointAnalyseTypePbValues = checkParams.getPointAnalyseTypePbValues();
        List<String> qxsbTypeValues = checkParams.getQxsbTypeValues();

        for (String aPointAnalyseType : split) {
            if (!analyseTypeValues.contains(aPointAnalyseType)
                    && !pointAnalyseTypePbValues.contains(aPointAnalyseType)
                    && !qxsbTypeValues.contains(aPointAnalyseType)) {
                errors.add(createCheckError(point, "智能分析子类取值错误"));
                return errors;
            }
            if (analyseTypeValues.contains(aPointAnalyseType)) {
                excelAnalyseTypeValues.add(aPointAnalyseType);
            }
            if (pointAnalyseTypePbValues.contains(aPointAnalyseType)) {
                excelPointAnalyseTypePbValues.add(aPointAnalyseType);
            }
            if (qxsbTypeValues.contains(aPointAnalyseType)) {
                excelQxsbTypeValues.add(aPointAnalyseType);
            }
        }
        String pointAnalyseCategory = point.getPointAnalyseCategory();
        if (StringUtils.isBlank(pointAnalyseCategory)) {
            if ("1".equals(pointAnalyseCategory) && excelAnalyseTypeValues.isEmpty()) {
                errors.add(createCheckError(point, "按照分析识别种类取值，智能分析子类取值错误"));
                return errors;
            }
            if ("2".equals(pointAnalyseCategory) && excelPointAnalyseTypePbValues.isEmpty()) {
                errors.add(createCheckError(point, "按照分析识别种类取值，智能分析子类取值错误"));
                return errors;
            }
            if ("3".equals(pointAnalyseCategory) && excelQxsbTypeValues.isEmpty()) {
                errors.add(createCheckError(point, "按照分析识别种类取值，智能分析子类取值错误"));
                return errors;
            }
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkPointAnalyseCategory(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String pointAnalyseCategory = point.getPointAnalyseCategory();
        if (StringUtils.isBlank(pointAnalyseCategory) || especialWord(pointAnalyseCategory)) {
            errors.add(createCheckError(point, "分析识别种类为空或者有特殊字符"));
            return errors;
        }
        if (!CustomStringUtils.isPositiveInteger(pointAnalyseCategory)) {
            errors.add(createCheckError(point, pointAnalyseCategory + " 分析识别种类应为数字"));
            return errors;
        }
        List<String> pointAnalyseCategoryValues = checkParams.getPointAnalyseCategoryValues();
        if (!pointAnalyseCategoryValues.contains(pointAnalyseCategory)) {
            errors.add(createCheckError(point, pointAnalyseCategory + " 分析识别种类范围错误"));
            return errors;
        }

        String recognitionTypeList = point.getRecognitionTypeList();
        if (StringUtils.isNotBlank(recognitionTypeList)) {
            if ("1".equals(recognitionTypeList)
                    || "4".equals(recognitionTypeList)
                    || "5".equals(recognitionTypeList)
                    || "6".equals(recognitionTypeList)) {
                if (!"1".equals(pointAnalyseCategory)) {
                    errors.add(createCheckError(point, "按识别类型取值判断，分析识别种类取值错误"));
                    return errors;
                }
            }
            if ("2".equals(recognitionTypeList)) {
                if (!"1".equals(pointAnalyseCategory) && !"3".equals(pointAnalyseCategory)) {
                    errors.add(createCheckError(point, "按识别类型取值判断，分析识别种类取值错误"));
                    return errors;
                }
            }
            if ("3".equals(recognitionTypeList)) {
                if (!"2".equals(pointAnalyseCategory) && !"3".equals(pointAnalyseCategory)) {
                    errors.add(createCheckError(point, "按识别类型取值判断，分析识别种类取值错误"));
                    return errors;
                }
            }
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkDevicePos(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String devicePos = point.getRobotPos();
        if (StringUtils.isBlank(devicePos) || especialWord(devicePos)) {
            errors.add(createCheckError(point, "预置位号为空或者有特殊字符"));
            return errors;
        }
        if (!CustomStringUtils.isPositiveInteger(devicePos)) {
            errors.add(createCheckError(point, "预置位号应为数字"));
            return errors;
        }
        String robotCode = point.getRobotCode();
        if (StringUtils.isBlank(robotCode)) {
            return errors;
        }
        Set<String> devicePosSet = robotPosMap.computeIfAbsent(robotCode, k -> new HashSet<>());
        if (devicePosSet.contains(devicePos)) {
            errors.add(createCheckError(point, "预置位号重复！必须修改"));
            return errors;
        } else {
            devicePosSet.add(devicePos);
            robotPosMap.put(robotCode, devicePosSet);
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkRobotRelation(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String pointType = point.getPointType();
        String robotCode = point.getRobotCode();
        if (StringUtils.isBlank(pointType) && !"robot".equals(pointType)) {
            errors.add(createCheckError(point, "点位类型错误 该系统仅支持 robot"));
            return errors;
        }
        String subCode = point.getSubCode();
        if (StringUtils.isBlank(robotCode)) {
            errors.add(createCheckError(point, "机器人编码不能为空"));
            return errors;
        }
        UniRobotEntity uniRobot = uniRobotService.getRobotByCode(subCode, robotCode);
        if (uniRobot == null) {
            errors.add(createCheckError(point, "机器人信息不存在"));
            return errors;
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkIsObj(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String isObj = point.getIsObj();
        if (StringUtils.isBlank(isObj) || especialWord(isObj)) {
            errors.add(createCheckError(point, "是否实物识别为空或者有特殊字符"));
            return errors;
        }
        if (!CustomStringUtils.isPositiveInteger(isObj)) {
            errors.add(createCheckError(point, "是否实物识别应为数字"));
            return errors;
        }
        if (!"1".equals(isObj) && !"0".equals(isObj)) {
            errors.add(createCheckError(point, "是否实物识别取值错误"));
            return errors;
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkTaskSubType(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String taskType = point.getTaskType();
        String taskSubType = point.getTaskSubType();
        if (StringUtils.isBlank(taskType)) {
            return errors;
        }

        List<String> taskTypelist = Arrays.asList(taskType.split(","));
        Boolean allowSubTypeNotNull = null; //subType 可以为空 = false  不可以为空 = true
        if (taskTypelist.contains("1") || taskTypelist.contains("4")) {
            allowSubTypeNotNull = false;  //允许为空
        }
        if (taskTypelist.contains("2") || taskTypelist.contains("3") || taskTypelist.contains("5")) {
            allowSubTypeNotNull = true;  //不允许为空
        }

        //都没有匹配上
        if (allowSubTypeNotNull == null) {
            return errors;
        }

        //subType可以为空 且 实际为空
        if (!allowSubTypeNotNull && StringUtils.isBlank(taskSubType)) {
            return errors;
        }

        //subType可以为空 但 实际为不空
        if (!allowSubTypeNotNull && StringUtils.isNotBlank(taskSubType)) {
            errors.add(createCheckError(point, "按当前任务类型，巡视任务子类型应为空"));
            return errors;
        }

        //subType 不可以为空  但实际为空
        if (allowSubTypeNotNull && StringUtils.isBlank(taskSubType)) {
            errors.add(createCheckError(point, "按当前任务类型，巡视任务子类型不能空"));
            return errors;
        }

        //subType 不可以为空  但 实际也不空  就验证特殊字符
        if (allowSubTypeNotNull && StringUtils.isNotBlank(taskSubType)) {
            if (especialWord(taskSubType)) {
                errors.add(createCheckError(point, "巡视任务子类型有特殊字符"));
                return errors;
            }
        }

        String[] typeList = taskSubType.split(",");

        List<String> excelTaskSubTypeValues2 = new ArrayList<>();
        List<String> excelTaskSubTypeValues3 = new ArrayList<>();
        List<String> excelTaskSubTypeValues5 = new ArrayList<>();

        List<String> taskSubTypeValues2 = checkParams.getTaskSubTypeValues2();
        List<String> taskSubTypeValues3 = checkParams.getTaskSubTypeValues3();
        List<String> taskSubTypeValues5 = checkParams.getTaskSubTypeValues5();

        for (String type : typeList) {
            if (!CustomStringUtils.isPositiveInteger(type)) {
                errors.add(createCheckError(point, "巡视任务子类型应为数字"));
                return errors;
            }
            if (!taskSubTypeValues2.contains(type)
                    && !taskSubTypeValues3.contains(type)
                    && !taskSubTypeValues5.contains(type)) {
                errors.add(createCheckError(point, "巡视任务子类型取值错误"));
                return errors;
            }
            if (taskSubTypeValues2.contains(type)) {
                excelTaskSubTypeValues2.add(type);
            }
            if (taskSubTypeValues3.contains(type)) {
                excelTaskSubTypeValues3.add(type);
            }
            if (taskSubTypeValues5.contains(type)) {
                excelTaskSubTypeValues5.add(type);
            }
        }


        if (StringUtils.isNotBlank(taskType)) {
            String[] taskTypeList = taskType.split(",");
            boolean hasType2 = false;
            boolean hasType3 = false;
            boolean hasType5 = false;
            for (String aTaskType : taskTypeList) {
                if ("2".equals(aTaskType)) hasType2 = true;
                if ("3".equals(aTaskType)) hasType3 = true;
                if ("5".equals(aTaskType)) hasType5 = true;
            }
            if (excelTaskSubTypeValues2.size() > 0 && !hasType2) {
                errors.add(createCheckError(point, "按当前巡视类型，不应该有特殊子类型"));
            }
            if (excelTaskSubTypeValues3.size() > 0 && !hasType3) {
                errors.add(createCheckError(point, "按当前巡视类型，不应该有专项巡视子类型"));
            }
            if (excelTaskSubTypeValues5.size() > 0 && !hasType5) {
                errors.add(createCheckError(point, "按当前巡视类型，不应该有熄灯巡视子类型"));
            }

            if (hasType2 && excelTaskSubTypeValues2.size() == 0) {
                errors.add(createCheckError(point, "缺少特殊巡视子类型"));
            }

            if (hasType3 && excelTaskSubTypeValues3.size() == 0) {
                errors.add(createCheckError(point, "缺少专项巡视子类型"));
            }

            if (hasType5 && excelTaskSubTypeValues5.size() == 0) {
                errors.add(createCheckError(point, "缺少熄灯巡视子类型"));
            }
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkTaskType(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String taskType = point.getTaskType();
        if (StringUtils.isBlank(taskType) || especialWord(taskType)) {
            errors.add(createCheckError(point, "巡视任务类型空或者有特殊字符"));
            return errors;
        }
        String[] typeList = taskType.split(",");
        for (String type : typeList) {
            if (!CustomStringUtils.isPositiveInteger(type)) {
                errors.add(createCheckError(point, taskType + " 巡视任务类型应为数字"));
                return errors;
            }
            List<String> taskTypeValues = checkParams.getTaskTypeValues();
            if (!taskTypeValues.contains(type)) {
                errors.add(createCheckError(point, taskType + " 巡视任务类型范围错误"));
                return errors;
            }
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkLowerValue(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String lowerValue = point.getLowerValue();
        if (StringUtils.isNotBlank(lowerValue)) {
            try {
                double v = Double.parseDouble(lowerValue);
            } catch (NumberFormatException e) {
                errors.add(createCheckError(point, lowerValue + " 点位正常范围下限取值有误"));
            }
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkUpperValue(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String upperValue = point.getUpperValue();
        if (StringUtils.isNotBlank(upperValue)) {
            try {
                double v = Double.parseDouble(upperValue);
            } catch (NumberFormatException e) {
                errors.add(createCheckError(point, upperValue + "点位正常范围上限取值有误"));
            }
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkMapPos(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String mapPos = point.getMapPos();
        if (StringUtils.isNotBlank(mapPos)) {
            String[] split = mapPos.split(",");
            if (split.length != 2) {
                errors.add(createCheckError(point, mapPos + " 坐标有误"));
            }
            String a = split[0];
            String b = split[0];
            try {
                double v = Double.parseDouble(a);
                double v1 = Double.parseDouble(b);
                if (v < 0 || v1 < 0) {
                    errors.add(createCheckError(point, mapPos + " 坐标取值有误"));
                }
            } catch (NumberFormatException e) {
                errors.add(createCheckError(point, mapPos + " 坐标取值有误"));
            }
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkPointDes(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String pointDes = point.getPointInfo();
        if (StringUtils.isNotBlank(pointDes) && especialWord(pointDes)) {
            errors.add(createCheckError(point, "点位描述有特殊字符"));
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkPhase(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String phase = point.getPhase();
        // todo 相位好像可以为空
        if (StringUtils.isBlank(phase)) {
            return errors;
        }
        if (especialWord(phase)) {
            errors.add(createCheckError(point, "相位有特殊字符"));
            return errors;
        }
        if (!CustomStringUtils.isPositiveInteger(phase)) {
            errors.add(createCheckError(point, phase + " 相位应为数字"));
        }
        List<String> phaseValues = checkParams.getPhaseValues();
        if (!phaseValues.contains(phase)) {
            errors.add(createCheckError(point, phase + " 相位范围错误"));
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkLevel(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String level = point.getLevel();
        if (StringUtils.isBlank(level) || especialWord(level)) {
            errors.add(createCheckError(point, "重要等级为空或者有特殊字符"));
            return errors;
        }
        if (!CustomStringUtils.isPositiveInteger(level)) {
            errors.add(createCheckError(point, level + " 重要等级应为数字"));
        }
        List<String> levelValues = checkParams.getLevelValues();
        if (!levelValues.contains(level)) {
            errors.add(createCheckError(point, level + " 重要等级范围错误"));
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkRecognitionTypeList(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String recognitionTypeList = point.getRecognitionTypeList();
        if (StringUtils.isBlank(recognitionTypeList) || especialWord(recognitionTypeList)) {
            errors.add(createCheckError(point, "识别类型为空或者有特殊字符"));
            return errors;
        }
        if (!CustomStringUtils.isPositiveInteger(recognitionTypeList)) {
            errors.add(createCheckError(point, recognitionTypeList + " 识别类型应为数字"));
        }
        List<String> recognitionTypeListValues = checkParams.getRecognitionTypeListValues();
        if (!recognitionTypeListValues.contains(recognitionTypeList)) {
            errors.add(createCheckError(point, recognitionTypeList + " 识别类型范围错误"));
        }
        return errors;

    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkSaveTypeList(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String saveTypeList = point.getSaveTypeList();
        if (StringUtils.isBlank(saveTypeList) || especialWord(saveTypeList)) {
            errors.add(createCheckError(point, "采集文件类型为空或者有特殊字符"));
            return errors;
        }
        String[] typeList = saveTypeList.split(",");
        for (String type : typeList) {
            if (!CustomStringUtils.isPositiveInteger(type)) {
                errors.add(createCheckError(point, saveTypeList + " 采集文件类型应为数字"));
                return errors;
            }
            List<String> saveTypeListValues = checkParams.getSaveTypeListValues();
            if (!saveTypeListValues.contains(type)) {
                errors.add(createCheckError(point, saveTypeList + " 采集文件类型范围错误"));
                return errors;
            }
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkAppearanceType(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String appearanceType = point.getAppearanceType();
        if (StringUtils.isBlank(appearanceType) || especialWord(appearanceType)) {
            errors.add(createCheckError(point, "外观类型为空或者有特殊字符"));
            return errors;
        }
        if (!CustomStringUtils.isPositiveInteger(appearanceType)) {
            errors.add(createCheckError(point, "外观类型应为数字"));
        }
        List<String> appearanceTypeValues = checkParams.getAppearanceTypeValues();
        if (!appearanceTypeValues.contains(appearanceType)) {
            errors.add(createCheckError(point, appearanceType + " 外观类型取值范围错误"));
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkMeterType(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String meterType = point.getMeterType();
        // todo 表计类型可以不填
        if (StringUtils.isBlank(meterType)) {
            return errors;
        }
        if (especialWord(meterType)) {
            errors.add(createCheckError(point, "表计类型有特殊字符"));
            return errors;
        }
        if (!CustomStringUtils.isPositiveInteger(meterType)) {
            errors.add(createCheckError(point, meterType + " 表计类型应为数字"));
        }
        List<String> meterTypeValues = checkParams.getMeterTypeValues();
        if (!meterTypeValues.contains(meterType)) {
            errors.add(createCheckError(point, meterType + " 表计类型取值范围错误"));
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkPointType(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String pointType = point.getPointType();
        if (StringUtils.isBlank(pointType) || especialWord(pointType)) {
            errors.add(createCheckError(point, "必须修改！ 点位类型为空或者有特殊字符"));
            return errors;
        }
        String[] pointTypeAry = pointType.split(",");
        for (String aPointType : pointTypeAry) {
            if (!"robot".equals(aPointType)) {
                errors.add(createCheckError(point, pointType + " 点位类型取值错误，必须修改！"));
                return errors;
            }
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkComponentName(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String componentName = point.getComponentName();
        if (StringUtils.isBlank(componentName) || especialWord(componentName)) {
            errors.add(createCheckError(point, "部件名称为空或者有特殊字符"));
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkDeviceType(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String deviceType = point.getDeviceType();
        if (StringUtils.isBlank(deviceType) || especialWord(deviceType)) {
            errors.add(createCheckError(point, "设备类型为空或者有特殊字符"));
            return errors;
        }
        if (!CustomStringUtils.isPositiveInteger(deviceType)) {
            errors.add(createCheckError(point, deviceType + " 设备类型应为数字"));
        }

        List<String> deviceTypeValues = checkParams.getDeviceTypeValues();
        if (!deviceTypeValues.contains(deviceType)) {
            errors.add(createCheckError(point, deviceType + " 设备类型取值范围错误"));
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkPointName(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String pointName = point.getPointName();
        if (StringUtils.isBlank(pointName) || especialWord(pointName)) {
            errors.add(createCheckError(point, "点位名称为空或者有特殊字符"));
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkDeviceName(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String deviceName = point.getDeviceName();
        if (StringUtils.isBlank(deviceName) || especialWord(deviceName)) {
            errors.add(createCheckError(point, "设备名称为空或者有特殊字符"));
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkBayName(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String bayName = point.getBayName();
        if (StringUtils.isBlank(bayName) || especialWord(bayName)) {
            errors.add(createCheckError(point, "间隔名称为空或者有特殊字符"));
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkAreaName(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String areaName = point.getAreaName();
        if (StringUtils.isBlank(areaName) || especialWord(areaName)) {
            errors.add(createCheckError(point, "区域名称为空或者有特殊字符"));
        }
        return errors;
    }

    private Collection<? extends UniPointExcelImportErrorEntity> checkSubCode(UniPointImportExcel point) {
        List<UniPointExcelImportErrorEntity> errors = new ArrayList<>();
        String subCode = point.getSubCode();
        if (StringUtils.isBlank(subCode) || especialWord(subCode)) {
            errors.add(createCheckError(point, "变电站编码为空或者有特殊字符"));
            return errors;
        }
        List<String> subCodeList = checkParams.getSubCodeList();
        if (!subCodeList.contains(subCode)) {
            errors.add(createCheckError(point, subCode + " 变电站编码错误"));
        }
        return errors;
    }

    private boolean especialWord(String str) {
        return zhibiao.matcher(str).find() || specialp.matcher(str).find();
    }

    private UniPointExcelImportErrorEntity createCheckError(UniPointImportExcel pointExcel, String reason) {
        UniPointExcelImportErrorEntity uniPointExcelImportError = new UniPointExcelImportErrorEntity();
        uniPointExcelImportError.setErrorFileId(pointExcel.getExcelId());
        uniPointExcelImportError.setErrorTime(new Date());
        uniPointExcelImportError.setErrorType(1);
        uniPointExcelImportError.setErrorReason(reason);
        uniPointExcelImportError.setErrorRow(pointExcel.getRowNum());
        return uniPointExcelImportError;
    }


}
