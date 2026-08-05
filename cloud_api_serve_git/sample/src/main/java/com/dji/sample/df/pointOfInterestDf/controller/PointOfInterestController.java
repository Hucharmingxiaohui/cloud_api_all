package com.dji.sample.df.pointOfInterestDf.controller;

import com.df.framework.vo.Result;
import com.dji.sample.df.pointOfInterestDf.model.PointOfInterest;
import com.dji.sample.df.windDf.service.PointOfInterestService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/pointOfInterest")
public class PointOfInterestController {

    @Resource
    private PointOfInterestService pointOfInterestService;

    /**
     * 新增兴趣点
     */
    @PostMapping("/save")
    public Result save(@RequestBody PointOfInterest pointOfInterest) {
        boolean success = pointOfInterestService.savePointOfInterest(pointOfInterest);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 根据ID更新兴趣点
     */
    @PostMapping("/update")
    public Result update(@RequestBody PointOfInterest pointOfInterest) {
        boolean success = pointOfInterestService.updatePointOfInterestById(pointOfInterest);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 根据ID删除兴趣点
     */
    @GetMapping("/delete")
    public Result delete(@RequestParam String id) {
        boolean success = pointOfInterestService.removePointOfInterestById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 根据ID查询兴趣点
     */
    @GetMapping("/getById")
    public Result<PointOfInterest> getById(@RequestParam String id) {
        PointOfInterest pointOfInterest = pointOfInterestService.getPointOfInterestById(id);
        return pointOfInterest != null ? Result.success(pointOfInterest) : Result.error("数据不存在");
    }

    /**
     * 查询兴趣点列表
     */
    @GetMapping("/selectList")
    public Result<Map> selectList(@RequestParam Map<String, Object> map) {
        Map<String, Object> result = pointOfInterestService.selectList(map);
        return Result.success(result);
    }
}
