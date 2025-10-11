package com.dji.sample.df.thirdKmzDf.entity.pointResult;

import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointCount.PointCount;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointResult {
    @JsonProperty("counts")
     private int counts;//总数
    @JsonProperty("pointCountList")
     private List<PointCount> pointCountList;//航点列表

}
