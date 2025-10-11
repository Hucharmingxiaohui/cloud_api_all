package com.df.server.service.sys.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.framework.utils.CustomStringUtils;
import com.df.server.entity.sys.SysConfigEntity;
import com.df.server.mapper.sys.SysConfigMapper;
import com.df.server.service.sys.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 参数配置 服务层实现
 *
 * @author ruoyi
 */
@Slf4j
@Service("sysConfigService")
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfigEntity> implements SysConfigService {

    @Autowired
    private SysConfigMapper sysConfigMapper;

    /**
     * 根据key直接查询value,不走缓存
     *
     * @param configKey 参数键
     * @return 缓存键key
     */
    @Override
    public String getValueByKey(String configKey) {
        String s = sysConfigMapper.getConfigValueByKey(configKey);
        if (CustomStringUtils.isNotBlank(s)) {
            return s;
        } else {
            log.error("sys_config里不存在配置项：{}", configKey);
            return "";
        }
    }

}
