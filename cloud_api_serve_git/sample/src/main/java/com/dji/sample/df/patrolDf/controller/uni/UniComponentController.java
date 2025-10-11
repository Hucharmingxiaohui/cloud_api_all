package com.dji.sample.df.patrolDf.controller.uni;


import com.df.framework.enums.OperLogTypeEnum;
import com.df.framework.exception.FastException;
import com.df.framework.utils.ConvertUtils;
import com.df.framework.utils.ParamsUtils;
import com.df.framework.vo.Result;
import com.df.server.dto.UniComponent.UniComponentAddDTO;
import com.df.server.dto.UniComponent.UniComponentUpdateDTO;
import com.df.server.entity.uni.UniComponentEntity;
import com.df.server.entity.uni.UniDeviceEntity;
import com.df.server.entity.uni.UniPointEntity;
import com.df.server.service.uni.UniComponentService;
import com.df.server.service.uni.UniDeviceService;
import com.df.server.service.uni.UniPointService;
import com.df.server.utils.HisLogBusinessUtils;
import com.df.server.vo.UniComponent.UniComponentVO;
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
 * 部件接口
 * <p>
 * Created by lianyc on 2023-11-09
 */
@RestController
@RequestMapping("/uniComponent")
@AuthInterceptor.IgnoreAuth
public class UniComponentController {

    private static final String LOG_TITLE = "部件接口";

    @Autowired
    private UniComponentService uniComponentService;
    @Autowired
    private UniDeviceService uniDeviceService;
    @Autowired
    private UniPointService uniPointService;


    /**
     * 新增记录
     *
     * @param addDTO
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody UniComponentAddDTO addDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(addDTO, "subCode", "componentName", "deviceId");
        String log = "新增部件：" + addDTO.getComponentName();
        String subCode = addDTO.getSubCode();
        try {
            UniDeviceEntity device = uniDeviceService.getByDeviceId(subCode, addDTO.getDeviceId());
            if (device == null) {
                throw new FastException("设备不存在");
            }
            UniComponentEntity entity = ConvertUtils.toBean(addDTO, UniComponentEntity.class);
            entity.setComponentId(UUID.randomUUID().toString().replace("-", ""));
            uniComponentService.save(entity);
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
    public Result update(@RequestBody UniComponentUpdateDTO updateDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(updateDTO, "subCode", "componentName", "id");
        String log = "更新部件：" + updateDTO.getComponentName();
        String subCode = updateDTO.getSubCode();
        try {
            UniDeviceEntity device = uniDeviceService.getByDeviceId(subCode, updateDTO.getDeviceId());
            if (device == null) {
                throw new FastException("设备不存在");
            }
            UniComponentEntity entity = ConvertUtils.toBean(updateDTO, UniComponentEntity.class);
            uniComponentService.updateById(entity);
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
    @PostMapping("/getByComponentId")
    public Result<UniComponentVO> getByComponentId(@RequestBody UniComponentUpdateDTO updateDTO) {
        ParamsUtils.isBlank(updateDTO, "subCode", "componentId");
        UniComponentEntity entity = uniComponentService.getByComponentId(updateDTO.getSubCode(), updateDTO.getComponentId());
        UniComponentVO vo = ConvertUtils.toBean(entity, UniComponentVO.class);
        return Result.success(vo);
    }

    /**
     * 根据设备查询
     *
     * @param updateDTO
     * @return
     */
    @PostMapping("/listByDeviceId")
    public Result<List<UniComponentVO>> listByDeviceId(@RequestBody UniComponentUpdateDTO updateDTO) {
        ParamsUtils.isBlank(updateDTO, "subCode", "deviceId");
        List<UniComponentVO> list = uniComponentService.listByDeviceId(updateDTO.getSubCode(), updateDTO.getDeviceId());
        return Result.success(list);
    }

    /**
     * 删除记录
     *
     * @return
     */
    @PostMapping("/delete")
    public Result delete(@RequestBody UniComponentUpdateDTO updateDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(updateDTO, "id", "subCode", "componentName");
        String log = "删除部件：" + updateDTO.getComponentName();
        String subCode = updateDTO.getSubCode();
        try {
            UniComponentEntity component = uniComponentService.getById(updateDTO.getId());
            if (component != null) {
                //删除点位
                uniPointService.lambdaUpdate()
                        .eq(UniPointEntity::getSubCode, subCode)
                        .eq(UniPointEntity::getComponentId, component.getComponentId())
                        .remove();
                //删除部件
                uniComponentService.removeById(updateDTO.getId());
            }
            HisLogBusinessUtils.recordSuccess(subCode, LOG_TITLE, log, OperLogTypeEnum.DELETE.getType(), request);
            return Result.success();
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(e, subCode, LOG_TITLE, log, OperLogTypeEnum.DELETE.getType(), request);
        }
    }
}

