package com.df.server.vo.UniPoint;

import com.df.framework.annotation.Excel;
import lombok.Data;

import java.io.Serializable;

/**
 * Excel 检查、导入错误信息表
 */
@Data
public class UniPointExcelImportErrorExcelVO implements Serializable {

    /**
     * 发生错误的数据行号
     */
    @Excel(name = "发生错误的数据行号")
    private Integer errorRow;

    /**
     * 错误原因
     */
    @Excel(name = "错误原因")
    private String errorReason;


    private static final long serialVersionUID = 1L;
}