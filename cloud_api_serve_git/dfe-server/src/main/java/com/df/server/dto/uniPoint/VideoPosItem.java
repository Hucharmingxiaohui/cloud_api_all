package com.df.server.dto.uniPoint;

import lombok.Data;

@Data
public class VideoPosItem {
    //摄像机编码
    String device_code;
    //预置位号
    String device_pos;
    //机器人编码
    String robot_code;
    //机器人预置位号
    String robot_pos;
    //无人机编码
    String uav_code;
    //无人机预置位号
    String uav_pos;

    public static void main(String[] args) {
        String device_code = null;
        device_code = "2";
        System.out.println(device_code);
    }
}
