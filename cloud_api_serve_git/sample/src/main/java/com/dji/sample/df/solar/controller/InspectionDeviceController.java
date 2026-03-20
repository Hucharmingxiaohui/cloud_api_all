package com.dji.sample.df.solar.controller;

import com.df.framework.vo.Result;
import com.dji.sample.df.solar.model.entity.InspectionDevice;
import com.dji.sample.df.solar.service.InspectionDeviceService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 巡视设备控制器
 */
@RestController
@RequestMapping("/api/inspectionDevice")
public class InspectionDeviceController {

    @Resource
    private InspectionDeviceService inspectionDeviceService;

    /**
     * 新增巡视设备
     */
    @PostMapping("/save")
    public Result save(@RequestBody InspectionDevice inspectionDevice) {
        boolean success = inspectionDeviceService.saveInspectionDevice(inspectionDevice);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 根据ID更新巡视设备
     */
    @PostMapping("/update")
    public Result update(@RequestBody InspectionDevice inspectionDevice) {
        boolean success = inspectionDeviceService.updateInspectionDeviceById(inspectionDevice);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 根据ID删除巡视设备
     */
    @GetMapping("/delete")
    public Result delete(@RequestParam Long id) {
        boolean success = inspectionDeviceService.removeInspectionDeviceById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 根据ID查询巡视设备
     */
    @GetMapping("/getById")
    public Result<InspectionDevice> getById(@RequestParam Long id) {
        InspectionDevice inspectionDevice = inspectionDeviceService.getInspectionDeviceById(id);
        return inspectionDevice != null ? Result.success(inspectionDevice) : Result.error("数据不存在");
    }

    /**
     * 分页/条件查询巡视设备列表
     */
    @GetMapping("/selectList")
    public Result<Map> selectList(@RequestParam Map<String, Object> params) {
        Map<String, Object> resultMap = inspectionDeviceService.selectList(params);
        return Result.success(resultMap);
    }
}
