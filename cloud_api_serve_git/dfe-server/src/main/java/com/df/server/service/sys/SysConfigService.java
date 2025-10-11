package com.df.server.service.sys;

import com.baomidou.mybatisplus.extension.service.IService;
import com.df.framework.vo.PageVO;
import com.df.server.entity.sys.SysConfigEntity;

import java.util.List;

/**
 * 参数配置 服务层
 *
 * @author ruoyi
 */
public interface SysConfigService extends IService<SysConfigEntity> {

    /**
     * 根据key直接查询value,不走缓存
     *
     * @param configKey
     * @return
     */
    String getValueByKey(String configKey);

}
