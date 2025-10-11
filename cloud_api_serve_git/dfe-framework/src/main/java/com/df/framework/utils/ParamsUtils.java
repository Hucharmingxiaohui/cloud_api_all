package com.df.framework.utils;

import com.df.framework.exception.FastException;
import org.apache.commons.lang3.StringUtils;

/**
 * 校验工具类
 * <p>
 * Created by lyc on 2020/3/18
 */
public class ParamsUtils {

    /**
     * 参数字段不能为空
     *
     * @param object
     * @param fields
     */
    public static void isNull(Object object, String... fields) {
        for (String field : fields) {
            Object valueByKey = ReflexObjectUtils.getValueByKey(object, field);
            if (valueByKey == null) {
                throw new FastException("参数字段" + field + "必传");
            }
        }
    }

    /**
     * 参数字段不能为空字符串
     *
     * @param object
     * @param fields
     */
    public static void isBlank(Object object, String... fields) {
        for (String field : fields) {
            Object valueByKey = ReflexObjectUtils.getValueByKey(object, field);
            if (valueByKey == null) {
                throw new FastException("参数字段" + field + "必传");
            }
            if (StringUtils.isBlank(valueByKey.toString())) {
                throw new FastException("参数字段" + field + "不能为空");
            }
        }
    }
}