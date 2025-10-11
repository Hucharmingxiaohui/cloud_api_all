package com.df.server.service.sys.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.server.entity.sys.SysLogSystemEntity;
import com.df.server.mapper.sys.SysLogSystemMapper;
import com.df.server.service.sys.SysLogSystemService;
import org.springframework.stereotype.Service;

@Service("sysLogSystemService")
public class SysLogSystemServiceImpl extends ServiceImpl<SysLogSystemMapper, SysLogSystemEntity> implements SysLogSystemService {


}
