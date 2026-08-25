package com.dji.sample.df.solarDf.service.impl;

import com.dji.sample.df.solarDf.model.entity.OrthophotoEntity;
import com.dji.sample.df.solarDf.model.entity.SolarPanel;
import com.dji.sample.df.solarDf.model.entity.SolarStationPoints;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

final class SolarStationPointGenerator {

    private static final int POINTS_PER_COMPONENT = 3;
    private static final String INFRARED_POINT_PREFIX = "红外点位";

    private SolarStationPointGenerator() {
    }

    static List<SolarStationPoints> generate(OrthophotoEntity orthophoto, List<SolarPanel> panels) {
        List<SolarStationPoints> result = new ArrayList<>();
        if (orthophoto == null || panels == null || panels.isEmpty()) {
            return result;
        }
        for (SolarPanel panel : panels) {
            if (panel == null || !StringUtils.hasText(panel.getSolarPanelName())) {
                continue;
            }
            String componentName = panel.getSolarPanelName() + "部件";
            String componentId = panel.getId() + "-component";
            for (int i = 1; i <= POINTS_PER_COMPONENT; i++) {
                result.add(createPoint(orthophoto, panel, componentName, componentId, "点位" + i));
            }
            for (int i = 1; i <= POINTS_PER_COMPONENT; i++) {
                result.add(createPoint(orthophoto, panel, componentName, componentId, INFRARED_POINT_PREFIX + i));
            }
        }
        return result;
    }

    private static SolarStationPoints createPoint(OrthophotoEntity orthophoto, SolarPanel panel,
                                                 String componentName, String componentId, String pointNameSuffix) {
        SolarStationPoints point = new SolarStationPoints();
        String orthophotoName = orthophoto.getName();
        point.setAreaName(orthophotoName);
        point.setAreaId(orthophoto.getId());
        point.setBayName(orthophotoName + "间隔");
        point.setBayId(orthophoto.getId() + "-bay");
        point.setMainDeviceName(panel.getSolarPanelName());
        point.setMainDeviceId(panel.getId());
        point.setComponentName(componentName);
        point.setComponentId(componentId);
        point.setPointName(componentName + "-" + pointNameSuffix);
        point.setDeviceType("");
        point.setSaveType("3");
        point.setDataType(4);
        point.setPointType("1");
        point.setRecognitionType("2");
        point.setMeterType("1");
        point.setAppearanceType("1");
        return point;
    }
}
