package com.dji.sample.df.indoor.controller;

import com.df.framework.vo.Result;
import com.dji.sample.df.indoor.model.dto.PointBindingRequest;
import com.dji.sample.df.indoor.model.dto.PointBindingResponse;
import com.dji.sample.df.indoor.service.PointBindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
@Validated
public class PointBindingController {

    private final PointBindingService pointBindingService;

    // 新增点位
    @PostMapping("/add")
    public Result createPoint(@Valid @RequestBody PointBindingRequest request) {
        PointBindingResponse response = pointBindingService.addPoint(request);
        return Result.success(response);
    }

    // 修改点位
    @PostMapping("/upt")
    public Result updatePoint(@RequestParam String id, @Valid @RequestBody PointBindingRequest request) {
        PointBindingResponse response = pointBindingService.updatePoint(id, request);
        return Result.success(response);
    }

    // 删除点位
    @GetMapping("/delete")
    public Result deletePoint(@RequestParam String id) {
        boolean b = pointBindingService.deletePoint(id);
        if (b) {
            return Result.success("删除成功");
        }else  {
            return Result.error("删除失败");
        }

    }

    // 查询单个点位
    @GetMapping("/getById")
    public Result getById(@RequestParam String id) {
        PointBindingResponse response = pointBindingService.getPointById(id);
        return Result.success(response);
    }

    // 查询所有点位
    @GetMapping
    public Result getAllPoints() {
        List<PointBindingResponse> list = pointBindingService.getAllPoints();
        return Result.success(list);
    }
}
