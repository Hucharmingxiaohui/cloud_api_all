package com.dji.sample.df.supControlDf.service;

public interface Dock2ConTrolService {
    //机巢控制
    //进入退出远程调试模式
    String dockControl(String sn,String Type,String Command,String Item);
}
