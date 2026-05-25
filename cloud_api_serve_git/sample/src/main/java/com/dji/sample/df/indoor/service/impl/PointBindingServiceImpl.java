package com.dji.sample.df.indoor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.dji.sample.df.indoor.dao.PointBindingMapper;
import com.dji.sample.df.indoor.model.dto.PointBindingRequest;
import com.dji.sample.df.indoor.model.dto.PointBindingResponse;
import com.dji.sample.df.indoor.model.entity.IndoorPointBinding;
import com.dji.sample.df.indoor.service.PointBindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointBindingServiceImpl implements PointBindingService {

    private final PointBindingMapper pointBindingMapper;  // 注入新 Mapper

    @Override
    @Transactional
    public PointBindingResponse addPoint(PointBindingRequest request) {
        checkNameUnique(null, request.getName());

        IndoorPointBinding entity = new IndoorPointBinding();   // 使用新实体
        entity.setName(request.getName());
        entity.setX(request.getX());
        entity.setY(request.getY());
        entity.setZ(request.getZ());

        pointBindingMapper.insert(entity);
        return convertToResponse(entity);
    }

    @Override
    @Transactional
    public PointBindingResponse updatePoint(String id, PointBindingRequest request) {
        IndoorPointBinding existing = getPointEntityById(id);
        checkNameUnique(id, request.getName());

        existing.setName(request.getName());
        existing.setX(request.getX());
        existing.setY(request.getY());
        existing.setZ(request.getZ());

        pointBindingMapper.updateById(existing);
        return convertToResponse(existing);
    }

    @Override
    @Transactional
    public boolean deletePoint(String id) {
        int i = pointBindingMapper.deleteById(id);
        if (i > 0) {
            return true;
        }else  {
            return false;
        }
    }

    @Override
    public PointBindingResponse getPointById(String id) {
        IndoorPointBinding entity = getPointEntityById(id);
        return convertToResponse(entity);
    }

    @Override
    public List<PointBindingResponse> getAllPoints() {
        List<IndoorPointBinding> list = pointBindingMapper.selectList(null);
        return list.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // ---------- 私有辅助方法 ----------
    private IndoorPointBinding getPointEntityById(String id) {
        IndoorPointBinding entity = pointBindingMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("点位不存在，id: " + id);
        }
        return entity;
    }

    private void checkNameUnique(String excludeId, String name) {
        LambdaQueryWrapper<IndoorPointBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IndoorPointBinding::getName, name);
        if (excludeId != null) {
            wrapper.ne(IndoorPointBinding::getId, excludeId);
        }
        Integer count = pointBindingMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("点位名称已存在: " + name);
        }
    }

    private PointBindingResponse convertToResponse(IndoorPointBinding entity) {
        PointBindingResponse response = new PointBindingResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
