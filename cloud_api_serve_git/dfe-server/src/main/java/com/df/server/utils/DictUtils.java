package com.df.server.utils;

import com.df.server.entity.sys.SysDictDataEntity;
import com.df.server.service.sys.SysDictDataService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 字典工具类
 *
 * @author ruoyi
 */
@Component
public class DictUtils {

    private static SysDictDataService sysDictDataService;


    /**
     * 分隔符
     */
    private static final String SEPARATOR = ",";


    @Autowired
    public void setComponent(SysDictDataService mapper) {
        DictUtils.sysDictDataService = mapper;
    }


    /**
     * 获取字典缓存
     *
     * @param dictType 参数键
     * @return dictDatas 字典数据列表
     */
    public static List<SysDictDataEntity> getDictCache(String dictType) {
        List<SysDictDataEntity> list = sysDictDataService.listDictEntityByDiceType(dictType);
        return list;
    }


    private static <T> List<T> castList(Object obj, Class<T> clazz) {
        List<T> result = new ArrayList<T>();
        if (obj instanceof List<?>) {
            for (Object o : (List<?>) obj) {
                result.add(clazz.cast(o));
            }
            return result;
        }
        return null;
    }

    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue) {
        if (dictValue == null || "".equals(dictValue)) {
            return "";
        }
        return getDictLabel(dictType, dictValue, SEPARATOR);
    }

    public static String getDictLabel(String dictType, Integer dictValue) {
        if (dictValue == null) {
            return "";
        }
        return getDictLabel(dictType, String.valueOf(dictValue.intValue()));
    }


    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     * @param separator 分隔符
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue, String separator) {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictDataEntity> datas = getDictCache(dictType);

        if (StringUtils.containsAny(separator, dictValue) && datas != null) {
            for (SysDictDataEntity dict : datas) {
                for (String value : dictValue.split(separator)) {
                    if (value.equals(dict.getDictValue())) {
                        propertyString.append(dict.getDictLabel() + separator);
                        break;
                    }
                }
            }
        } else {
            for (SysDictDataEntity dict : datas) {
                if (dictValue.equals(dict.getDictValue())) {
                    return dict.getDictLabel();
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     *
     * @param dictType  字典类型
     * @param dictLabel 字典标签
     * @param separator 分隔符
     * @return 字典值
     */
    public static String getDictValue(String dictType, String dictLabel, String separator) {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictDataEntity> datas = getDictCache(dictType);

        if (StringUtils.containsAny(separator, dictLabel) && datas != null) {
            for (SysDictDataEntity dict : datas) {
                for (String label : dictLabel.split(separator)) {
                    if (label.equals(dict.getDictLabel())) {
                        propertyString.append(dict.getDictValue() + separator);
                        break;
                    }
                }
            }
        } else {
            for (SysDictDataEntity dict : datas) {
                if (dictLabel.equals(dict.getDictLabel())) {
                    return dict.getDictValue();
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }


    /**
     * 根据字典类型和字典标签获取字典值
     *
     * @param dictType  字典类型
     * @param dictLabel 字典标签
     * @return 字典值
     */
    /*public static String getDictValue(String dictType, String dictLabel) {
        return getDictValue(dictType, dictLabel, SEPARATOR);
    }*/
}
