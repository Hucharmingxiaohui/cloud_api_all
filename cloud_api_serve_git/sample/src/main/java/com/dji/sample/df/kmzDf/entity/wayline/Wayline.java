package com.dji.sample.df.kmzDf.entity.wayline;



import com.dji.sample.df.kmzDf.entity.wayline.Folder.Folder;
import com.dji.sample.df.kmzDf.entity.wayline.MissionConfig.MissionConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
//航点航线实体类
@Data
public class Wayline {
    @JsonProperty("missionConfig")
    private MissionConfig missionConfig;//无人机配置
    @JsonProperty("folder")
    private Folder folder;//全局及航点配置
}
