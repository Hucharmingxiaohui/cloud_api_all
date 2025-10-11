package com.dji.sample.df.patrolDf.controller.uni;


import com.df.framework.enums.OperLogTypeEnum;
import com.df.framework.exception.FastException;
import com.df.framework.utils.ConvertUtils;
import com.df.framework.utils.ParamsUtils;
import com.df.framework.vo.Result;
import com.df.server.dto.UniBay.UniBayAddDTO;
import com.df.server.dto.UniBay.UniBayUpdateDTO;
import com.df.server.entity.uni.UniAreaEntity;
import com.df.server.entity.uni.UniBayEntity;
import com.df.server.service.uni.UniAreaService;
import com.df.server.service.uni.UniBayService;
import com.df.server.utils.HisLogBusinessUtils;
import com.df.server.vo.UniBay.UniBayVO;
import com.dji.sample.component.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

/**
 * 间隔接口
 * <p>
 * Created by lianyc on 2023-11-09
 */
@RestController
@RequestMapping("/uniBay")
@AuthInterceptor.IgnoreAuth
public class UniBayController {

    private static final String LOG_TITLE = "点位分级管理";

    @Autowired
    private UniBayService uniBayService;
    @Autowired
    private UniAreaService uniAreaService;


    /**
     * 新增记录
     *
     * @param addDTO
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody UniBayAddDTO addDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(addDTO, "areaId", "subCode", "bayName");
        String log = "新增间隔：" + addDTO.getBayName();
        String subCode = addDTO.getSubCode();
        try {
            UniAreaEntity byAreaId = uniAreaService.getByAreaId(addDTO.getSubCode(), addDTO.getAreaId());
            if (byAreaId == null) {
                throw new FastException("区域不存在，检查提交的区域编码");
            }
            UniBayEntity entity = ConvertUtils.toBean(addDTO, UniBayEntity.class);
            entity.setBayId(UUID.randomUUID().toString().replace("-", ""));
            uniBayService.save(entity);
            HisLogBusinessUtils.recordSuccess(subCode, LOG_TITLE, log, OperLogTypeEnum.ADD.getType(), request);
            return Result.success();
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(e, subCode, LOG_TITLE, log, OperLogTypeEnum.ADD.getType(), request);
        }
    }

    /**
     * 修改记录
     *
     * @param updateDTO
     * @return
     */
    @PostMapping("/update")
    public Result update(@RequestBody UniBayUpdateDTO updateDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(updateDTO, "subCode", "bayName", "id", "areaId");
        String log = "更新间隔：" + updateDTO.getBayName();
        String subCode = updateDTO.getSubCode();
        try {
            UniAreaEntity byAreaId = uniAreaService.getByAreaId(updateDTO.getSubCode(), updateDTO.getAreaId());
            if (byAreaId == null) {
                throw new FastException("区域不存在，检查提交的区域编码");
            }
            UniBayEntity entity = ConvertUtils.toBean(updateDTO, UniBayEntity.class);
            entity.setSubCode(null);
            entity.setAreaId(null);
            entity.setBayId(null);
            uniBayService.updateById(entity);
            HisLogBusinessUtils.recordSuccess(subCode, LOG_TITLE, log, OperLogTypeEnum.UPDATE.getType(), request);
            return Result.success();
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(e, subCode, LOG_TITLE, log, OperLogTypeEnum.UPDATE.getType(), request);
        }
    }

    /**
     * 根据编码查询
     *
     * @param updateDTO
     * @return
     */
    @PostMapping("/getByBayId")
    public Result<UniBayVO> getByBayId(@RequestBody UniBayUpdateDTO updateDTO) {
        ParamsUtils.isBlank(updateDTO, "subCode", "bayId");
        UniBayEntity entity = uniBayService.getByBayId(updateDTO.getSubCode(), updateDTO.getBayId());
        UniBayVO vo = ConvertUtils.toBean(entity, UniBayVO.class);
        return Result.success(vo);
    }

    /**
     * 根据区域查询
     *
     * @param updateDTO
     * @return
     */
    @PostMapping("/listByAreaId")
    public Result<List<UniBayVO>> listByAreaId(@RequestBody UniBayUpdateDTO updateDTO) {
        ParamsUtils.isBlank(updateDTO, "subCode", "areaId");
        List<UniBayVO> list = uniBayService.listByAreaId(updateDTO.getSubCode(), updateDTO.getAreaId());
        return Result.success(list);
    }

    /**
     * 删除记录
     *
     * @param updateDTO
     * @return
     */
    @PostMapping("/delete")
    public Result delete(@RequestBody UniBayUpdateDTO updateDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(updateDTO, "id", "subCode", "bayName");
        String log = "删除间隔：" + updateDTO.getBayName();
        String subCode = updateDTO.getSubCode();
        try {
            uniBayService.deleteBay(updateDTO.getId());
            HisLogBusinessUtils.recordSuccess(subCode, LOG_TITLE, log, OperLogTypeEnum.DELETE.getType(), request);
            return Result.success();
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(e, subCode, LOG_TITLE, log, OperLogTypeEnum.DELETE.getType(), request);
        }
    }
}

