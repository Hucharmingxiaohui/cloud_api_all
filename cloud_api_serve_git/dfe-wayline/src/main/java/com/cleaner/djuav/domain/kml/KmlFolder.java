package com.cleaner.djuav.domain.kml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Data;

import java.util.List;

@Data
@XStreamAlias("Folder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KmlFolder {
    @XStreamAlias("wpml:templateType")
    private String templateType;

    @XStreamAlias("wpml:templateId")
    private String templateId;

    @XStreamAlias("wpml:autoFlightSpeed")
    private String autoFlightSpeed;

    @XStreamAlias("wpml:waylineCoordinateSysParam")
    private KmlWayLineCoordinateSysParam waylineCoordinateSysParam;

    @XStreamAlias("wpml:payloadParam")
    private KmlPayloadParam payloadParam;

    // 以下为航点飞行模板元素
    @XStreamAlias("wpml:globalWaypointTurnMode")
    private String globalWaypointTurnMode;

    @XStreamAlias("wpml:globalUseStraightLine")
    private String globalUseStraightLine;

    @XStreamAlias("wpml:gimbalPitchMode")
    private String gimbalPitchMode;

    @XStreamAlias("wpml:globalHeight")
    private String globalHeight;

    @XStreamAlias("wpml:globalWaypointHeadingParam")
    private KmlGlobalWaypointHeadingParam globalWaypointHeadingParam;

    @XStreamImplicit(itemFieldName = "Placemark")
    private List<KmlPlacemark> placemarkList;

    // 以下为 wpml 文件使用
    @XStreamAlias("wpml:executeHeightMode")
    private String executeHeightMode;

    @XStreamAlias("wpml:waylineId")
    private String waylineId;

    @XStreamAlias("wpml:startActionGroup")
    private KmlActionGroup startActionGroup;

}
