package com.df.server.mapper.his;


import com.df.server.entity.his.HisLogBusinessEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 业务日志记录接口
 * <p>
 * Created by lianyc on 2023-11-02
 */
@Mapper
public interface HisLogBusinessMapper {

    /**
     * 新增操作日志
     *
     * @param hisLogBusinessEntity 日志输入对象
     */
    void insertHisLogBusiness(@Param("hisLogBusinessEntity") HisLogBusinessEntity hisLogBusinessEntity);

}