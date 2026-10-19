package com.dji.sample.df.solarDf.controller;

import com.alibaba.fastjson.JSONObject;
import com.df.framework.vo.Result;
import com.dji.sample.df.solarDf.model.entity.SolarAccessory;
import com.dji.sample.df.solarDf.service.SolarAccessoryService;
import com.dji.sample.df.solarDf.service.SolarAccessoryCoordinateService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/solarAccessory")
public class SolarAccessoryController {
    private final SolarAccessoryService service;
    private final SolarAccessoryCoordinateService coordinates;

    public SolarAccessoryController(SolarAccessoryService service, SolarAccessoryCoordinateService coordinates) {
        this.service = service;
        this.coordinates = coordinates;
    }

    @GetMapping("/list")
    public Result list(@RequestParam("orthophoto_id") String orthophotoId) {
        return Result.success(service.list(orthophotoId));
    }

    @PostMapping("/save")
    public Result save(@RequestBody SolarAccessory device) {
        return Result.success(service.save(device));
    }

    @PostMapping("/delete")
    public Result delete(@RequestBody SolarAccessory device) {
        service.delete(device.getId(), device.getOrthophotoId());
        return Result.success();
    }

    @PostMapping("/coordinates")
    public Result coordinates(@RequestBody JSONObject request) {
        return Result.success(coordinates.convert(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result invalid(IllegalArgumentException error) {
        return Result.error(error.getMessage());
    }
}
