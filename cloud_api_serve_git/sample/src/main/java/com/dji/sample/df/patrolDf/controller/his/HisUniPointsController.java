package com.dji.sample.df.patrolDf.controller.his;

import com.df.framework.utils.PageUtils;
import com.df.framework.utils.ParamsUtils;
import com.df.framework.vo.PageVO;
import com.df.framework.vo.Result;
import com.df.server.dto.HisUniTaskItemPoints.HisPointsHandleDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisUniTaskItemPointsPageDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisUniTaskItemPointsParamsDTO;
import com.df.server.service.his.HisUniTaskItemPointsService;
import com.df.server.vo.his.HisUniTaskItemPointsVO;
import com.dji.sample.component.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 巡检点位结果查询
 *
 * @author 平善闯
 * @date 2025-04-20 22:58
 */
@RestController
@RequestMapping("/his/points")
@AuthInterceptor.IgnoreAuth
public class HisUniPointsController {
    @Autowired
    private HisUniTaskItemPointsService hisUniTaskItemPointsService;

    /**
     * 巡检点位结果分页查询
     *
     * @param hisUniPointsDTO
     * @return
     */
    @PostMapping("/page")
    public Result<PageVO<HisUniTaskItemPointsVO>> page(@RequestBody HisUniTaskItemPointsPageDTO hisUniPointsDTO) {
        PageUtils.pageParamsExtend(hisUniPointsDTO);
        ParamsUtils.isBlank(hisUniPointsDTO, "taskPatrolledId");
        PageVO<HisUniTaskItemPointsVO> hisPointsVOPageVO = hisUniTaskItemPointsService.getHisPoints(hisUniPointsDTO);
        return Result.success(hisPointsVOPageVO);
    }

    /**
     * 巡检点位结果详情查询
     *
     * @param paramsDTO
     * @return
     */
    @PostMapping("/detail")
    public Result<HisUniTaskItemPointsVO> getHisPointDetail(@RequestBody HisUniTaskItemPointsParamsDTO paramsDTO) {
        ParamsUtils.isBlank(paramsDTO, "taskPatrolledId", "pointCode");
        HisUniTaskItemPointsVO hisPointDetail = hisUniTaskItemPointsService.getHisPointDetail(paramsDTO);
        return Result.success(hisPointDetail);
    }

    /**
     * 人工修正
     *
     * @param params
     * @return
     */
    @PostMapping("/abnormalHandle")
    public Result abnormalHandle(@RequestBody HisPointsHandleDTO params) {
        ParamsUtils.isBlank(params, "taskPatrolledId", "pointCode", "setVal");
        hisUniTaskItemPointsService.abnormalHandle(params);
        return Result.success();
    }

    /**
     * 结果确认 一键确认只传taskPatrolledId，单个结果确认 加pointCode
     *
     * @param params
     * @return
     */
    @PostMapping("/confirm")
    public Result confirm(@RequestBody HisPointsHandleDTO params) {
        ParamsUtils.isBlank(params, "taskPatrolledId");
        hisUniTaskItemPointsService.confirm(params);
        return Result.success();
    }

    /**
     * 告警确认
     *
     * @param params
     * @return
     */
    @PostMapping("/confirmAlarm")
    public Result confirmAlarm(@RequestBody HisPointsHandleDTO params) {
        ParamsUtils.isBlank(params, "taskPatrolledId", "pointCode", "confirmResult");
        ParamsUtils.isNull(params, "alarmConfirmComment");
        hisUniTaskItemPointsService.confirmAlarm(params);
        return Result.success();
    }


}
