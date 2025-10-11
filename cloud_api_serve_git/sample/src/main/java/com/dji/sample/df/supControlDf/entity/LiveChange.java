package com.dji.sample.df.supControlDf.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class LiveChange implements Serializable {
    private String camera_position;
    private String video_id;
}
