package com.dji.sample.df.TemperatureMeasurementDF.control;

import com.dji.sample.df.TemperatureMeasurementDF.modol.TemParamEntity;
import com.dji.sample.df.TemperatureMeasurementDF.modol.TemResultEntity;
import com.dji.sample.df.TemperatureMeasurementDF.service.TemMeasureService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("tem/api/v1/workspace")
public class TemMesureControl {
    @Autowired
    private TemMeasureService temMeasureService;
    private final Object lock=new Object();
    @PostMapping("/getTemByWorkSpaceIdAndFileId")
    public HttpResultResponse getTemByWorkSpaceIdAndFileId(@RequestParam String workspace_id, @RequestParam String file_id,@RequestBody TemParamEntity temParamEntity){
        synchronized (lock){
            TemResultEntity temResultEntity= temMeasureService.getTemByWorkSpaceIdAndFileId(workspace_id, file_id, temParamEntity);
            return HttpResultResponse.success(temResultEntity);
        }

    }

}
