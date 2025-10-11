package com.df.server.mapper.sys;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.entity.sys.SysDictDataEntity;
import com.df.server.vo.SysDictData.SysDictDataVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 字典数据接口
 * <p>
 * Created by lianyc on 2023-11-02
 */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictDataEntity> {


    @Select("select * from sys_dict_data where dict_type = #{dictType,jdbcType=VARCHAR} order by dict_sort")
    List<SysDictDataEntity> selectDictDataByType(String dictType);


    /**
     * 根据排序号和类型查询
     *
     * @param dictSort
     * @param dictType
     * @return
     */
    SysDictDataEntity selectByDictTypeAndSort(@Param("dictSort") int dictSort,
                                              @Param("dictType") String dictType);
}