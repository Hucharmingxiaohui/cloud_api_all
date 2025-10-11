package com.dji.sample.df.patrolDf.controller.his;

import com.df.framework.utils.PageUtils;
import com.df.framework.vo.PageVO;
import com.df.framework.vo.Result;
import com.df.server.dto.HisUniTaskItemAlarm.HisUniTaskItemAlarmPageDTO;
import com.df.server.service.his.HisUniTaskItemAlarmService;
import com.df.server.vo.HisUniTaskItemAlarm.HisUniTaskItemAlarmVO;
import com.dji.sample.component.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 机器人巡检告警接口
 * <p>
 * Created by lianyc on 2025-05-23
 */
@RestController
@RequestMapping("/hisUniTaskItemAlarm")
@AuthInterceptor.IgnoreAuth
public class HisUniTaskItemAlarmController {

    private static final String LOG_TITLE = "";

    @Autowired
    private HisUniTaskItemAlarmService hisUniTaskItemAlarmService;

    /**
     * 分页查询
     *
     * @param pageDTO
     * @return
     */
    @PostMapping("/page")
    public Result<PageVO<HisUniTaskItemAlarmVO>> getPageCustom(@RequestBody HisUniTaskItemAlarmPageDTO pageDTO, HttpServletRequest request) {
        PageUtils.pageParamsExtend(pageDTO);
        PageVO<HisUniTaskItemAlarmVO> page = hisUniTaskItemAlarmService.customPage(pageDTO);
        return Result.success(page);
    }

}

