package com.df.server.service.uni;


import com.baomidou.mybatisplus.extension.service.IService;
import com.df.server.dto.UniPointThreshold.UniPointThresholdAddDTO;
import com.df.server.dto.uniPoint.PointDetailDTO;
import com.df.server.entity.uni.UniPointEntity;
import com.df.server.entity.uni.UniPointThresholdEntity;
import com.df.server.vo.UniPointThreshold.PointAlarmThresholdVO;

/**
 * 机器人点位告警阈值接口
 * <p>
 * Created by lianyc on 2025-05-19
 */
public interface UniPointThresholdService extends IService<UniPointThresholdEntity> {


    PointAlarmThresholdVO getPointRelAlarmThresholdInfo(PointDetailDTO dto);

    void modifyThreshold(UniPointThresholdAddDTO addDTO);

    void delete(PointDetailDTO pointDetailDTO);

    /**
     * 点位结果阈值分析
     *
     * @param uniPoint
     * @param pointVal
     * @return
     */
    Integer isAlarmByThreshold(UniPointEntity uniPoint, String pointVal);

    String getPointThresholdDes(String pointCode);
}

