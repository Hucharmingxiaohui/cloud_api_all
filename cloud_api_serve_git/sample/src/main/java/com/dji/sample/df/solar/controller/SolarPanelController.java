package com.dji.sample.df.solar.controller;

import com.df.framework.vo.Result;
import com.dji.sample.df.solar.model.entity.SolarPanel;
import com.dji.sample.df.solar.service.SolarPanelService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/solarPanel")
public class SolarPanelController {

    @Resource
    private SolarPanelService solarPanelService;

    /**
     * 新增光伏区域参数
     */
    @PostMapping("/save")
    public Result save(@RequestBody SolarPanel solarPanel) {
        boolean success = solarPanelService.saveSolarPanel(solarPanel);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 根据ID更新光伏区域参数
     */
    @PostMapping("/update")
    public Result update(@RequestBody SolarPanel solarPanel) {
        boolean success = solarPanelService.updateSolarPanelById(solarPanel);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 根据ID删除光伏区域参数
     */
    @GetMapping("/delete")
    public Result delete(@RequestParam Long id) {
        boolean success = solarPanelService.removeSolarPanelById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 根据ID查询光伏区域参数
     */
    @GetMapping("/getById")
    public Result<SolarPanel> getById(@RequestParam Long id) {
        SolarPanel solarPanel = solarPanelService.getSolarPanelById(id);
        return solarPanel != null ? Result.success(solarPanel) : Result.error("数据不存在");
    }

    /**
     * 分页/条件查询光伏区域列表
     */
    @GetMapping("/selectList")
    public Result<Map> selectList(@RequestParam Map<String, Object> params) {
        Map<String, Object> resultMap = solarPanelService.selectList(params);
        return Result.success(resultMap);
    }
}
