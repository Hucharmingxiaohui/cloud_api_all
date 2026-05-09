package com.dji.sample.df.solar.controller;

import com.df.framework.vo.Result;
import com.dji.sample.df.solar.model.dto.SolarDetectRequestDTO;
import com.dji.sample.df.solar.model.entity.SolarPanelArea;
import com.dji.sample.df.solar.service.SolarPanelAreaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 光伏区域管理接口
 */

@Slf4j
@RestController
@RequestMapping("/api/solarPanelArea")
public class SolarPanelAreaController {

    @Resource
    private SolarPanelAreaService solarPanelAreaService;

    /**
     * 新增光伏区域参数
     */
    @PostMapping("/save")
    public Result save(@RequestBody SolarPanelArea solarPanelArea) {
        boolean success = solarPanelAreaService.saveSolarPanelArea(solarPanelArea);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 根据ID更新光伏区域参数
     */
    @PostMapping("/update")
    public Result update(@RequestBody SolarPanelArea solarPanelArea) {
        boolean success = solarPanelAreaService.updateSolarPanelAreaById(solarPanelArea);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 根据前端正射图新增光伏区域
     */
    @PostMapping("/detectAreaGenSolar")
    public Result detectAreaGenSolar(@RequestBody SolarDetectRequestDTO solarDetectRequestDTO) {
        return solarPanelAreaService.detectAreaGenSolar(solarDetectRequestDTO);
    }
    /**
     * 根据ID删除光伏区域参数
     */
    @GetMapping("/delete")
    public Result delete(@RequestParam String id) {
        boolean success = solarPanelAreaService.removeSolarPanelAreaById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 根据ID查询光伏区域参数
     */
    @GetMapping("/getById")
    public Result<SolarPanelArea> getById(@RequestParam Long id) {
        SolarPanelArea solarPanelArea = solarPanelAreaService.getSolarPanelAreaById(id);
        return solarPanelArea != null ? Result.success(solarPanelArea) : Result.error("数据不存在");
    }

    /**
     * 分页/条件查询光伏区域列表
     */
    @GetMapping("/selectList")
    public Result<Map> selectList(@RequestParam Map<String, Object> params) {
        Map<String, Object> resultMap = solarPanelAreaService.selectList(params);
        return Result.success(resultMap);
    }
}
