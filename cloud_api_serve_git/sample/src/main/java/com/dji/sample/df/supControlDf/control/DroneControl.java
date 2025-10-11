package com.dji.sample.df.supControlDf.control;

import com.dji.sample.df.supControlDf.service.DroneControlService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("pub/api/v1/droneController")
public class DroneControl {
    @Autowired
    private DroneControlService droneControlService;
    @PostMapping("/controllerDrone")
    public HttpResultResponse controllerDrone(@RequestParam String sn,
                                              @RequestParam String Type,
                                              @RequestParam String Command,
                                               String Item) throws Exception {
        String flag= droneControlService.droneControl(sn,Type, Command,Item);
        return HttpResultResponse.success(flag);
    }
}
