package com.dji.sample.control.model.param;

import com.dji.sdk.cloudapi.control.FileParam;
import com.dji.sdk.cloudapi.control.Point;
import com.dji.sdk.cloudapi.device.ExitWaylineWhenRcLostEnum;
import com.dji.sdk.cloudapi.wayline.OutOfControlActionEnum;
import com.dji.sdk.cloudapi.wayline.RthModeEnum;
import com.dji.sdk.cloudapi.wayline.WaylinePrecisionTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InFlightWaylineDeliverParam {

    @NotNull
    private FileParam file;
//  怎么取值
    @NotNull
    private String inFlightWaylineId;

    @NotNull
    private OutOfControlActionEnum outOfControlAction;

    @NotNull
    private ExitWaylineWhenRcLostEnum exitWaylineWhenRcLost;

    @Range(min = 20, max = 1500)
    @NotNull
    private Integer rthAltitude;

    @NotNull
    private RthModeEnum rthMode;

    @NotNull
    private WaylinePrecisionTypeEnum waylinePrecisionType;

}
