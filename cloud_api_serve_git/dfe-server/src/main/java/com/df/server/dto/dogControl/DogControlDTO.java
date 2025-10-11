package com.df.server.dto.dogControl;

import lombok.Data;

/**
 * |
 * 机器狗控制
 */
@Data
public class DogControlDTO {

    /**
     * 前进	{"key":"w"}
     * 后退	{"key":"s"}
     * 左转	{"key":"q"}
     * 右转	{"key":"e"}
     * 左移	{"key":"a"}
     * 右移	{"key":"d"}
     * 站立	{"key":"f"}
     * 趴下	{"key":"g"}
     * 行走步态	{"key":"1"}
     * 小跑步态	{"key":"2"}
     * 上楼步态	{"key":"3"}
     * 下楼步态	{"key":"4"}
     */
    private String key;
    private Integer type;
    private String command;
}
