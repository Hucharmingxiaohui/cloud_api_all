package com.df.server.entity.uni;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * Excel 点位上传记录接口
 * <p>
 * Created by lianyc on 2023-11-11
 */
@Data
@TableName("df_uni_point_excel")
public class UniPointExcelEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 唯一标识
     */
    @TableId
    private Integer id;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 文件保存地址
     */
    private String filePath;
    /**
     * 上传时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date uploadTime;
    /**
     * 上传数据的用户id
     */
    private String uploadUser;
    /**
     * 导入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date importTime;
    /**
     * 0未检查 1数据检查中 2检查没通过 3检查通过 4导入中  5导入完成
     */
    private Integer importStatus;
    /**
     * 读取Excel总行数
     */
    private Integer pointAllCount;
    /**
     * 导入成功点位数量
     */
    private Integer importSuccessCount;

}
