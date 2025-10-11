package com.df.server.service.uni;


import com.baomidou.mybatisplus.extension.service.IService;
import com.df.server.entity.uni.UniDeviceEntity;
import com.df.server.vo.UniDevice.UniDeviceVO;

import java.util.List;

/**
 * 主设备接口
 * <p>
 * Created by lianyc on 2023-11-09
 */
public interface UniDeviceService extends IService<UniDeviceEntity> {


    UniDeviceEntity getByName(String subCode, String bayId, String deviceName);

    UniDeviceEntity getByDeviceId(String subCode, String deviceId);

    List<UniDeviceVO> listByBayId(String subCode, String bayId);

    void deleteByArea(String subCode, String areaId);

    /**
     * 按变电站删除
     *
     * @param subCode
     * @return
     */
    int deleteBySubCode(String subCode);
}

