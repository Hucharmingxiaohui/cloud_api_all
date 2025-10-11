package com.dji.sample.df.patrolDf.controller.uni;


import com.df.framework.enums.OperLogTypeEnum;
import com.df.framework.utils.ConvertUtils;
import com.df.framework.utils.PageUtils;
import com.df.framework.utils.ParamsUtils;
import com.df.framework.vo.PageVO;
import com.df.framework.vo.Result;
import com.df.server.dto.HisUniTask.HisUniTaskPageDTO;
import com.df.server.dto.UniTaskPlan.UniTaskPlanAddDTO;
import com.df.server.dto.UniTaskPlan.UniTaskPlanPageDTO;
import com.df.server.dto.UniTaskPlan.UniTaskPlanParamsDTO;
import com.df.server.dto.UniTaskPlan.UniTaskPlanUpdateDTO;
import com.df.server.dto.UniTaskPlanItemPoint.UniTaskPlanItemPointPageDTO;
import com.df.server.entity.uni.UniTaskPlanEntity;
import com.df.server.service.his.HisUniTaskService;
import com.df.server.service.uni.UniTaskPlanItemPointService;
import com.df.server.service.uni.UniTaskPlanService;
import com.df.server.utils.HisLogBusinessUtils;
import com.df.server.vo.UniTaskPlan.UniTaskPlanVO;
import com.df.server.vo.UniTaskPlanItemPoint.UniTaskPlanItemPointVO;
import com.df.server.vo.his.HisUniTaskVO;
import com.dji.sample.component.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 巡检方案接口
 * <p>
 * Created by lianyc on 2025-05-20
 */
@RestController
@RequestMapping("/uniTaskPlan")
@AuthInterceptor.IgnoreAuth
public class UniTaskPlanController {

    private static final String LOG_TITLE = "";

    @Autowired
    private UniTaskPlanService uniTaskPlanService;
    @Autowired
    private UniTaskPlanItemPointService uniTaskPlanItemPointService;
    @Autowired
    private HisUniTaskService hisUniTaskService;

    /**
     * 分页查询
     *
     * @param pageDTO
     * @return
     */
    @PostMapping("/page")
    public Result<PageVO<UniTaskPlanVO>> getPageCustom(@RequestBody UniTaskPlanPageDTO pageDTO, HttpServletRequest request) {
        PageUtils.pageParamsExtend(pageDTO);
        PageVO<UniTaskPlanVO> page = uniTaskPlanService.customPage(pageDTO);
        return Result.success(page);
    }

    /**
     * 新增记录
     *
     * @param addDTO
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody UniTaskPlanAddDTO addDTO, HttpServletRequest request) {
        String logInfo = "";
        ParamsUtils.isBlank(addDTO,
                "subCode", "planName", "planType", "taskType", "deviceLevel", "deviceList"
                , "priority", "isenable", "executeType", "isSecondAnalyse");
        String subCode = addDTO.getSubCode();
        try {
            UniTaskPlanEntity entity = ConvertUtils.toBean(addDTO, UniTaskPlanEntity.class);
            uniTaskPlanService.saveNewPlan(entity);
            HisLogBusinessUtils.recordSuccess(
                    subCode, LOG_TITLE, logInfo, OperLogTypeEnum.ADD.getType(), request);
            return Result.success();
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(
                    e, subCode, LOG_TITLE, logInfo, OperLogTypeEnum.ADD.getType(), request);
        }
    }

    /**
     * 修改记录
     *
     * @param updateDTO
     * @return
     */
    @PostMapping("/update")
    public Result update(@RequestBody UniTaskPlanUpdateDTO updateDTO, HttpServletRequest request) {
        String logInfo = "";
        ParamsUtils.isBlank(updateDTO,
                "id", "subCode", "sysCode", "planNo", "planName", "planType", "taskType", "deviceLevel", "deviceList"
                , "priority", "isenable", "executeType", "isSecondAnalyse", "createSource");
        String subCode = updateDTO.getSubCode();
        try {
            UniTaskPlanEntity entity = ConvertUtils.toBean(updateDTO, UniTaskPlanEntity.class);
            uniTaskPlanService.updatePlan(entity);
            HisLogBusinessUtils.recordSuccess(
                    subCode, LOG_TITLE, logInfo, OperLogTypeEnum.UPDATE.getType(), request);
            return Result.success();
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(
                    e, subCode, LOG_TITLE, logInfo, OperLogTypeEnum.UPDATE.getType(), request);
        }
    }

    /**
     * 根据编号查询
     *
     * @param paramsDTO
     * @return
     */
    @PostMapping("/get")
    public Result<UniTaskPlanVO> getInfoByParams(@RequestBody UniTaskPlanParamsDTO paramsDTO, HttpServletRequest request) {
        ParamsUtils.isNull(paramsDTO, "planNo");
        UniTaskPlanVO vo = uniTaskPlanService.getInfoByParams(paramsDTO);
        return Result.success(vo);
    }

    /**
     * 删除记录
     *
     * @param paramsDTO
     * @return
     */
    @PostMapping("/delete")
    public Result delete(@RequestBody UniTaskPlanParamsDTO paramsDTO, HttpServletRequest request) {
        ParamsUtils.isNull(paramsDTO, "subCode", "planNo");
        String subCode = paramsDTO.getSubCode();
        String logInfo = "";
        try {
            uniTaskPlanService.deleteByParams(paramsDTO);
            HisLogBusinessUtils.recordSuccess(
                    subCode, LOG_TITLE, logInfo, OperLogTypeEnum.DELETE.getType(), request);
            return Result.success();
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(
                    e, subCode, LOG_TITLE, logInfo, OperLogTypeEnum.DELETE.getType(), request);
        }
    }

    /**
     * 禁用 启用
     *
     * @param paramsDTO
     * @return
     */
    @PostMapping("/enableByPlanNo")
    public Result enableByPlanNo(@RequestBody UniTaskPlanParamsDTO paramsDTO, HttpServletRequest request) {
        ParamsUtils.isNull(paramsDTO, "planNo", "isenable");
        uniTaskPlanService.enableByPlanNo(paramsDTO);
        return Result.success();
    }

    /**
     * 审核
     *
     * @param paramsDTO
     * @return
     */
    @PostMapping("/auditByPlanNo")
    public Result auditByPlanNo(@RequestBody UniTaskPlanParamsDTO paramsDTO, HttpServletRequest request) {
        ParamsUtils.isNull(paramsDTO, "planNo", "isaudit");
        uniTaskPlanService.auditByPlanNo(paramsDTO);
        return Result.success();
    }

    /**
     * 任务点位分页
     *
     * @param paramsDTO
     * @return
     */
    @PostMapping("/pagePlanPoint")
    public Result<PageVO<UniTaskPlanItemPointVO>> pagePlanPoint(@RequestBody UniTaskPlanItemPointPageDTO paramsDTO, HttpServletRequest request) {
        PageUtils.pageParamsExtend(paramsDTO);
        ParamsUtils.isNull(paramsDTO, "planNo");
        PageVO<UniTaskPlanItemPointVO> page = uniTaskPlanItemPointService.customPage(paramsDTO);
        return Result.success(page);
    }

    /**
     * 执行记录
     *
     * @param paramsDTO
     * @return
     */
    @PostMapping("/pageHisTask")
    public Result<PageVO<HisUniTaskVO>> pageHisTask(@RequestBody HisUniTaskPageDTO paramsDTO, HttpServletRequest request) {
        PageUtils.pageParamsExtend(paramsDTO);
        ParamsUtils.isNull(paramsDTO, "planNo");
        PageVO<HisUniTaskVO> pageVO = hisUniTaskService.getPageList(paramsDTO);
        return Result.success(pageVO);
    }

    /**
     * 立即执行
     *
     * @param paramsDTO
     * @return
     */
    @PostMapping("/executeNow")
    public Result executeNow(@RequestBody UniTaskPlanParamsDTO paramsDTO, HttpServletRequest request) {
        ParamsUtils.isNull(paramsDTO, "planNo");
        UniTaskPlanEntity uniTaskPlanEntity = uniTaskPlanService.lambdaQuery().eq(UniTaskPlanEntity::getPlanNo, paramsDTO.getPlanNo()).one();
        if (uniTaskPlanEntity.getIsenable() != null && uniTaskPlanEntity.getIsenable() == 1) {
            return Result.error("任务已经停用，不能执行");
        }
        hisUniTaskService.executePlan(uniTaskPlanEntity, 1);
        return Result.success();
    }
}

