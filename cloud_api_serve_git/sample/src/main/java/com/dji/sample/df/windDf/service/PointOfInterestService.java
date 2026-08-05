package com.dji.sample.df.windDf.service;

import com.dji.sample.df.pointOfInterestDf.model.PointOfInterest;

import java.util.Map;

public interface PointOfInterestService {
    /**
     * 新增兴趣点
     *
     * @param pointOfInterest 兴趣点实体
     * @return 是否新增成功
     */
    boolean savePointOfInterest(PointOfInterest pointOfInterest);

    /**
     * 根据ID更新兴趣点
     * @param pointOfInterest 兴趣点实体（含ID）
     * @return 是否更新成功
     */
    boolean updatePointOfInterestById(PointOfInterest pointOfInterest);

    /**
     * 根据ID删除兴趣点
     * @param id 主键ID
     * @return 是否删除成功
     */
    boolean removePointOfInterestById(String id);

    /**
     * 根据ID查询兴趣点
     * @param id 主键ID
     * @return 兴趣点实体
     */
    PointOfInterest getPointOfInterestById(String id);

    /**
     * 查询所有兴趣点
     * @return 兴趣点列表
     */
    Map<String,Object> selectList(Map map);
}
