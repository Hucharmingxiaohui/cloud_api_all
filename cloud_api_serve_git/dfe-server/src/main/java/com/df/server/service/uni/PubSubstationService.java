package com.df.server.service.uni;


import com.baomidou.mybatisplus.extension.service.IService;
import com.df.framework.vo.Tree;
import com.df.server.entity.uni.PubSubstationEntity;

import java.util.List;

/**
 * 场站接口
 * <p>
 * Created by lianyc on 2025-02-11
 */
public interface PubSubstationService extends IService<PubSubstationEntity> {


    List<Tree> pubSubstationTree();

    PubSubstationEntity getStationBySubCode(String subCode);

    List<String> listAllSubCode();

}

