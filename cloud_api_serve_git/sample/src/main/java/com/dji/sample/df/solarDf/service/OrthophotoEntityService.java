package com.dji.sample.df.solarDf.service;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dji.sample.df.solarDf.model.entity.OrthophotoEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface OrthophotoEntityService extends IService<OrthophotoEntity> {
    OrthophotoEntity importOrthophoto(MultipartFile file, String name);
    Map<String, Object> selectList(Map<String, Object> params);
    void deleteOrthophoto(String id);
    OrthophotoEntity selectById(String id);
    JSONArray selectComponentsById(String id);
}
