package com.df.server.entity.his;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by lianyc on 2025-05-23
 */
@Data
public class HisUniTaskReportEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private Integer id;
    /**
     *
     */
    private String name;
    /**
     *
     */
    private String taskPatrolledId;
    /**
     *
     */
    private String confirmUser;
    /**
     *
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date confirmTime;
    /**
     * 相对于文件发布的相对路径，参考任务图片
     */
    private String pdfFile;
    /**
     *
     */
    private String wordFile;
    /**
     * 0未审核，1已审核
     */
    private Integer isConfirm;
    /**
     * 环境数据，时间点为报告首次生成时
     */
    private String env;
    /**
     * 1：待生成 2：报告生成中 3：报告生成完毕
     */
    private Integer reportStatus;
    /**
     * 报告开始生成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reportStartTime;
    /**
     * 报告生成结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reportEndTime;

}
