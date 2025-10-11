package com.df.server.service.uni;


import com.baomidou.mybatisplus.extension.service.IService;
import com.df.server.entity.uni.UniBayEntity;
import com.df.server.vo.UniBay.UniBayVO;

import java.util.List;

/**
 * 间隔接口
 * <p>
 * Created by lianyc on 2023-11-09
 */
public interface UniBayService extends IService<UniBayEntity> {


    UniBayEntity getByName(String subCode, String areaId, String bayName);

    UniBayEntity getByBayId(String subCode, String bayId);

    void deleteBay(Integer tableId);

    /**
     * 按变电站删除
     *
     * @param subCode
     * @return
     */
    int deleteBySubCode(String subCode);

    List<UniBayVO> listByAreaId(String subCode, String areaId);
}

