package com.dji.sample.df.wind.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dji.sample.df.wind.model.entity.WindTurbine;

import java.util.List;
import java.util.Map;

public interface WindTurbineService  extends IService<WindTurbine> {
    /**
     * 新增风机参数
     *
     * @param windTurbine 风机参数实体
     * @return 是否新增成功
     */
    boolean saveWindTurbine(WindTurbine windTurbine);

    /**
     * 根据ID更新风机参数
     * @param windTurbine 风机参数实体（含ID）
     * @return 是否更新成功
     */
    boolean updateWindTurbineById(WindTurbine windTurbine);

    /**
     * 根据ID删除风机参数
     * @param id 主键ID
     * @return 是否删除成功
     */
    boolean removeWindTurbineById(String id);

    /**
     * 根据ID查询风机参数
     * @param id 主键ID
     * @return 风机参数实体
     */
    WindTurbine getWindTurbineById(String id);

    /**
     * 查询所有风机参数
     * @return 风机参数列表
     */
    List<WindTurbine> selectList(Map map);

    WindTurbine selectByName(String name);
}
