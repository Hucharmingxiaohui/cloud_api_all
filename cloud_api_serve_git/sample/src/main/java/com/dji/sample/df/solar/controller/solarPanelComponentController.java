package com.dji.sample.df.solar.controller;

import com.df.framework.vo.Result;
import com.dji.sample.df.solar.model.entity.SolarPanelComponent;
import com.dji.sample.df.solar.service.SolarPanelComponentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 光伏组件接口
 */

@Slf4j
@RestController
@RequestMapping("/api/solarPanelComponent")
public class solarPanelComponentController {

    @Autowired
    private SolarPanelComponentService solarPanelComponentService;


    /**
     * 根据光伏板id查询光伏组件列表
     */
    @GetMapping("/selectListBySolarPanelId")
    public Result<Map> selectListBySolarPanelId(@RequestParam String solarPanelId) {
        List<SolarPanelComponent> solarPanelComponents = solarPanelComponentService.selectListBySolarPanelId(solarPanelId);
        return Result.success(solarPanelComponents);
    }

    /**
     * 根据光伏组件id查询光伏组件
     */
    @GetMapping("/selectListByComponentId")
    public Result<Map> selectListByComponentId(@RequestParam String componentId) {
        List<SolarPanelComponent> solarPanelComponents = solarPanelComponentService.selectListByComponentId(componentId);
        return Result.success(solarPanelComponents);
    }

    /**
     * 根据正射图id查询光伏组件
     */
    @GetMapping("/selectListByOrthophotoId")
    public Result<Map> selectListByOrthophotoId(@RequestParam String orthophotoId) {
        List<SolarPanelComponent> solarPanelComponents = solarPanelComponentService.selectListByOrthophotoId(orthophotoId);
        return Result.success(solarPanelComponents);
    }
}
