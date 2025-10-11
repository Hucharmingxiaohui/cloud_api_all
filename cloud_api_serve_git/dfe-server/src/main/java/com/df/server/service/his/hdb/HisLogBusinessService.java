package com.df.server.service.his.hdb;


import com.df.server.entity.his.HisLogBusinessEntity;

/**
 * 业务日志记录接口
 * <p>
 * Created by lianyc on 2023-11-02
 */
public interface HisLogBusinessService {


    //新增操作日志
    void insertHisLogBusiness(HisLogBusinessEntity entity);

}

