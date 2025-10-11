package com.dji.sample.df.patrolDf.controller.uni;


import com.df.framework.enums.OperLogTypeEnum;
import com.df.framework.exception.FastException;
import com.df.framework.utils.ConvertUtils;
import com.df.framework.utils.ParamsUtils;
import com.df.framework.vo.Result;
import com.df.server.dto.UniDevice.UniDeviceAddDTO;
import com.df.server.dto.UniDevice.UniDeviceUpdateDTO;
import com.df.server.entity.uni.UniBayEntity;
import com.df.server.entity.uni.UniComponentEntity;
import com.df.server.entity.uni.UniDeviceEntity;
import com.df.server.entity.uni.UniPointEntity;
import com.df.server.service.uni.UniBayService;
import com.df.server.service.uni.UniComponentService;
import com.df.server.service.uni.UniDeviceService;
import com.df.server.service.uni.UniPointService;
import com.df.server.utils.HisLogBusinessUtils;
import com.df.server.vo.UniDevice.UniDeviceVO;
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
 * 主设备接口
 * <p>
 * Created by lianyc on 2023-11-09
 */
@RestController
@RequestMapping("/uniDevice")
@AuthInterceptor.IgnoreAuth
public class UniDeviceController {

    private static final String LOG_TITLE = "点位分级管理";

    @Autowired
    private UniDeviceService uniDeviceService;
    @Autowired
    private UniBayService uniBayService;
    @Autowired
    private UniComponentService uniComponentService;
    @Autowired
    private UniPointService uniPointService;


    /**
     * 新增记录
     *
     * @param addDTO
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody UniDeviceAddDTO addDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(addDTO, "deviceName", "subCode", "deviceType", "bayId");
        String log = "新增设备：" + addDTO.getDeviceName();
        String subCode = addDTO.getSubCode();
        try {
            UniBayEntity bay = uniBayService.getByBayId(subCode, addDTO.getBayId());
            if (bay == null) {
                throw new FastException("间隔不存在");
            }
            UniDeviceEntity entity = ConvertUtils.toBean(addDTO, UniDeviceEntity.class);
            bay.setBayId(UUID.randomUUID().toString().replace("-", ""));
            uniDeviceService.save(entity);
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
    public Result update(@RequestBody UniDeviceUpdateDTO updateDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(updateDTO, "id", "deviceName", "subCode", "deviceType");
        String log = "更新设备：" + updateDTO.getDeviceName();
        String subCode = updateDTO.getSubCode();
        try {
            UniBayEntity bay = uniBayService.getByBayId(subCode, updateDTO.getBayId());
            if (bay == null) {
                throw new FastException("间隔不存在");
            }
            UniDeviceEntity entity = ConvertUtils.toBean(updateDTO, UniDeviceEntity.class);
            entity.setSubCode(null);
            entity.setDeviceId(null);
            entity.setBayId(null);
            uniDeviceService.updateById(entity);
            HisLogBusinessUtils.recordSuccess(subCode, LOG_TITLE, log, OperLogTypeEnum.UPDATE.getType(), request);
            return Result.success();
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(e, subCode, LOG_TITLE, log, OperLogTypeEnum.UPDATE.getType(), request);
        }
    }

    /**
     * 根据id查询
     *
     * @param updateDTO
     * @return
     */
    @PostMapping("/getByDeviceId")
    public Result<UniDeviceVO> getByDeviceId(@RequestBody UniDeviceUpdateDTO updateDTO) {
        ParamsUtils.isBlank(updateDTO, "subCode", "deviceId");
        UniDeviceEntity entity = uniDeviceService.getByDeviceId(updateDTO.getSubCode(), updateDTO.getDeviceId());
        UniDeviceVO vo = ConvertUtils.toBean(entity, UniDeviceVO.class);
        return Result.success(vo);
    }

    /**
     * 根据间隔查询
     *
     * @param updateDTO
     * @return
     */
    @PostMapping("/listByBayId")
    public Result<List<UniDeviceVO>> listByBayId(@RequestBody UniDeviceUpdateDTO updateDTO) {
        ParamsUtils.isBlank(updateDTO, "subCode", "bayId");
        List<UniDeviceVO> list = uniDeviceService.listByBayId(updateDTO.getSubCode(), updateDTO.getBayId());
        return Result.success(list);
    }

    /**
     * 删除记录
     *
     * @return
     */
    @PostMapping("/delete")
    public Result delete(@RequestBody UniDeviceUpdateDTO updateDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(updateDTO, "id", "subCode", "deviceName");
        String log = "删除设备：" + updateDTO.getDeviceName();
        String subCode = updateDTO.getSubCode();
        try {
            UniDeviceEntity device = uniDeviceService.getById(updateDTO.getId());
            if (device != null) {
                //删除点位
                uniPointService.lambdaUpdate()
                        .eq(UniPointEntity::getSubCode, subCode)
                        .eq(UniPointEntity::getDeviceId, device.getDeviceId())
                        .remove();
                //删除部件
                uniComponentService.lambdaUpdate()
                        .eq(UniComponentEntity::getSubCode, subCode)
                        .eq(UniComponentEntity::getDeviceId, device.getDeviceId())
                        .remove();
                //删除设备
                uniDeviceService.removeById(updateDTO.getId());
            }
            HisLogBusinessUtils.recordSuccess(subCode, LOG_TITLE, log, OperLogTypeEnum.DELETE.getType(), request);
            return Result.success();
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(e, subCode, LOG_TITLE, log, OperLogTypeEnum.DELETE.getType(), request);
        }
    }
}

