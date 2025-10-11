package com.df.server.service.sys.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.framework.utils.ConvertUtils;
import com.df.framework.utils.PageUtils;
import com.df.framework.vo.PageVO;
import com.df.server.entity.sys.SysDictDataEntity;
import com.df.server.mapper.sys.SysDictDataMapper;
import com.df.server.service.sys.SysDictDataService;
import com.df.server.vo.SysDictData.SysDictDataVO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service("sysDictDataService")
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictDataEntity> implements SysDictDataService {

    /**
     * 根据字典类型和字典键值查询Label
     *
     * @param dictType
     * @param dictValue
     * @return
     */
    @Override
    public String getLabel(String dictType, String dictValue) {
        QueryWrapper<SysDictDataEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("dict_type", dictType).eq("dict_value", dictValue);
        SysDictDataEntity sysDictData = baseMapper.selectOne(wrapper);
        if (null != sysDictData) {
            return sysDictData.getDictLabel();
        }
        return "";
    }

    /**
     * 根据字典类型和字典键值查询字典对象
     *
     * @param dictType
     * @param dictValue
     * @return
     */
    @Override
    public SysDictDataEntity getDictEntity(String dictType, String dictValue) {
        QueryWrapper<SysDictDataEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("dict_type", dictType).eq("dict_value", dictValue);
        SysDictDataEntity sysDictData = baseMapper.selectOne(wrapper);
        return sysDictData;
    }


    @Override
    public Set<String> formatSilentPlanAnalyseTypeToAnalyse(String silentAnaLyseTypeDictDataSorts) {
        Set<String> result = new HashSet<>();
        List<String> sortValues = Arrays.asList(silentAnaLyseTypeDictDataSorts.split(","));
        for (String dictSort : sortValues) {
            SysDictDataEntity dictData =
                    baseMapper.selectByDictTypeAndSort(
                            Integer.valueOf(dictSort),
                            "silent_analyse_type");
            String dictValue = dictData.getDictValue();
            result.add(dictValue);
            //人员聚集徘徊，需要多发一个类型
            if (4 == dictData.getDictSort()) {
                result.add("ryph");
            }
            //渗漏油，需要多发一个类型
            if (202 == dictData.getDictSort()) {
                result.add("sly_dmyw");
            }
        }
        return result;
    }

    @Override
    public List<SysDictDataEntity> listDictEntityByDiceType(String dictType) {
        return baseMapper.selectDictDataByType(dictType);
    }

    @Override
    public List<String> listDictValueByType(String deviceType) {
        return this.lambdaQuery()
                .eq(SysDictDataEntity::getDictType, deviceType)
                .list()
                .stream()
                .map(SysDictDataEntity::getDictValue)
                .collect(Collectors.toList());
    }
}
