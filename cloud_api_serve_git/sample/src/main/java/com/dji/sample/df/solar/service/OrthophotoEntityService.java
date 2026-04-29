package com.dji.sample.df.solar.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dji.sample.df.solar.model.entity.OrthophotoEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface OrthophotoEntityService extends IService<OrthophotoEntity> {
    OrthophotoEntity importOrthophoto(MultipartFile file, String name);
    Map<String, Object> selectList(Map<String, Object> params);
    void deleteOrthophoto(Long id);
    OrthophotoEntity selectById(Long id);
}
