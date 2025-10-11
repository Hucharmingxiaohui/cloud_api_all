package com.dji.sample.df.patrolDf.controller.uni;


import com.df.framework.enums.OperLogTypeEnum;
import com.df.framework.utils.ConvertUtils;
import com.df.framework.utils.ParamsUtils;
import com.df.framework.vo.Result;
import com.df.server.dto.UniArea.UniAreaAddDTO;
import com.df.server.dto.UniArea.UniAreaUpdateDTO;
import com.df.server.entity.uni.UniAreaEntity;
import com.df.server.service.uni.UniAreaService;
import com.df.server.utils.HisLogBusinessUtils;
import com.df.server.vo.UniArea.UniAreaVO;
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
 * 区域接口
 * <p>
 * Created by lianyc on 2023-11-09
 */
@RestController
@RequestMapping("/uniArea")
@AuthInterceptor.IgnoreAuth
public class UniAreaController {

    private static final String LOG_TITLE = "点位分级管理";

    @Autowired
    private UniAreaService uniAreaService;

    /**
     * 新增记录
     *
     * @param addDTO
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody UniAreaAddDTO addDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(addDTO, "areaName", "subCode");
        String log = "新增区域：" + addDTO.getAreaName();
        String subCode = addDTO.getSubCode();
        try {
            UniAreaEntity entity = ConvertUtils.toBean(addDTO, UniAreaEntity.class);
            entity.setAreaId(UUID.randomUUID().toString().replace("-", ""));
            uniAreaService.save(entity);
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
    public Result update(@RequestBody UniAreaUpdateDTO updateDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(updateDTO, "id", "areaName", "subCode");
        String log = "更新区域：" + updateDTO.getAreaName();
        String subCode = updateDTO.getSubCode();
        try {
            UniAreaEntity entity = ConvertUtils.toBean(updateDTO, UniAreaEntity.class);
            entity.setAreaId(null);
            entity.setSubCode(null);
            uniAreaService.updateById(entity);
            HisLogBusinessUtils.recordSuccess(subCode, LOG_TITLE, log, OperLogTypeEnum.UPDATE.getType(), request);
            return Result.success();
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(e, subCode, LOG_TITLE, log, OperLogTypeEnum.UPDATE.getType(), request);
        }
    }

    /**
     * 根据编码查询
     *
     * @return
     */
    @PostMapping("/getByAreaId")
    public Result<UniAreaVO> getByAreaId(@RequestBody UniAreaUpdateDTO addDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(addDTO, "areaId", "subCode");
        UniAreaEntity entity = uniAreaService.getByAreaId(addDTO.getSubCode(), addDTO.getAreaId());
        UniAreaVO vo = ConvertUtils.toBean(entity, UniAreaVO.class);
        return Result.success(vo);
    }

    /**
     * 根据变电站编码查询
     *
     * @return
     */
    @PostMapping("/listBySubCode")
    public Result<List<UniAreaVO>> listBySubCode(@RequestBody UniAreaUpdateDTO addDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(addDTO, "subCode");
        List<UniAreaVO> list = uniAreaService.listBySubCode(addDTO.getSubCode());
        return Result.success(list);
    }

    /**
     * 删除记录
     *
     * @return
     */
    @PostMapping("/delete")
    public Result delete(@RequestBody UniAreaUpdateDTO updateDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(updateDTO, "id", "areaName", "subCode");
        String log = "删除区域：" + updateDTO.getAreaName();
        String subCode = updateDTO.getSubCode();
        try {
            uniAreaService.deleteById(updateDTO.getId());
            HisLogBusinessUtils.recordSuccess(subCode, LOG_TITLE, log, OperLogTypeEnum.DELETE.getType(), request);
            return Result.success();
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(e, subCode, LOG_TITLE, log, OperLogTypeEnum.DELETE.getType(), request);
        }
    }
}

