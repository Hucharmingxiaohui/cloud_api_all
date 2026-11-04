package com.dji.sample.df.cqDockDf.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dji.sample.df.cqDockDf.dao.CqDockHmsMapper;
import com.dji.sample.df.cqDockDf.dao.CqDockUavMonitoringMapper;
import com.dji.sample.df.cqDockDf.model.dto.*;
import com.dji.sample.df.cqDockDf.model.entity.CqDockHmsEntity;
import com.dji.sample.df.cqDockDf.model.entity.CqDockUavMonitoringEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Service
public class CqDockReadService {
    private static final String[] MONITORING_COLUMNS = ("id,unit_code,topic_dock_sn,task_id,dock_sn,drone_sn,"
            + "drone_message_id,dock_message_id,drone_curr_time,dock_curr_time,drone_mode_code,dock_mode_code,"
            + "flighttask_step_code,drone_in_dock,latitude,longitude,flight_height,horizontal_speed,vertical_speed,"
            + "home_distance,flight_distance,total_flight_time,battery_level,nest_voltage,nest_temperature,"
            + "nest_humidity,ambient_temperature,wind_speed,rainfall,create_time,update_time").split(",");
    private static final String[] HMS_COLUMNS = ("id,unit_code,topic_dock_sn,message_id,alarm_code,dock_sn,uav_sn,"
            + "gateway_type,imminent,in_the_sky,level,module,message,create_time,update_time").split(",");

    private final CqDockUavMonitoringMapper monitoringMapper;
    private final CqDockHmsMapper hmsMapper;

    public CqDockReadService(CqDockUavMonitoringMapper monitoringMapper, CqDockHmsMapper hmsMapper) {
        this.monitoringMapper = monitoringMapper;
        this.hmsMapper = hmsMapper;
    }

    public CqDockReadPage<CqDockMonitoringView> monitoringPage(CqDockReadQuery query) {
        validate(query);
        if (query.getLevel() != null || query.getModule() != null || query.getGatewayType() != null) {
            throw new IllegalArgumentException("Alarm filters apply only to HMS");
        }
        QueryWrapper<CqDockUavMonitoringEntity> wrapper = new QueryWrapper<>();
        wrapper.select(MONITORING_COLUMNS);
        common(wrapper, query, "drone_sn", "update_time");
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = literalLike(query.getKeyword());
            wrapper.and(w -> w.apply("unit_code LIKE {0}", keyword).or().apply("dock_sn LIKE {0}", keyword)
                    .or().apply("drone_sn LIKE {0}", keyword).or().apply("topic_dock_sn LIKE {0}", keyword)
                    .or().apply("task_id LIKE {0}", keyword));
        }
        wrapper.orderByDesc("update_time", "id");
        Page<CqDockUavMonitoringEntity> page = monitoringMapper.selectPage(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return new CqDockReadPage<>(page.getRecords().stream().map(CqDockMonitoringView::from).collect(Collectors.toList()),
                page.getTotal(), query.getCurrent(), query.getSize());
    }

    public CqDockReadPage<CqDockHmsView> hmsPage(CqDockReadQuery query) {
        validate(query);
        QueryWrapper<CqDockHmsEntity> wrapper = new QueryWrapper<>();
        wrapper.select(HMS_COLUMNS);
        common(wrapper, query, "uav_sn", "create_time");
        wrapper.eq(query.getLevel() != null, "level", query.getLevel())
                .eq(query.getModule() != null, "module", query.getModule())
                .eq(query.getGatewayType() != null, "gateway_type", query.getGatewayType());
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = literalLike(query.getKeyword());
            wrapper.and(w -> w.apply("unit_code LIKE {0}", keyword).or().apply("dock_sn LIKE {0}", keyword)
                    .or().apply("uav_sn LIKE {0}", keyword).or().apply("topic_dock_sn LIKE {0}", keyword)
                    .or().apply("alarm_code LIKE {0}", keyword).or().apply("message LIKE {0}", keyword)
                    .or().apply("message_id LIKE {0}", keyword));
        }
        wrapper.orderByDesc("create_time", "id");
        Page<CqDockHmsEntity> page = hmsMapper.selectPage(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return new CqDockReadPage<>(page.getRecords().stream().map(CqDockHmsView::from).collect(Collectors.toList()),
                page.getTotal(), query.getCurrent(), query.getSize());
    }

    private static <T> void common(QueryWrapper<T> wrapper, CqDockReadQuery query, String droneColumn, String timeColumn) {
        wrapper.eq(StringUtils.hasText(query.getUnitCode()), "unit_code", query.getUnitCode())
                .eq(StringUtils.hasText(query.getDockSn()), "dock_sn", query.getDockSn())
                .eq(StringUtils.hasText(query.getDroneSn()), droneColumn, query.getDroneSn())
                .ge(query.getStartTime() != null, timeColumn, query.getStartTime())
                .le(query.getEndTime() != null, timeColumn, query.getEndTime());
    }

    private static String literalLike(String text) {
        return "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    private static void validate(CqDockReadQuery query) {
        if (query == null || query.getCurrent() < 1 || query.getSize() < 1 || query.getSize() > 100
                || query.getCurrent() - 1 > Long.MAX_VALUE / query.getSize()) {
            throw new IllegalArgumentException("Invalid pagination: current >= 1, size 1..100, bounded offset required");
        }
        if (query.getStartTime() != null && query.getEndTime() != null && query.getStartTime().isAfter(query.getEndTime())) {
            throw new IllegalArgumentException("start_time must not exceed end_time");
        }
        if (query.getLevel() != null && (query.getLevel() < 0 || query.getLevel() > 2)
                || query.getModule() != null && (query.getModule() < 0 || query.getModule() > 3)
                || query.getGatewayType() != null && !"1".equals(query.getGatewayType()) && !"2".equals(query.getGatewayType())) {
            throw new IllegalArgumentException("Invalid HMS level, module or gateway_type");
        }
        for (String text : new String[]{query.getUnitCode(), query.getDockSn(), query.getDroneSn(), query.getKeyword()}) {
            if (text != null && text.length() > 256) {
                throw new IllegalArgumentException("Filter text must not exceed 256 characters");
            }
        }
    }
}
