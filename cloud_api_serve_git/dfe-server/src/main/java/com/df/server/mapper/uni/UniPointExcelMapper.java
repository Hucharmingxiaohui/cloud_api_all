package com.df.server.mapper.uni;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.entity.uni.UniPointExcelEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Excel 点位上传记录接口
 * <p>
 * Created by lianyc on 2023-11-11
 */
@Mapper
public interface UniPointExcelMapper extends BaseMapper<UniPointExcelEntity> {
    void clearExcel();
}