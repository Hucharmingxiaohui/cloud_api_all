package com.df.server.dto.uniPoint;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.df.framework.dto.PageDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class UniPointPageDTO extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 点位编码，对应标准规范device_id
     */
    private String pointCode;
    /**
     * 点位名称，对应标准规范device_name
     */
    private String pointName;

    /**
     * 变电站编码
     */
    private String subCode;

    /**
     * 所属区域ID
     */
    private String areaId;

    /**
     * 所属间隔ID
     */
    private String bayId;

    /**
     * 所属主设备ID
     */
    private String deviceId;

    /**
     * 所属部件ID
     */
    private String componentId;

    /**
     * 主设备类型(字典表device_type)
     */
    private Integer deviceType;
    /**
     * 表计类型 (字典表meter_type)
     */
    private Integer meterType;

    /**
     * 识别类型(字典表类型recognition_type)：1表计读取， 2位置状态识别，3设备外观查看，4红外测温，5声音检测，6闪烁检测
     */
    private String recognitionTypeList;

    @JsonIgnore
    private List<String> recognitionTypeSearchList;

    /**
     * 点位等级(字典表point_level)
     */
    private Integer pointLevel;

    public void setRecognitionTypeList(String recognitionTypeList) {
        this.recognitionTypeList = recognitionTypeList;
        if (StringUtils.isNotBlank(recognitionTypeList)) {
            this.recognitionTypeSearchList = Arrays.asList(recognitionTypeList.split(","));
        }
    }


}
