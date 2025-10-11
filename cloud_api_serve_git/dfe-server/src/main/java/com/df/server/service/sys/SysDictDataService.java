package com.df.server.service.sys;


import com.baomidou.mybatisplus.extension.service.IService;
import com.df.framework.vo.PageVO;
import com.df.server.entity.sys.SysDictDataEntity;
import com.df.server.vo.SysDictData.SysDictDataVO;

import java.util.List;
import java.util.Set;

/**
 * 字典数据接口
 * <p>
 * Created by lianyc on 2023-11-02
 */
public interface SysDictDataService extends IService<SysDictDataEntity> {

    /**
     * 根据字典类型和字典键值查询Label
     *
     * @param dictType
     * @param dictValue
     * @return
     */
    String getLabel(String dictType, String dictValue);

    /**
     * 根据字典类型和字典键值查询字典对象
     *
     * @param dictType
     * @param dictValue
     * @return
     */
    SysDictDataEntity getDictEntity(String dictType, String dictValue);


    /**
     * 将静默任务的AnalyseType字段值  转化成 智能分析主机的分析字典值
     *
     * @param silentAnaLyseTypeDictDataSorts
     * @return
     */
    Set<String> formatSilentPlanAnalyseTypeToAnalyse(String silentAnaLyseTypeDictDataSorts);


    List<SysDictDataEntity> listDictEntityByDiceType(String dictType);

    /**
     * 根据类型查询字典值
     *
     * @param deviceType
     * @return
     */
    List<String> listDictValueByType(String deviceType);
}

