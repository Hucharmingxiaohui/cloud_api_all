package com.dji.sample.df.wind.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dji.sample.df.commonDf.util.PageUtil;
import com.dji.sample.df.wind.dao.PointOfInterestMapper;
import com.dji.sample.df.wind.model.entity.PointOfInterest;
import com.dji.sample.df.wind.service.PointOfInterestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class PointOfInterestServiceImpl extends ServiceImpl<PointOfInterestMapper, PointOfInterest> implements PointOfInterestService {

    @Resource
    private PointOfInterestMapper pointOfInterestMapper;

    @Override
    public boolean savePointOfInterest(PointOfInterest pointOfInterest) {
        // 生成ID并设置默认值（如果有需要的话）
        pointOfInterest.setId(UUID.randomUUID().toString());
        // 如果有其他需要设置的默认字段，可以在这里添加
        // 例如: pointOfInterest.setSomeField(defaultValue);
        int insert = pointOfInterestMapper.insert(pointOfInterest);
        return insert > 0;
    }

    @Override
    public boolean updatePointOfInterestById(PointOfInterest pointOfInterest) {
        int i = pointOfInterestMapper.updateById(pointOfInterest);
        return i > 0;
    }

    @Override
    public boolean removePointOfInterestById(String id) {
        int i = pointOfInterestMapper.deleteById(id);
        return i > 0;
    }

    @Override
    public PointOfInterest getPointOfInterestById(String id) {
        return pointOfInterestMapper.selectById(id);
    }

    @Override
    public Map<String, Object> selectList(Map map) {
        // 设置分页参数（假设PageUtil工具类适用）
        PageUtil.setPageArgs(map);

        // 查询数据列表和总数
        List<PointOfInterest> pointList = pointOfInterestMapper.selectList(map);
        int count = pointOfInterestMapper.selectListCount(map);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> pagination = new HashMap<>();

        pagination.put("page", Integer.parseInt(map.get("page").toString()));
        pagination.put("pageSize", Integer.parseInt(map.get("pageSize").toString()));
        pagination.put("total", count);

        result.put("list", pointList);
        result.put("pagination", pagination);

        return result;
    }

}
