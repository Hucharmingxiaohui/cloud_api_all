package com.df.server.service.uni;


import com.baomidou.mybatisplus.extension.service.IService;
import com.df.server.entity.uni.UniAreaEntity;
import com.df.server.vo.UniArea.UniAreaVO;

import java.util.List;

/**
 * 区域接口
 * <p>
 * Created by lianyc on 2023-11-09
 */
public interface UniAreaService extends IService<UniAreaEntity> {


    /**
     * 根据变电站编码+ 名字查询
     *
     * @param areaName
     * @param subCode
     * @return
     */
    UniAreaEntity getByName(String areaName, String subCode);

    UniAreaEntity getByAreaId(String subCode, String areaId);

    void deleteById(Integer tableId);

    /**
     * 按变电站删除
     *
     * @param subCode
     * @return
     */
    int deleteBySubCode(String subCode);

    List<UniAreaVO> listBySubCode(String subCode);
}

