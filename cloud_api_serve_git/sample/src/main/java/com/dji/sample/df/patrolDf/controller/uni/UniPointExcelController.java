package com.dji.sample.df.patrolDf.controller.uni;


import com.df.framework.config.FileConfig;
import com.df.framework.dto.IdDTO;
import com.df.framework.exception.FastException;
import com.df.framework.utils.ConvertUtils;
import com.df.framework.utils.PageUtils;
import com.df.framework.utils.file.FileUploadUtils;
import com.df.framework.utils.file.FileUtils;
import com.df.framework.vo.PageVO;
import com.df.framework.vo.Result;
import com.df.server.dto.uniPoint.UniPointExcelImportErrorDTO;
import com.df.server.entity.uni.UniPointExcelEntity;
import com.df.server.entity.uni.UniPointExcelImportErrorEntity;
import com.df.server.service.uni.UniPointExcelImportErrorService;
import com.df.server.service.uni.UniPointExcelService;
import com.df.server.utils.ExcelUtil;
import com.df.server.vo.FileDownLoad.FileDownLoadVO;
import com.df.server.vo.UniPoint.UniPointExcelImportErrorExcelVO;
import com.dji.sample.component.AuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/**
 * 点位导入检查接口
 */
@Slf4j
@RestController
@RequestMapping("/uniPointExcel")
@AuthInterceptor.IgnoreAuth
public class UniPointExcelController {

    @Autowired
    private FileConfig fileConfig;
    @Autowired
    private UniPointExcelService uniPointExcelService;
    @Autowired
    private UniPointExcelImportErrorService uniPointExcelImportErrorService;

    /**
     * 文件上传
     *
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public Result uploadFile(MultipartFile file, HttpServletRequest request) {
        UniPointExcelEntity oldExcel = uniPointExcelService.getExcel();
        if (oldExcel != null) {
            if (oldExcel.getImportStatus() == 1) {
                return Result.error("当前文件正在数据检查，请稍后上传");
            }
            if (oldExcel.getImportStatus() == 4) {
                return Result.error("当前文件正在入库，请稍后上传");
            }
        }

        if (file == null) {
            return Result.error("上传文件有误！");
        }
        if (file.getSize() == 0) {
            return Result.error("不能上传空文件！");
        }
        int fileSize = 0;
        if (file.getSize() > 0) {
            fileSize = (int) Math.ceil((double) file.getSize() / 1048576);
            if (fileSize > 10) {
                return Result.error("请控制上传文件不大于10MB！");
            }
        }
        FileUploadUtils.checkExcelFileExtension(file);
        String dbSavePath = FileUploadUtils.saveFileReturnDBSavePath(file, "importPoint/excel/");

        UniPointExcelEntity excel = new UniPointExcelEntity();
        excel.setFileName(file.getOriginalFilename());
        excel.setFilePath(dbSavePath);
        excel.setUploadTime(new Date());
        excel.setUploadUser("");
        excel.setImportStatus(0);//未检查

        uniPointExcelService.clearExcel();
        uniPointExcelImportErrorService.clearErrorInfo();
        uniPointExcelService.save(excel);
        uniPointExcelImportErrorService.addNewCheckExcel(excel);
        return Result.success("文件上传成功！开始检查中...");
    }

    /**
     * 错误检查
     *
     * @param idDTO
     * @return
     */
    @PostMapping("/importCheck")
    public Result importCheck(@RequestBody IdDTO idDTO) {
        try {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            uniPointExcelService.importCheck(idDTO.getId());
            return Result.success("开始检查中，请稍后查看结果..");
        } catch (FastException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 开始数据入库
     *
     * @param idDTO
     * @return
     */
    @PostMapping("/import")
    public Result importFile(@RequestBody IdDTO idDTO) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        uniPointExcelService.importExcel(idDTO.getId());
        return Result.success("开始导入中，请稍后查看结果..");
    }

    /**
     * 查询文件信息
     *
     * @param idDTO
     * @return
     */
    @PostMapping("/getExcel")
    public Result<UniPointExcelEntity> getExcel(@RequestBody IdDTO idDTO) {
        UniPointExcelEntity excel = uniPointExcelService.getExcel();
        return Result.success(excel);
    }

    /**
     * 分页查询错误
     *
     * @param dto
     * @return
     */
    @PostMapping("/listError")
    @AuthInterceptor.IgnoreAuth
    public Result<List<UniPointExcelImportErrorEntity>> listError(@RequestBody UniPointExcelImportErrorDTO dto) {
        PageUtils.pageParamsExtend(dto);
        PageVO<UniPointExcelImportErrorEntity> list = uniPointExcelImportErrorService.getPage(dto);
        return Result.success(list);
    }


    /**
     * 导出错误
     *
     * @param dto
     * @return
     */
    @PostMapping("/exportError")
    @AuthInterceptor.IgnoreAuth
    public void exportError(@RequestBody UniPointExcelImportErrorDTO dto, HttpServletResponse response) {
        List<UniPointExcelImportErrorEntity> pointList = uniPointExcelImportErrorService.listError(dto);
        List<UniPointExcelImportErrorExcelVO> excelVOs =
                ConvertUtils.toBean(pointList, UniPointExcelImportErrorExcelVO.class);
        //点位数据导出的处理
        ExcelUtil<UniPointExcelImportErrorExcelVO> util = new ExcelUtil<>(UniPointExcelImportErrorExcelVO.class);
        FileDownLoadVO file = util.exportExcel(excelVOs, "错误数据");
        FileUtils.downloadLocalFile(file.getFileFullPath(), response);
        FileUtils.deleteFile(file.getFileFullPath());
    }

    /**
     * 导出上传文件
     *
     * @param request
     * @param response
     */
    @GetMapping("/downloadExcel")
    public void downloadTemplate(HttpServletRequest request, HttpServletResponse response) {
        String idstr = request.getParameter("id");
        if (idstr == null) {
            return;
        }
        int id = 0;
        try {
            id = Integer.parseInt(idstr);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return;
        }
        UniPointExcelEntity excel = uniPointExcelService.getById(id);
        if (excel == null) {
            return;
        }
        String fileFullPath = fileConfig.getFileSavePath() + "/" + excel.getFilePath();
        FileUtils.downloadLocalFile(fileFullPath, response);
    }

}
