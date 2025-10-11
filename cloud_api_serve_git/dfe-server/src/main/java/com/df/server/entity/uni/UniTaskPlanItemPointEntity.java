package com.df.server.entity.uni;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 机器人巡检任务接口
 * <p>
 * Created by lianyc on 2025-05-20
 */
@Data
@TableName("df_uni_task_plan_item_point")
public class UniTaskPlanItemPointEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @TableId
    private Integer id;
    /**
     *
     */
    private String subCode;
    /**
     * 任务计划唯一编号
     */
    private String planNo;
    /**
     * 点位类型，1摄像机 2机器人 3摄像机+机器人 4无人机 5摄像机+无人机 8声纹 16在线监测
     */
    private Integer dataType;
    /**
     * 采集保存文件类型，详见字典表类型save_type 与df_uni_point保持一致
     */
    private Integer fileType;
    /**
     * 关联df_uni_point
     */
    private String pointCode;
    /**
     *
     */
    private Integer robotPos;
    /**
     *
     */
    private String robotCode;

}
