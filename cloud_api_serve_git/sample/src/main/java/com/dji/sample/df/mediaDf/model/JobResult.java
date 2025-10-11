package com.dji.sample.df.mediaDf.model;

import com.dji.sample.df.electricInspectionDf.model.PubWaylinePointDfEntity;
import lombok.Data;

@Data
public class JobResult {
    MediaFileDTO mediaFileDTO;//媒体信息
    PubWaylinePointDfEntity pubWaylinePointDfEntity;//点位信息
}
