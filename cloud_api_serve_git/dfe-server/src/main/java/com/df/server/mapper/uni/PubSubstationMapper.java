package com.df.server.mapper.uni;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.framework.vo.Tree;
import com.df.server.entity.uni.PubSubstationEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 场站接口
 * <p>
 * Created by lianyc on 2025-02-11
 */
@Mapper
public interface PubSubstationMapper extends BaseMapper<PubSubstationEntity> {
    List<Tree> pubSubstationTree();

    PubSubstationEntity getOneStation();
}