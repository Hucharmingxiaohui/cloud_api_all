package com.cleaner.djuav.domain.kml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

@Data
@XStreamAlias("Document")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KmlDocument {

    @XStreamAlias("wpml:author")
    private String author;

    @XStreamAlias("wpml:createTime")
    private String createTime;

    @XStreamAlias("wpml:updateTime")
    private String updateTime;

    @XStreamAlias("wpml:missionConfig")
    private KmlMissionConfig kmlMissionConfig;

    @XStreamAlias("Folder")
    private KmlFolder folder;


}
