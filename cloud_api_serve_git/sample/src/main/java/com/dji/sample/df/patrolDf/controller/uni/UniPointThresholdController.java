package com.dji.sample.df.patrolDf.controller.uni;


import com.df.framework.utils.ParamsUtils;
import com.df.framework.vo.Result;
import com.df.server.dto.UniPointThreshold.UniPointThresholdAddDTO;
import com.df.server.dto.uniPoint.PointDetailDTO;
import com.df.server.service.uni.UniPointThresholdService;
import com.df.server.vo.UniPointThreshold.PointAlarmThresholdVO;
import com.dji.sample.component.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 点位告警阈值接口
 * <p>
 * Created by lianyc on 2025-05-19
 */
@RestController
@RequestMapping("/uniPointThreshold")
@AuthInterceptor.IgnoreAuth
public class UniPointThresholdController {

    private static final String LOG_TITLE = "";

    @Autowired
    private UniPointThresholdService uniPointThresholdService;

    /**
     * 配置管理-巡检点位管理-点位列表-点击告警阈值配置弹窗获取的告警信息
     */
    @PostMapping("/getPointThreshold")
    @ResponseBody
    public Result<PointAlarmThresholdVO> getPointRelAlarmThresholdInfo(@RequestBody PointDetailDTO dto) {
        try {
            PointAlarmThresholdVO pointRelAlarmThreshold = uniPointThresholdService.getPointRelAlarmThresholdInfo(dto);
            return Result.success(pointRelAlarmThreshold);
        } catch (Exception ex) {
            return Result.error("请求异常");
        }
    }

    /**
     * 修改点位告警阈值
     */
    @PostMapping(value = {"/modify"})
    @ResponseBody
    public Result modify(@RequestBody UniPointThresholdAddDTO addDTO) {
        ParamsUtils.isBlank(addDTO, "subCode", "pointCode");
        uniPointThresholdService.modifyThreshold(addDTO);
        return Result.success();
    }

    /**
     * 删除点位告警阈值
     */
    @PostMapping(value = {"/delete"})
    @ResponseBody
    public Result delete(@RequestBody PointDetailDTO pointDetailDTO) {
        ParamsUtils.isBlank(pointDetailDTO, "subCode", "pointCode");
        uniPointThresholdService.delete(pointDetailDTO);
        return Result.success();
    }
}

