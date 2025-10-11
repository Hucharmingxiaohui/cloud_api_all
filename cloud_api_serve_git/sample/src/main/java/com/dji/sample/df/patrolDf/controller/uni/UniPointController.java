package com.dji.sample.df.patrolDf.controller.uni;


import com.df.framework.enums.OperLogTypeEnum;
import com.df.framework.utils.PageUtils;
import com.df.framework.utils.ParamsUtils;
import com.df.framework.utils.file.FileUtils;
import com.df.framework.vo.PageVO;
import com.df.framework.vo.Result;
import com.df.framework.vo.Tree;
import com.df.server.dto.uniPoint.*;
import com.df.server.entity.uni.UniPointEntity;
import com.df.server.service.uni.UniPointService;
import com.df.server.utils.ExcelUtil;
import com.df.server.utils.HisLogBusinessUtils;
import com.df.server.vo.FileDownLoad.FileDownLoadVO;
import com.df.server.vo.UniPoint.UniPointVO;
import com.dji.sample.component.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;


/**
 * 巡检点位接口
 * <p>
 * Created by lianyc on 2025-02-11
 */
@RestController
@RequestMapping("/correction/uniPoint")
@AuthInterceptor.IgnoreAuth
public class UniPointController {

    private static final String LOG_TITLE = "";

    @Autowired
    private UniPointService uniPointService;

    /**
     * 点位树
     *
     * @return
     */
    @PostMapping("/tree")
    public Result<String> pointTree(@RequestBody UniPointPageDTO paramsDto) {
        ParamsUtils.isBlank(paramsDto, "subCode");
        List<Tree> treeList = uniPointService.pointTree(paramsDto);
        return Result.success(treeList);
    }

    /**
     * 点位层级树
     *
     * @param dto
     * @return
     */
    @PostMapping("/pointLevelTree")
    public Result<List<Tree>> pointLevelTree(@RequestBody UniTreeDTO dto) {
        ParamsUtils.isBlank(dto, "level");
        List<Tree> treeList = uniPointService.pointLevelTree(dto);
        return Result.success(treeList);
    }

    /**
     * 分页查询
     *
     * @param pageDTO
     * @return
     */
    @PostMapping("/page")
    public Result<PageVO<UniPointVO>> getPageCustom(@RequestBody UniPointPageDTO pageDTO, HttpServletRequest request) {
        PageUtils.pageParamsExtend(pageDTO);
        PageVO<UniPointVO> page = uniPointService.customPage(pageDTO);
        return Result.success(page);
    }

    /**
     * 配置管理-标准点位库-巡检点位模板下载
     *
     * @param response 响应
     */
    @GetMapping("/downloadTemplate")
    public void downloadTemplate(HttpServletResponse response) {
        FileUtils.downloadProjectFile("static/excel/点位导入模板.xlsx", response);
    }

    /**
     * 配置管理-标准点位库-点位导出
     *
     * @return 结果
     */
    @PostMapping("/pointExport")
    public void pointExport(@RequestBody UniPointPageDTO dto, HttpServletRequest request, HttpServletResponse response) {
        //ParamsUtils.isBlank(sendDto, "subCode");
        String log = "变电站点位导出：" + dto.getSubCode();
        String subCode = dto.getSubCode();
        try {
            List<UniPointImportExcel> list = uniPointService.listPointExport(dto);
            ExcelUtil<UniPointImportExcel> util = new ExcelUtil<>(UniPointImportExcel.class);
            FileDownLoadVO file = util.exportExcel(list, "点位信息");
            FileUtils.downloadLocalFile(file.getFileFullPath(), response);
            FileUtils.deleteFile(file.getFileFullPath());
            HisLogBusinessUtils.recordSuccess(subCode, LOG_TITLE, log, OperLogTypeEnum.UPDATE.getType(), request);
        } catch (Exception e) {
            HisLogBusinessUtils.recordAndRtnErr(e, subCode, LOG_TITLE, log, OperLogTypeEnum.UPDATE.getType(), request);
        }
    }

    /**
     * 添加点位
     */
    @PostMapping("/addPoint")
    public Result addPoint(@RequestBody UniPointAddDTO addDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(addDTO,
                "pointName", "pointType", "subCode", "sysCode",
                "areaId", "areaName", "bayId", "bayName", "deviceId", "deviceName", "componentId", "componentName",
                "saveTypeList", "recognitionTypeList",
                "deviceType", "phase", "meterType", "appearanceType", "isObj",
                "level");
        String log = "新增点位：" + addDTO.getPointName();
        String subCode = addDTO.getSubCode();
        try {
            uniPointService.addPoint(addDTO);
            HisLogBusinessUtils.recordSuccess(subCode, LOG_TITLE, log, OperLogTypeEnum.ADD.getType(), request);
            return Result.success();
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(e, subCode, LOG_TITLE, log, OperLogTypeEnum.ADD.getType(), request);
        }
    }

    /**
     * 修改点位
     */
    @PostMapping("/modifyPoint")
    public Result modifyPoint(@RequestBody UniPointUpdateDTO updateDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(updateDTO, "id",
                "pointName", "pointType", "subCode", "sysCode",
                "areaId", "areaName", "bayId", "bayName", "deviceId", "deviceName", "componentId", "componentName",
                "saveTypeList", "recognitionTypeList",
                "deviceType", "phase", "meterType", "appearanceType", "isObj",
                "level");
        String log = "更新点位：" + updateDTO.getPointName();
        String subCode = updateDTO.getSubCode();
        try {
            uniPointService.modifyPoint(updateDTO);
            HisLogBusinessUtils.recordSuccess(subCode, LOG_TITLE, log, OperLogTypeEnum.UPDATE.getType(), request);
            return Result.success();
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(e, subCode, LOG_TITLE, log, OperLogTypeEnum.UPDATE.getType(), request);
        }
    }

    /**
     * 删除点位
     */
    @PostMapping("/deletePoint")
    public Result deletePoint(@RequestBody UniPointUpdateDTO updateDTO, HttpServletRequest request) {
        ParamsUtils.isBlank(updateDTO, "id", "pointName", "subCode", "pointCode");
        String log = "删除点位：" + updateDTO.getPointName();
        String subCode = updateDTO.getSubCode();
        try {
            String pointCode = updateDTO.getPointCode();
            uniPointService.lambdaUpdate().eq(UniPointEntity::getSubCode, subCode).eq(UniPointEntity::getPointCode, pointCode).remove();
            HisLogBusinessUtils.recordSuccess(subCode, LOG_TITLE, log, OperLogTypeEnum.DELETE.getType(), request);
            return Result.success();
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(e, subCode, LOG_TITLE, log, OperLogTypeEnum.DELETE.getType(), request);
        }
    }

    /**
     * 点位识别类型 recognition_type_list
     * 转
     * 智能分析子类，多个用英文逗号分割 point_analyse_type
     * <p>
     * 智能分析大类 point_analyse_category
     */
    @PostMapping("/formatRegType")
    public Result formatRegType() {
        return Result.success(uniPointService.getPointACTTypeByReg());
    }

    /**
     * 配置点位基准图片
     *
     * @param file
     * @param subCode
     * @param pointCode
     * @param request
     * @return
     */
    /*@PostMapping("/uploadPbBaseImage")
    public Result uploadImage(@RequestParam("file") MultipartFile file,
                              @RequestParam("subCode") String subCode,
                              @RequestParam("pointCode") String pointCode,
                              HttpServletRequest request) {
        FileUploadUtils.checkJPGFileExtension(file);
        if (StringUtils.isBlank(subCode)) {
            return Result.error("请选择变电站");
        }
        if (StringUtils.isBlank(pointCode)) {
            return Result.error("请选择点位");
        }
        String log = "配置点位基准图片 点位编码：" + pointCode;
        try {
            String visitUrl = uniPointService.uploadPbBaseImage(subCode, pointCode, file);
            HisLogBusinessUtils.recordSuccess(subCode, LOG_TITLE, log, OperLogTypeEnum.UPDATE.getType(), request);
            return Result.success(visitUrl, "");
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(e, subCode, LOG_TITLE, log, OperLogTypeEnum.UPDATE.getType(), request);
        }
    }*/


    /**
     * 向上级点位模型更新
     *
     * @return
     */
   /* @PostMapping("/syncModelToUpper")
    public Result syncModelToUpper(@RequestBody SyncUpperModelMessage syncUpperModelMessage, HttpServletRequest request) {
        ParamsUtils.isBlank(syncUpperModelMessage , "subCode");
        String subCode = syncUpperModelMessage.getSubCode();
        String log = "向上级点位模型更新 变电站：" + subCode;
        try {
            syncUpperModelMessage.setItemType("1");
            patrolPlatformService.modelSyncToUpper(syncUpperModelMessage);
            HisLogBusinessUtils.recordSuccess(subCode, LOG_TITLE, log, OperLogTypeEnum.UPDATE.getType(), request);
            return Result.success();
        } catch (Exception e) {
            return HisLogBusinessUtils.recordAndRtnErr(e, subCode, LOG_TITLE, log, OperLogTypeEnum.UPDATE.getType(), request);
        }
    }*/
}

