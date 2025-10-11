package com.dji.sample.df.waylineDf.model.dto;

import com.dji.sample.wayline.model.dto.WaylineTaskExecutableConditionDTO;
import com.dji.sample.wayline.model.dto.WaylineTaskFileDTO;
import com.dji.sample.wayline.model.dto.WaylineTaskReadyConditionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author sean
 * @version 1.1
 * @date 2022/6/1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WaylineTaskCreateDTO {

    private String flightId;

    private Integer taskType;

    private Integer waylineType;

    private Long executeTime;

    private WaylineTaskFileDTO file;

    private Integer rthAltitude;

    private Integer outOfControlAction;

    private WaylineTaskReadyConditionDTO readyConditions;

    private WaylineTaskExecutableConditionDTO executableConditions;
}
