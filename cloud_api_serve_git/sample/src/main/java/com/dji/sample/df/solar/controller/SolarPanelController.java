package com.dji.sample.df.solar.controller;

import com.alibaba.fastjson.JSONObject;
import com.df.framework.utils.HttpUtils;
import com.df.framework.vo.Result;
import com.dji.sample.df.solar.model.dto.SolarPanelDTO;
import com.dji.sample.df.solar.model.entity.SolarPanel;
import com.dji.sample.df.solar.model.entity.SolarPanelArea;
import com.dji.sample.df.solar.service.SolarPanelService;
import com.dji.sample.df.wind.config.WaylineUrlConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 光伏板接口
 */

@Slf4j
@RestController
@RequestMapping("/api/solarPanel")
public class SolarPanelController {

    @Autowired
    SolarPanelService  solarPanelService;

    @Resource
    WaylineUrlConfig waylineUrlConfig;

    @Resource
    HttpUtils httpUtils;

    /**
     * 根正射图获取光伏板位置（包括像素点和经纬度）并新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody SolarPanelDTO solarPanelDTO) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("solar_area_name", solarPanelDTO.getSolar_area_name());
        String orthophotoId = solarPanelDTO.getOrthophoto_id();
        String jsonString = jsonObject.toString();
        String s = httpUtils.sendPostJson(waylineUrlConfig.getStartSegmentationUrl(),jsonString);
        boolean result = solarPanelService.parseAndSave(s,orthophotoId);
        if (result) {
            return Result.success("插入成功");
        }else {
            return Result.error("插入失败");
        }
    }

    /**
     * 根据ID更新光伏板参数
     */
    @PostMapping("/update")
    public Result update(@RequestBody SolarPanel solarPanel) {
        boolean success = solarPanelService.updateSolarPanelById(solarPanel);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 根据ID删除光伏板参数
     */
    @GetMapping("/delete")
    public Result delete(@RequestParam String id) {
        boolean success = solarPanelService.removeSolarPanelById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 根据ID查询光伏板参数
     */
    @GetMapping("/getById")
    public Result<SolarPanelArea> getById(@RequestParam String id) {
        SolarPanel solarPanel = solarPanelService.getSolarPanelById(id);
        return solarPanel != null ? Result.success(solarPanel) : Result.error("数据不存在");
    }

    /**
     * 分页/条件查询光伏板列表
     */
    @GetMapping("/selectList")
    public Result<Map> selectList(@RequestParam Map<String, Object> params) {
        Map<String, Object> resultMap = solarPanelService.selectList(params);
        return Result.success(resultMap);
    }

}
