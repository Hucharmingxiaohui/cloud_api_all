package com.df.server.service.uni.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.framework.utils.ConvertUtils;
import com.df.framework.utils.DateUtils;
import com.df.server.dto.UniPointThreshold.UniPointThresholdAddDTO;
import com.df.server.dto.uniPoint.PointDetailDTO;
import com.df.server.entity.his.HisUniTaskItemPointsEntity;
import com.df.server.entity.uni.UniPointEntity;
import com.df.server.entity.uni.UniPointThresholdEntity;
import com.df.server.mapper.uni.UniPointMapper;
import com.df.server.mapper.uni.UniPointThresholdMapper;
import com.df.server.service.his.HisUniTaskItemPointsService;
import com.df.server.service.uni.UniPointThresholdService;
import com.df.server.vo.UniPointThreshold.PointAlarmThresholdVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service("uniPointThresholdService")
public class UniPointThresholdServiceImpl extends ServiceImpl<UniPointThresholdMapper, UniPointThresholdEntity> implements UniPointThresholdService {

    @Autowired
    private UniPointMapper uniPointMapper;
    @Autowired
    private HisUniTaskItemPointsService hisUniTaskItemPointsService;

    @Override
    public PointAlarmThresholdVO getPointRelAlarmThresholdInfo(PointDetailDTO dto) {
        String subCode = dto.getSubCode();
        String pointCode = dto.getPointCode();
        PointAlarmThresholdVO pointAlarmThreshold = new PointAlarmThresholdVO();
        //获取点位信息
        UniPointEntity uniPointEntity = uniPointMapper.getPointEntityByCode(subCode, pointCode);
        if (Objects.isNull(uniPointEntity)) {
            return pointAlarmThreshold;
        }
        //根据点位编码获取最新的巡视任务/巡视结果等信息
        HisUniTaskItemPointsEntity hisUniTaskItemPoints =
                hisUniTaskItemPointsService.getLastItems(subCode, pointCode);
        if (Objects.nonNull(hisUniTaskItemPoints)) {
            pointAlarmThreshold.setPointName(hisUniTaskItemPoints.getPointName());
            pointAlarmThreshold.setTaskName(hisUniTaskItemPoints.getTaskName());
            pointAlarmThreshold.setTaskTime(DateUtils.parseDateToStr(hisUniTaskItemPoints.getRunTime()));
            pointAlarmThreshold.setTaskResult(hisUniTaskItemPoints.getPointVal());
            pointAlarmThreshold.setResultDes(hisUniTaskItemPoints.getPointValUnit());
        }
        //根据点位编码获取配置的阈值
        UniPointThresholdEntity uniPointThreshold = this.lambdaQuery().eq(UniPointThresholdEntity::getSubCode, subCode)
                .eq(UniPointThresholdEntity::getPointCode, pointCode).oneOpt().orElse(null);
        if (Objects.nonNull(uniPointThreshold)) {
            BeanUtils.copyProperties(uniPointThreshold, pointAlarmThreshold);
        }
        return pointAlarmThreshold;
    }

    @Override
    public void modifyThreshold(UniPointThresholdAddDTO addDTO) {
        String subCode = addDTO.getSubCode();
        String pointCode = addDTO.getPointCode();
        this.remove(new QueryWrapper<UniPointThresholdEntity>().eq("sub_code", subCode).eq("point_code", pointCode));
        UniPointThresholdEntity bean = ConvertUtils.toBean(addDTO, UniPointThresholdEntity.class);
        this.save(bean);
    }

    @Override
    public void delete(PointDetailDTO pointDetailDTO) {
        this.lambdaUpdate().eq(UniPointThresholdEntity::getSubCode, pointDetailDTO.getSubCode()).eq(UniPointThresholdEntity::getPointCode, pointDetailDTO.getPointCode()).remove();
    }

    @Override
    public Integer isAlarmByThreshold(UniPointEntity uniPoint, String pointVal) {
        Integer alarmLevel = null;
        //查询阈值
        UniPointThresholdEntity uniPointThreshold =
                this.lambdaQuery().eq(UniPointThresholdEntity::getPointCode, uniPoint.getPointCode()).oneOpt().orElse(null);
        if (uniPointThreshold != null) {
            alarmLevel = checkIsAlarmByThreshold(uniPointThreshold, pointVal);
        }

        //如果是缺陷识别类，只要value=1即应报警，不需要配阈值
        //如果已配阈值且判断有告警，就按照配置了的级别报警，如果没有配就固定按照严重级别报警
        Integer point_analyse_category = uniPoint.getPointAnalyseCategory();
        if (alarmLevel == null && point_analyse_category != null && point_analyse_category == 2 && "1".equals(pointVal)) {
            alarmLevel = 3;
        }
        return alarmLevel;
    }

    @Override
    public String getPointThresholdDes(String pointCode) {
        StringBuffer result = new StringBuffer();
        UniPointThresholdEntity threshold = this.lambdaQuery()
                .eq(UniPointThresholdEntity::getPointCode, pointCode).oneOpt().orElse(null);
        if (Objects.isNull(threshold)) {
            return "";
        }
        String warning_upper_limit = threshold.getWarningUpperLimit();
        String common_upper_limit = threshold.getCommonUpperLimit();
        String serious_upper_limit = threshold.getSeriousUpperLimit();
        String critical_upper_limit = threshold.getCriticalUpperLimit();
        String warning_lower_limit = threshold.getWarningLowerLimit();
        String common_lower_limit = threshold.getCommonLowerLimit();
        String serious_lower_limit = threshold.getSeriousLowerLimit();
        String critical_lower_limit = threshold.getCriticalLowerLimit();

        String warningState = threshold.getWarningState();
        String commonState = threshold.getCommonState();
        String seriousState = threshold.getSeriousState();
        String criticalState = threshold.getCriticalState();

        Integer decision_rules = threshold.getDecisionRules();
        if (Objects.isNull(decision_rules)) {
            return null;
        }
        //1上限
        if (decision_rules == 1) {
            upperLimitDes(result, warning_upper_limit, common_upper_limit, serious_upper_limit, critical_upper_limit);
        }
        //2下限
        if (decision_rules == 2) {
            lowerLimitDes(result, warning_lower_limit, common_lower_limit, serious_lower_limit, critical_lower_limit);
        }
        //3区间
        if (decision_rules == 3) {
            upperLimitDes(result, warning_upper_limit, common_upper_limit, serious_upper_limit, critical_upper_limit);
            result.append(",");
            lowerLimitDes(result, warning_lower_limit, common_lower_limit, serious_lower_limit, critical_lower_limit);
        }
        //4状态
        if (decision_rules == 4) {
            stateLimitDes(result, warningState, commonState, seriousState, criticalState);
        }
        return result.toString().replace(",]", "]");
    }

    private Integer checkIsAlarmByThreshold(UniPointThresholdEntity threshold, String value) {
        //如果传过来的值是空直接返回null
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        //因为结果和阈值配置都可能会逗号分割，故拆分逗号位置对应去比对
        //例如value="31,1" 阈值配置"50,0"，需要对应第一个位置，第二个位置都去比对判断
        String[] valueArr = splitLimit(value);
        if (valueArr == null || valueArr.length == 0) {
            return null;
        }
        Integer alarmLevel = null;
        for (int i = 0; i < valueArr.length; i++) {
            String valueTemp = valueArr[i];
            alarmLevel = getAlarmLevelByIndex(threshold, valueTemp, i);
            //只要匹配到某个位置报警级别就返回
            if (alarmLevel != null) {
                log.info("【告警提示】匹配到结果的第[{}]位数超过阈值告警，点位编码：{},", (i + 1), threshold.getPointCode());
                break;
            }
        }
        return alarmLevel;
    }

    /**
     * 逗号分割值统一处理方法
     *
     * @param limit
     * @return
     */
    private String[] splitLimit(String limit) {
        String[] split = null;
        if (StringUtils.isNotEmpty(limit)) {
            split = limit.split(",");
        }
        return split;
    }

    /**
     * 阈值逗号分割值统一处理方法
     *
     * @param limit
     * @param i
     * @return 返回Double类型的值
     */
    private Double splitLimitToDouble(String limit, Integer i) {
        Double result = null;
        try {
            if (StringUtils.isNotEmpty(limit)) {
                String s = limit.split(",")[i];
                result = ConvertUtils.toDouble(s);
            }
        } catch (Exception e) {
            //数组下标越界的话直接返回null;
            return null;
        }
        return result;
    }

    /**
     * 阈值逗号分割值统一处理方法
     *
     * @param limit
     * @param i
     * @return 返回String类型的值
     */
    private String splitLimitToString(String limit, Integer i) {
        String result = null;
        try {
            if (StringUtils.isNotEmpty(limit)) {
                result = limit.split(",")[i];
            }
        } catch (Exception e) {
            //数组下标越界的话直接返回null;
            return null;
        }
        return result;
    }

    /**
     * 比对两个值（识别值和阈值）根据判定规则来判断告警级别
     *
     * @param threshold
     * @param value
     * @return 返回告警级别
     */
    private Integer getAlarmLevelByIndex(UniPointThresholdEntity threshold, String value, Integer i) {
        Integer alarmLevel = null;
        Integer decision_rules = threshold.getDecisionRules();

        Double critical_upper_limit = splitLimitToDouble(threshold.getCriticalUpperLimit(), i);
        Double serious_upper_limit = splitLimitToDouble(threshold.getSeriousUpperLimit(), i);
        Double common_upper_limit = splitLimitToDouble(threshold.getCommonUpperLimit(), i);
        Double warning_upper_limit = splitLimitToDouble(threshold.getWarningUpperLimit(), i);

        Double critical_lower_limit = splitLimitToDouble(threshold.getCriticalLowerLimit(), i);
        Double serious_lower_limit = splitLimitToDouble(threshold.getSeriousLowerLimit(), i);
        Double common_lower_limit = splitLimitToDouble(threshold.getCommonLowerLimit(), i);
        Double warning_lower_limit = splitLimitToDouble(threshold.getWarningLowerLimit(), i);

        String critical_state = splitLimitToString(threshold.getCriticalState(), i);
        String serious_state = splitLimitToString(threshold.getSeriousState(), i);
        String common_state = splitLimitToString(threshold.getCommonState(), i);
        String warning_state = splitLimitToString(threshold.getWarningState(), i);


        // 判定规则1,比对[上限]
        if (decision_rules == 1) {
            Double val = ConvertUtils.toDouble(value);
            if (val != null) {
                if (critical_upper_limit != null && val > critical_upper_limit) {
                    //值大于危急上限，设置告警级别 4危急
                    alarmLevel = 4;
                } else if (serious_upper_limit != null && val > serious_upper_limit) {
                    //值大于严重上限，设置告警级别 3严重
                    alarmLevel = 3;
                } else if (common_upper_limit != null && val > common_upper_limit) {
                    //值大于一般上限，设置告警级别 2一般
                    alarmLevel = 2;
                } else if (warning_upper_limit != null && val > warning_upper_limit) {
                    //值大于预警上限，设置告警级别 1预警
                    alarmLevel = 1;
                }
            }
        }

        //判定规则2,比对[下限]
        if (decision_rules == 2) {
            Double val = ConvertUtils.toDouble(value);
            if (val != null) {
                if (critical_lower_limit != null && val < critical_lower_limit) {
                    //值小于危急下限，设置告警级别 4危急
                    alarmLevel = 4;
                } else if (serious_lower_limit != null && val < serious_lower_limit) {
                    //值小于严重下限，设置告警级别 3严重
                    alarmLevel = 3;
                } else if (common_lower_limit != null && val < common_lower_limit) {
                    //值小于一般下限，设置告警级别 2一般
                    alarmLevel = 2;
                } else if (warning_lower_limit != null && val < warning_lower_limit) {
                    //值小于预警下限，设置告警级别 1预警
                    alarmLevel = 1;
                }
            }
        }

        //判定规则3,比对[区间]
        if (decision_rules == 3) {
            Double val = ConvertUtils.toDouble(value);
            if (val != null) {
                // 区间
                if ((critical_lower_limit != null && val < critical_lower_limit)
                        || (critical_upper_limit != null && val > critical_upper_limit)) {
                    //值小于危急下限，或者值大于危急上限，设置告警级别，4危急
                    alarmLevel = 4;
                } else if ((serious_lower_limit != null && val < serious_lower_limit)
                        || (serious_upper_limit != null && val > serious_upper_limit)) {
                    //值小于严重下限，或者值大于严重上限，设置告警级别，3严重
                    alarmLevel = 3;
                } else if ((common_lower_limit != null && val < common_lower_limit)
                        || (common_upper_limit != null && val > common_upper_limit)) {
                    //值小于一般下限，或者值大于一般上限，设置告警级别，2一般
                    alarmLevel = 2;
                } else if ((warning_lower_limit != null && val < warning_lower_limit)
                        || (warning_upper_limit != null && val > warning_upper_limit)) {
                    //值小于预警下限，或者值大于预警上限，设置告警级别，1预警
                    alarmLevel = 1;
                }
            }
        }

        //判定规则4,比对[状态，即字符串匹配]
        if (decision_rules == 4) {
            String val = value.trim();

            if (StringUtils.isNotEmpty(critical_state) && !val.equals(critical_state.trim())) {
                //值大于危急上限，设置告警级别 4危急
                alarmLevel = 4;
            } else if (StringUtils.isNotEmpty(serious_state) && val.equals(serious_state.trim())) {
                //值大于严重上限，设置告警级别 3严重
                alarmLevel = 3;
            } else if (StringUtils.isNotEmpty(common_state) && val.equals(common_state.trim())) {
                //值大于一般上限，设置告警级别 2一般
                alarmLevel = 2;
            } else if (StringUtils.isNotEmpty(warning_state) && val.contains(warning_state)) {
                //值大于预警上限，设置告警级别 1预警
                alarmLevel = 1;
            }
        }
        return alarmLevel;
    }

    private void stateLimitDes(StringBuffer result, String warningState, String commonState, String seriousState, String criticalState) {
        result.append("状态[");

        if (StringUtils.isNotEmpty(criticalState)) {
            result.append("危急：").append(criticalState);
            result.append(",");
        }
        if (StringUtils.isNotEmpty(seriousState)) {
            result.append("严重：").append(seriousState);
            result.append(",");
        }
        if (StringUtils.isNotEmpty(commonState)) {
            result.append("一般：").append(commonState);
            result.append(",");
        }
        if (StringUtils.isNotEmpty(warningState)) {
            result.append("告警：").append(warningState);
            result.append(",");
        }
        result.append("]");
    }

    private void lowerLimitDes(StringBuffer result, String warning_lower_limit, String common_lower_limit, String serious_lower_limit, String critical_lower_limit) {
        result.append("下限[");
        if (StringUtils.isNotEmpty(critical_lower_limit)) {
            result.append("危急：").append(critical_lower_limit);
            result.append(",");
        }
        if (StringUtils.isNotEmpty(serious_lower_limit)) {
            result.append("严重：").append(serious_lower_limit);
            result.append(",");
        }
        if (StringUtils.isNotEmpty(common_lower_limit)) {
            result.append("一般：").append(common_lower_limit);
            result.append(",");
        }
        if (StringUtils.isNotEmpty(warning_lower_limit)) {
            result.append("预警：").append(warning_lower_limit);
            result.append(",");
        }
        result.append("]");
        result.toString().replace(",]", "]");
    }

    private void upperLimitDes(StringBuffer result, String warning_upper_limit, String common_upper_limit, String serious_upper_limit, String critical_upper_limit) {
        result.append("上限[");
        if (StringUtils.isNotEmpty(critical_upper_limit)) {
            result.append("危急：").append(critical_upper_limit);
            result.append(",");
        }
        if (StringUtils.isNotEmpty(serious_upper_limit)) {
            result.append("严重：").append(serious_upper_limit);
            result.append(",");
        }
        if (StringUtils.isNotEmpty(common_upper_limit)) {
            result.append("一般：").append(common_upper_limit);
            result.append(",");
        }
        if (StringUtils.isNotEmpty(warning_upper_limit)) {
            result.append("预警：").append(warning_upper_limit);
            result.append(",");
        }
        result.append("]");
        result.toString().replace(",]", "]");
    }

}
