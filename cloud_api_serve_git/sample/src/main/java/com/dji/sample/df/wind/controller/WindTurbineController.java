package com.dji.sample.df.wind.controller;

import com.df.framework.vo.Result;
import com.dji.sample.df.wind.FlyToFront;
import com.dji.sample.df.wind.model.entity.WindTurbine;
import com.dji.sample.df.wind.service.WindTurbineService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/windTurbine")
public class WindTurbineController {


    @Resource
    private WindTurbineService windTurbineService;

    @Resource
    FlyToFront flyToFront;

    /**
     * 新增风机参数
     */
    @PostMapping("/save")
    public Result save(@RequestBody WindTurbine windTurbine) {
        System.out.println("1111111111111111");
        boolean success = windTurbineService.saveWindTurbine(windTurbine);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 根据ID更新风机参数
     */
    @PostMapping("/update")
    public Result update(@RequestBody WindTurbine windTurbine) {
        boolean success = windTurbineService.updateWindTurbineById(windTurbine);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 根据ID删除风机参数
     */
    @GetMapping("/delete")
    public Result delete(@RequestParam String id) {
        boolean success = windTurbineService.removeWindTurbineById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 根据ID查询风机参数
     */
    @GetMapping("/getById")
    public Result<WindTurbine> getById(@RequestParam String id) {
        WindTurbine windTurbine = windTurbineService.getWindTurbineById(id);
        return windTurbine != null ? Result.success(windTurbine) : Result.error("数据不存在");
    }

    /**
     * 查询所有风机参数
     */
    @GetMapping("selectList")
    public Result<List<WindTurbine>> selectList(@RequestParam Map <String, Object> map) {
        List<WindTurbine> list = windTurbineService.selectList(map);
        flyToFront.flyToFront("风机H",0.0);
        return Result.success(list);
    }

}
