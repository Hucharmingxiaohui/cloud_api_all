package com.df.server.mapper.uni;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.dto.uniPoint.UniPointExcelImportErrorDTO;
import com.df.server.entity.uni.UniPointExcelImportErrorEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Excel 检查、导入错误信息接口
 * <p>
 * Created by lianyc on 2023-11-11
 */
@Mapper
public interface UniPointExcelImportErrorMapper extends BaseMapper<UniPointExcelImportErrorEntity> {
    List<UniPointExcelImportErrorEntity> listError(UniPointExcelImportErrorDTO dto);

    int listErrorTotal(UniPointExcelImportErrorDTO dto);

    void clearErrorInfo();


}