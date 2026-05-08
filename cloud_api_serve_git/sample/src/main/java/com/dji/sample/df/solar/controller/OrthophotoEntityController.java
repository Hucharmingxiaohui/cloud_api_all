package com.dji.sample.df.solar.controller;

import com.alibaba.fastjson.JSONArray;
import com.df.framework.vo.Result;
import com.dji.sample.df.solar.model.entity.OrthophotoEntity;
import com.dji.sample.df.solar.service.OrthophotoEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;


@RestController
@RequestMapping("/api/Orthophoto")
public class OrthophotoEntityController {

    @Autowired
    private OrthophotoEntityService orthophotoEntityService;

    /**
     *  导入正射图
     */
    @PostMapping("/import")
    public Result<OrthophotoEntity> importOrthophoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name) {
        OrthophotoEntity entity = orthophotoEntityService.importOrthophoto(file, name);
        return Result.success(entity);
    }


    /**
     * 分页/条件查询正射图列表
     */
    @GetMapping("/selectList")
    public Result<Map> selectList(@RequestParam Map<String, Object> params) {
        Map<String, Object> resultMap = orthophotoEntityService.selectList(params);
        return Result.success(resultMap);
    }

    /**
     * 删除正射图
     */
    @GetMapping("/deleteOrthophoto")
    public Result<Void> deleteOrthophoto(@RequestParam String id) {
        orthophotoEntityService.deleteOrthophoto(id);
        return Result.success();
    }

    /**
     * 根据id查正射图
     */
    @GetMapping("/selectById")
    public Result<Void> selectById(@RequestParam String id) {
        OrthophotoEntity orthophotoEntity = orthophotoEntityService.selectById(id);
        return Result.success(orthophotoEntity);
    }

    /**
     * 根据正射图id查光伏组件树
     */
    @GetMapping("/selectComponentsById")
    public Result<Void> selectComponentsById(@RequestParam String id) {
        JSONArray objects = orthophotoEntityService.selectComponentsById(id);
        return Result.success(objects);
    }

}
