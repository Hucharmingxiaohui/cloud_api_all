package com.dji.sample.df.thirdKmzDf.entity.thirdPoints;

import com.dji.sample.df.electricInspectionDf.model.PubWaylinePointDfEntity;
import com.dji.sample.df.thirdKmzDf.entity.thirdPoints.thirdActions.ThirdAction;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ThirdPointEntity implements Serializable {
    private int id;//航点位置
    private String photo_sequence;//拍照序列
    private List<ThirdAction> thirdActionsList;
    private PubWaylinePointDfEntity pubWaylinePointDfEntity;


}
