package com.dji.sample.df.thirdKmzDf.entity.pointResult.PointCount;

import com.dji.sample.df.electricInspectionDf.model.PubWaylinePointDfEntity;
import com.dji.sample.df.mediaDf.model.MediaFileDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PointCount {
    @JsonProperty("pointPosition")
    private int pointPosition;//点位
    @JsonProperty("count")
    private int count;//数量
    @JsonProperty("mediaFileDTOS")//点位的媒体文件信息
    private List<MediaFileDTO> mediaFileDTOS;
    @JsonProperty("pubWaylinePointDfEntity")//关联点位信息
    private PubWaylinePointDfEntity pubWaylinePointDfEntity;
}
