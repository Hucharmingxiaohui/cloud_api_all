package com.df.server.entity.uni;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * Excel 检查、导入错误信息接口
 * <p>
 * Created by lianyc on 2023-11-11
 */
@Data
@TableName("df_uni_point_excel_import_error")
public class UniPointExcelImportErrorEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 唯一标识
     */
    @TableId
    private Integer id;
    /**
     * 发生错误的数据行号
     */
    private Integer errorRow;
    /**
     * 对应的文件id
     */
    private Integer errorFileId;
    /**
     * 错误原因
     */
    private String errorReason;
    /**
     * 错误记录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date errorTime;
    /**
     * 1检查过程  2导入过程
     */
    private Integer errorType;

}
