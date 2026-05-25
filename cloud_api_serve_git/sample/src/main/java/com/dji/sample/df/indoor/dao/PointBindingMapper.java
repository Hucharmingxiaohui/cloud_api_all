package com.dji.sample.df.indoor.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.dji.sample.df.indoor.model.entity.IndoorPointBinding;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PointBindingMapper extends BaseMapper<IndoorPointBinding> {
    // 基础CRUD方法已由BaseMapper提供，无需额外编写
}
