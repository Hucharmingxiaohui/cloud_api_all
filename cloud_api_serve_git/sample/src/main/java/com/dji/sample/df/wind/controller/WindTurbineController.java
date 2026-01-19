package com.dji.sample.df.wind.controller;

import com.df.framework.vo.Result;
import com.dji.sample.df.wind.handler.PictureSaveHandler;
import com.dji.sample.df.wind.model.entity.FanStationPoints;
import com.dji.sample.df.wind.model.entity.WindTurbine;
import com.dji.sample.df.wind.service.FjReportService;
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
    PictureSaveHandler pictureSaveHandler;

    @Resource
    FjReportService fjReportService;

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
     * 查询风机列表
     */
    @GetMapping("selectList")
    public Result<Map> selectList(@RequestParam Map <String, Object> map) {
        Map<String, Object> stringObjectMap = windTurbineService.selectList(map);
        return Result.success(stringObjectMap);
    }

    /**
     * 根据ID增加风机点位模型
     */
    @GetMapping("/addPointsById")
    public Result addPointsById(@RequestParam String id) {
        boolean b = windTurbineService.addPointsById(id);

        return Result.success(b);
    }

    /**
     * 根据ID获取风机点位模型
     */
    @GetMapping("/getPointsById")
    public Result<Map> getPointsById(@RequestParam Map <String, Object> map) {
        Map<String, Object> pointsList = windTurbineService.getPointsByFanId(map);

        return Result.success(pointsList);
    }

    /**
     * 根据id重置风机点位模型
     */
    @GetMapping("/deletePointsById")
    public Result<Map> deletePointsById(@RequestParam Map <String, Object> map) {
        int i = windTurbineService.deletePointsById(map);
        if (i > 0) {
            return Result.success("删除成功");
        }else {
            return Result.error("删除失败");
        }
    }



}
