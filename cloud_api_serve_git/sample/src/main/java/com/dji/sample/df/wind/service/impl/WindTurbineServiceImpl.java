package com.dji.sample.df.wind.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dji.sample.df.commonDf.util.PageUtil;
import com.dji.sample.df.wind.dao.FanStationPointsMapper;
import com.dji.sample.df.wind.dao.WindTurbineMapper;
import com.dji.sample.df.wind.model.entity.FanStationPoints;
import com.dji.sample.df.wind.model.entity.WindTurbine;
import com.dji.sample.df.wind.service.WindTurbineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Service
@Transactional
public class WindTurbineServiceImpl extends ServiceImpl<WindTurbineMapper, WindTurbine> implements WindTurbineService {

    @Resource
    private WindTurbineMapper windTurbineMapper;

    @Resource
    FanStationPointsMapper fanStationPointsMapper;

    @Override
    public boolean saveWindTurbine(WindTurbine windTurbine) {
        windTurbine.setId(UUID.randomUUID().toString());
        windTurbine.setApproachYaw(0.0);
        windTurbine.setBladeStopAngle(0.0);
        int insert = windTurbineMapper.insert(windTurbine);
        if (insert > 0) {
            return true;
        }else  {
            return false;
        }
    }

    @Override
    public boolean updateWindTurbineById(WindTurbine windTurbine) {
        int i = windTurbineMapper.updateById(windTurbine);
        if (i > 0) {
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean removeWindTurbineById(String id) {
        int i = windTurbineMapper.deleteById(id);
        if (i > 0) {
            return true;
        }else {
            return false;
        }
    }

    @Override
    public WindTurbine getWindTurbineById(String id) {
        WindTurbine windTurbine = windTurbineMapper.selectById(id);
        return windTurbine;
    }

    @Override
    public boolean addPointsById(String id) {
        // 1. 查询风机信息
        WindTurbine windTurbine = windTurbineMapper.selectById(id);
        if (windTurbine == null) {
            throw new RuntimeException("未找到风机信息，ID: " + id);
        }

        // 2. 获取风机参数（假设WindTurbine实体类中有这些字段）
        String fanName = windTurbine.getTurbineName(); // 风机名称
        String fanId = windTurbine.getId(); // 风机ID

        // 假设从配置或风机参数中获取点数
        int bladePoints = windTurbine.getBladePoints(); // 每个叶片的点数，可从配置获取
        int towerPoints = windTurbine.getTowerPoints(); // 塔筒点数，可从配置获取

        // 3. 生成点位列表
        List<FanStationPoints> pointsList = generatePoints(fanName, fanId, bladePoints, towerPoints);

        for (FanStationPoints fanStationPoints : pointsList) {
            // 根据 pointName 查询是否已存在记录
            String pointName = fanStationPoints.getPointName();

            if (pointName != null && !pointName.trim().isEmpty()) {
                // 使用 LambdaQueryWrapper 构建查询条件
                LambdaQueryWrapper<FanStationPoints> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FanStationPoints::getPointName, pointName);

                // 查询数据库中是否已存在相同 pointName 的记录
                FanStationPoints existingPoint = fanStationPointsMapper.selectOne(queryWrapper);

                if (existingPoint != null) {
                    // 如果存在，更新记录
                    // 将新数据的ID设置为查询到的ID，确保更新的是同一条记录
                    fanStationPoints.setId(existingPoint.getId());
                    fanStationPointsMapper.updateById(fanStationPoints);
                    System.out.println("更新记录: pointName = " + pointName);
                } else {
                    // 如果不存在，插入新记录
                    fanStationPointsMapper.insert(fanStationPoints);
                    System.out.println("插入新记录: pointName = " + pointName);
                }
            } else {
                // 如果 pointName 为空，直接插入（或根据业务需求处理）
                fanStationPointsMapper.insert(fanStationPoints);
                System.out.println("插入新记录（pointName为空）");
            }
        }
        return true;
    }

    @Override
    public Map<String,Object> getPointsByFanId(Map map) {
        PageUtil.setPageArgs(map);
        List<FanStationPoints> fanStationPoints = fanStationPointsMapper.selectListById(map);
        int count = fanStationPointsMapper.selectListCount(map);
        Map result = new HashMap();
        Map pagination = new HashMap();
        pagination.put("page",Integer.parseInt(map.get("page").toString()));
        pagination.put("pageSize",Integer.parseInt(map.get("pageSize").toString()));
        pagination.put("total",count);
        result.put("list", fanStationPoints);
        result.put("pagination", pagination);
        return result;
    }

    /**
     * 生成点位列表
     */
    private List<FanStationPoints> generatePoints(String fanName, String fanId, int bladePoints, int towerPoints) {
        List<FanStationPoints> points = new ArrayList<>();

        // 叶片类型数组
        String[] bladeTypes = {"A", "B", "C"};

        // 1. 生成迎风面点位
        // 叶片迎风面
        for (String bladeType : bladeTypes) {
            for (int i = 1; i <= bladePoints; i++) {
                points.add(createPoint(fanName, fanId, String.format("%s叶片-迎风面-%d", bladeType, i)));
            }
        }

        // 轮毂迎风面
        points.add(createPoint(fanName, fanId, "轮毂-迎风面"));

        // 塔筒迎风面
        for (int i = 1; i <= towerPoints; i++) {
            points.add(createPoint(fanName, fanId, String.format("塔筒-迎风面-%d", i)));
        }

        // 2. 生成背风面点位
        // 叶片背风面
        for (String bladeType : bladeTypes) {
            for (int i = 1; i <= bladePoints; i++) {
                points.add(createPoint(fanName, fanId, String.format("%s叶片-背风面-%d", bladeType, i)));
            }
        }

        // 轮毂背风面
        points.add(createPoint(fanName, fanId, "轮毂-背风面"));

        // 塔筒背风面
        for (int i = 1; i <= towerPoints; i++) {
            points.add(createPoint(fanName, fanId, String.format("塔筒-背风面-%d", i)));
        }

        return points;
    }

    /**
     * 创建单个点位对象
     */
    private FanStationPoints createPoint(String fanName, String fanId, String pointName) {
        // 这里需要根据你的实际业务设置字段值
        // 假设你已经有了FanStationPoints实体类

        FanStationPoints point = new FanStationPoints();

        // 设置必要的字段
        point.setMainDeviceName(fanName);     // 风机名称
        point.setMainDeviceId(fanId);         // 风机ID
        point.setPointName(fanName+"-"+pointName);        // 点位名称

        // 生成点位ID（可根据需要调整）
        point.setPointId(UUID.randomUUID().toString());

        // 设置其他固定字段（根据你的业务需求）
        point.setStationName("龙源风电场");          // 根据实际设置
        point.setStationCode("Sub_WeiLan");        // 根据实际设置
        point.setAreaName("风电场区域");            // 根据实际设置
        point.setAreaId("A001");                   // 根据实际设置
        point.setBayName("1号间隔");               // 根据实际设置
        point.setBayId("BAY001");                  // 根据实际设置
        point.setComponentName(fanName+"Component");                  // 根据实际设置
        point.setComponentId(fanName+"Component001");                  // 根据实际设置

        // 设置其他固定字段（写死）
        point.setDeviceType("1");
        point.setSaveType("3");
        point.setDataType(4);
        point.setPointType("1");
        point.setRecognitionType("2");
        point.setMeterType("1");
        point.setAppearanceType("1");

        return point;
    }




    @Override
    public Map<String,Object> selectList(Map map) {
        PageUtil.setPageArgs(map);
        List<WindTurbine> windTurbines = windTurbineMapper.selectList(map);
        int count = windTurbineMapper.selectListCount(map);
        Map result = new HashMap();
        Map pagination = new HashMap();
        pagination.put("page",Integer.parseInt(map.get("page").toString()));
        pagination.put("pageSize",Integer.parseInt(map.get("pageSize").toString()));
        pagination.put("total",count);
        result.put("list", windTurbines);
        result.put("pagination", pagination);
        return result;
    }

    @Override
    public WindTurbine selectByName(String name) {
        WindTurbine windTurbine = windTurbineMapper.selectOne(new LambdaQueryWrapper<WindTurbine>().eq(WindTurbine::getTurbineName, name));
        return windTurbine;
    }


}
