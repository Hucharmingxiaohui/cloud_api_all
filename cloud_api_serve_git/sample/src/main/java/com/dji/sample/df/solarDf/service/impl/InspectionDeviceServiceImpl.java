package com.dji.sample.df.solarDf.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dji.sample.df.commonDf.util.PageUtil;
import com.dji.sample.df.solarDf.dao.InspectionDeviceMapper;
import com.dji.sample.df.solarDf.model.entity.InspectionDevice;
import com.dji.sample.df.solarDf.service.InspectionDeviceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 巡视设备服务实现类
 */
@Service
public class InspectionDeviceServiceImpl extends ServiceImpl<InspectionDeviceMapper, InspectionDevice> implements InspectionDeviceService {

    @Resource
    private InspectionDeviceMapper inspectionDeviceMapper;

    @Override
    public boolean saveInspectionDevice(InspectionDevice inspectionDevice) {
        // 主键自增，无需设置 ID；如有默认值字段可在此初始化
        int insert = inspectionDeviceMapper.insert(inspectionDevice);
        return insert > 0;
    }

    @Override
    public boolean updateInspectionDeviceById(InspectionDevice inspectionDevice) {
        int i = inspectionDeviceMapper.updateById(inspectionDevice);
        return i > 0;
    }

    @Override
    public boolean removeInspectionDeviceById(Long id) {
        int i = inspectionDeviceMapper.deleteById(id);
        return i > 0;
    }

    @Override
    public InspectionDevice getInspectionDeviceById(Long id) {
        return inspectionDeviceMapper.selectById(id);
    }

    @Override
    public Map<String, Object> selectList(Map<String, Object> params) {
        // 使用分页工具类设置分页参数
        PageUtil.setPageArgs(params);
        List<InspectionDevice> list = inspectionDeviceMapper.selectListByMap(params);
        int count = inspectionDeviceMapper.selectListCount(params);

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", Integer.parseInt(params.get("page").toString()));
        pagination.put("pageSize", Integer.parseInt(params.get("pageSize").toString()));
        pagination.put("total", count);
        result.put("list", list);
        result.put("pagination", pagination);
        return result;
    }
}
