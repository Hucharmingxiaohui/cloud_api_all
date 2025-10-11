package com.df.server.dto.uniPoint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * 变电站公共树入参类
 *
 * @author 贾彬
 * @Time 2023/10/30 14:29
 */
@Data
public class UniTreeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 所属运维班ID
     */
    //private Integer deptId;

    /**
     * 变电站编码
     */
    private String subCode;

    /**
     * 需要获取到树的那个层级 (只针对点位层级树有效)
     * 1、公司  2、运维班  3、变电站  4、区域 5、间隔  6、设备  7、部件  8、点位
     * 如果获取到点位层级
     */
    private Integer level;

    /**
     * 针对有相机的树  1：只要视频相机  2：只要机器人相机  3：只要无人机相机  null：全部
     */
    //private Integer onlyCameraType;
    /**
     * 任务类型
     */
    private Integer taskType;
    /**
     * 任务子类型,多个逗号隔开
     */
    private String taskSubType;

    @JsonIgnore
    private List<String> taskSubTypeList;

    public void setTaskSubType(String taskSubType) {
        this.taskSubType = taskSubType;
        if (StringUtils.isNotBlank(taskSubType)) {
            this.taskSubTypeList = Arrays.asList(taskSubType.split(","));
        }
    }
}
