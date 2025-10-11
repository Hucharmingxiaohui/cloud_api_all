package com.df.framework.utils;


import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 点位识别类型 对应关系类
 */
public class RecognitionTypeUtils {


    /**
     * 根据识别类型 获取对应的 主任务类型
     *
     * @param regTypeAry
     * @return
     */
    public static String getTaskTypeByRegTypeAry(String regTypeAry) {
        String result = "";
        //格式化
        regTypeAry = CustomStringUtils.replaceAllBlank(regTypeAry);
        if (CustomStringUtils.isBlank(regTypeAry)) {
            return result;
        }
        try {
            String[] regTypeArylist = regTypeAry.split(",");

            List<String> taskTypeList = new ArrayList<>();

            for (String s : regTypeArylist) {
                String s1 = formatTaskTypeByOneRegType(s);
                if (CustomStringUtils.isBlank(s1)) {
                    continue;
                }
                taskTypeList.add(s1);
            }
            if (taskTypeList.size() == 0) {
                return result;
            }
            if (taskTypeList.size() == 1) {
                return taskTypeList.get(0);
            }
            String join = CustomStringUtils.join(taskTypeList, ",");
            //去重 排序
            List<String> allTaskType = Arrays.asList(join.split(","));
            List<String> collect = allTaskType.stream().distinct().sorted().collect(Collectors.toList());
            if (collect.size() == 1) {
                return collect.get(0);
            }
            result = CustomStringUtils.join(collect, ",");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * 根据识别类型 获取对应的 分析识别种类
     * 表计1        ——  设备状态类识别  1
     * 位置状态识别2  ——  设备状态类识别  1
     * 设备外观3     ——   缺陷类识别    2
     * 红外测温4     ——  设备状态类识别  1
     * 声纹5        ——   设备状态类识别  1
     *
     * @param regType
     * @return
     */
    public static Integer getCategoryByRegTypeAry(String regType) {
        regType = CustomStringUtils.replaceAllBlank(regType);
        if (CustomStringUtils.isBlank(regType)) {
            return null;
        }
        String atype = regType.split(",")[0];
        if (CustomStringUtils.isBlank(atype) || !NumberUtils.isDigits(atype)) {
            return null;
        }
        int regTypeValue = Integer.parseInt(atype);
        if (regTypeValue > 0 && regTypeValue < 6) {
            if (regTypeValue == 3) {
                return 2;
            } else {
                return 1;
            }
        }
        return null;
    }


    /**
     * 识别类型 转 智能分析子类
     * 表计1        ——  仪表读数  meter
     * 位置状态识别2  ——  ""
     * 设备外观3     ——  18类
     * 红外测温4     ——  红外温度 infrared
     * 声纹5        ——  声音   sound
     *
     * @param regType
     * @return
     */
    public static String getAnalyseTypeByRegTypeAry(String regType) {
        regType = CustomStringUtils.replaceAllBlank(regType);
        if (CustomStringUtils.isBlank(regType)) {
            return "";
        }
        String atype = regType.split(",")[0];
        if (CustomStringUtils.isBlank(atype) || !NumberUtils.isDigits(atype)) {
            return "";
        }
        if ("1".equals(atype)) {
            return "meter";
        }
        if ("2".equals(atype)) {
            return "switch";
        }
        if ("3".equals(atype)) {
            return "bj_bpmh," +
                    "bj_bpps," +
                    "bj_wkps," +
                    "jyz_pl," +
                    "sly_bjbmyw," +
                    "sly_dmyw," +
                    "hxq_gjtps," +
                    "xmbhyc," +
                    "yw_gkxfw," +
                    "yw_nc," +
                    "gbps," +
                    "wcaqm," +
                    "wcgz," +
                    "xy," +
                    "bjdsyc," +
                    "ywzt_yfyc," +
                    "hxq_gjbs," +
                    "kgg_ybh";
        }
        if ("4".equals(atype)) {
            return "infrared";
        }
        if ("5".equals(atype)) {
            return "sound";
        }
        return "";
    }


    /**
     * a.  表计1、红外测温4 位置状态识别2 匹配：专项3 + 特殊2
     * b.  设备外观3、声纹5、在线监测/局放类11~13 101~104 匹配：例行1 + 特殊2
     * c.  其他类型 6闪烁 匹配：特殊2
     */
    private static String formatTaskTypeByOneRegType(String regType) {
        if (!NumberUtils.isDigits(regType)) {
            return "";
        }
        if ("1".equals(regType) || "2".equals(regType) || "4".equals(regType)) {
            //a.
            return "2,3";
        } else {
            if ("6".equals(regType)) {
                //c.
                return "2";
            } else {
                // b.
                return "1,2";
            }
        }
    }
}
