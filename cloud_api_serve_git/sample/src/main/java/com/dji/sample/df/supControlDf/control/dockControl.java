package com.dji.sample.df.supControlDf.control;

import com.dji.sample.df.supControlDf.service.Dock2ConTrolService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("pub/api/v1/dock2Controller")
public class dockControl {
    @Autowired
    private Dock2ConTrolService dock2ConTrolService;
    @PostMapping("/controllerDock")
    public HttpResultResponse controllerDock(@RequestParam String sn,
                                             @RequestParam String Type,
                                             @RequestParam String Command,
                                              String Item){
        String flag= dock2ConTrolService.dockControl(sn,Type, Command,Item);
        return HttpResultResponse.success(flag);
    }
}
