package com.df.server.service.uni;


import com.baomidou.mybatisplus.extension.service.IService;
import com.df.server.entity.uni.UniComponentEntity;
import com.df.server.vo.UniComponent.UniComponentVO;

import java.util.List;

/**
 * 部件接口
 * <p>
 * Created by lianyc on 2023-11-09
 */
public interface UniComponentService extends IService<UniComponentEntity> {


    UniComponentEntity getByName(String subCode, String deviceId, String componentName);

    UniComponentEntity getByComponentId(String subCode, String componentId);

    List<UniComponentVO> listByDeviceId(String subCode, String deviceId);

    void deleteByBay(String subCode, String bayId);

    void deleteByArea(String subCode, String areaId);

    /**
     * 按变电站删除
     *
     * @param subCode
     * @return
     */
    int deleteBySubCode(String subCode);
}

