package com.df.server.mapper.uni;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.entity.uni.UniBayEntity;
import com.df.server.vo.UniBay.UniBayVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 间隔接口
 * <p>
 * Created by lianyc on 2023-11-09
 */
@Mapper
public interface UniBayMapper extends BaseMapper<UniBayEntity> {

    UniBayEntity getByName(@Param("subCode") String subCode,
                           @Param("areaId") String areaId,
                           @Param("bayName") String bayName);

    List<UniBayVO> listByAreaId(@Param("subCode") String subCode,
                                @Param("areaId") String areaId);
}