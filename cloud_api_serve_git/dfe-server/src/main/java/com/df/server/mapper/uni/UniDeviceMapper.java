package com.df.server.mapper.uni;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.entity.uni.UniDeviceEntity;
import com.df.server.vo.UniDevice.UniDeviceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 主设备接口
 * <p>
 * Created by lianyc on 2023-11-09
 */
@Mapper
public interface UniDeviceMapper extends BaseMapper<UniDeviceEntity> {

    UniDeviceEntity getByName(@Param("subCode") String subCode,
                              @Param("bayId") String bayId,
                              @Param("deviceName") String deviceName);

    UniDeviceEntity getByDeviceId(@Param("subCode") String subCode,
                                  @Param("deviceId") String deviceId);

    List<UniDeviceVO> listByBayId(@Param("subCode") String subCode,
                                  @Param("bayId") String bayId);
}