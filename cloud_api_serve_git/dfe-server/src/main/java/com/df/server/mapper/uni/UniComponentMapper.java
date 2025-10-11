package com.df.server.mapper.uni;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.entity.uni.UniComponentEntity;
import com.df.server.vo.UniComponent.UniComponentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部件接口
 * <p>
 * Created by lianyc on 2023-11-09
 */
@Mapper
public interface UniComponentMapper extends BaseMapper<UniComponentEntity> {

    UniComponentEntity getByName(@Param("subCode") String subCode,
                                 @Param("deviceId") String deviceId,
                                 @Param("componentName") String componentName);

    UniComponentEntity getByComponentId(@Param("subCode") String subCode,
                                        @Param("componentId") String componentId);

    List<UniComponentVO> listByDeviceId(@Param("subCode") String subCode,
                                        @Param("deviceId") String deviceId);
}