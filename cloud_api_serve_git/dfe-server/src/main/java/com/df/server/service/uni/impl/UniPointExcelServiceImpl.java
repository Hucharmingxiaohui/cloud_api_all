package com.df.server.service.uni.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.framework.config.FileConfig;
import com.df.framework.exception.FastException;
import com.df.server.dto.uniPoint.UniPointImportExcel;
import com.df.server.entity.uni.UniPointExcelEntity;
import com.df.server.mapper.uni.UniPointExcelMapper;
import com.df.server.service.uni.UniPointExcelImportErrorService;
import com.df.server.service.uni.UniPointExcelService;
import com.df.server.service.uni.UniPointService;
import com.df.server.utils.ExcelUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service("uniPointExcelService")
public class UniPointExcelServiceImpl extends ServiceImpl<UniPointExcelMapper, UniPointExcelEntity> implements UniPointExcelService {

    private static final ConcurrentLinkedQueue<UniPointExcelEntity> ImportExcelQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    private FileConfig fileConfig;
    @Autowired
    private UniPointExcelImportErrorService uniPointExcelImportErrorService;
    @Autowired
    private UniPointService uniPointService;

    @Override
    public UniPointExcelEntity getExcel() {
        return this.lambdaQuery().last("limit 1").one();
    }

    @Override
    public void importCheck(Long id) {
        UniPointExcelEntity excel = this.getById(id);
        if (excel == null || StringUtils.isBlank(excel.getFilePath())) {
            throw new FastException("文件不存在");
        }
        File excelFile = new File(fileConfig.getFileSavePath() + "/" + excel.getFilePath());
        if (!excelFile.exists()) {
            throw new FastException("文件不存在");
        }
        if (excel.getImportStatus() == 1) {
            throw new FastException("文件正在检查中");
        }
        if (excel.getImportStatus() == 4) {
            throw new FastException("文件正在导入中");
        }
        uniPointExcelImportErrorService.addNewCheckExcel(excel);
    }

    @Override
    public void importExcel(Long id) {
        if (id == null) {
            throw new FastException("文件不存在");
        }
        UniPointExcelEntity excel = this.getById(id);
        if (excel == null || StringUtils.isBlank(excel.getFilePath())) {
            throw new FastException("文件不存在");
        }
        File excelFile = new File(fileConfig.getFileSavePath() + "/" + excel.getFilePath());
        if (!excelFile.exists()) {
            throw new FastException("文件不存在");
        }
        if (excel.getImportStatus() == 0) {
            throw new FastException("请先检查文件");
        }
        if (excel.getImportStatus() == 1) {
            throw new FastException("数据检查中，请稍后");
        }
        /*if (excel.getImport_status() == 2) {
            throw new BusinessException("数据检查不同过，请重新上传并检查");
        }*/
        if (excel.getImportStatus() == 4) {
            throw new FastException("数据已经在导入中，请稍后。。");
        }
        excel.setImportStatus(4);
        excel.setImportTime(new Date());
        this.updateById(excel);
        ImportExcelQueue.add(excel);
    }

    @Override
    public void clearExcel() {
        baseMapper.clearExcel();
    }

    @Scheduled(fixedDelay = 3 * 1000L)
    public void executeCheckExcel() throws Exception {
        UniPointExcelEntity excel = ImportExcelQueue.poll();
        if (excel == null || StringUtils.isBlank(excel.getFilePath())) {
            return;
        }
        if (excel.getImportStatus() == 0) {
            return;
        }
        if (excel.getImportStatus() == 1) {
            return;
        }
        if (excel.getImportStatus() == 2) {
            return;
        }
        File excelFile = new File(fileConfig.getFileSavePath() + "/" + excel.getFilePath());
        if (!excelFile.exists()) {
            log.error("文件不存在");
            return;
        }

        ExcelUtil<UniPointImportExcel> util = new ExcelUtil<>(UniPointImportExcel.class);
        List<UniPointImportExcel> points = util.importExcel(Files.newInputStream(Paths.get(fileConfig.getFileSavePath() + "/" + excel.getFilePath())));

        // 判断是否有操作修改或者更新的点
       /* List<DfUniPointExcel> pointsForUpdate = points.stream()
                .filter(item -> StringUtils.isNotBlank(item.getOperate()) &&
                        ("1".equals(item.getOperate()) || "2".equals(item.getOperate())) &&
                        StringUtils.isNotBlank(item.getPoint_code()))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(pointsForUpdate)) {
            points = pointsForUpdate;
        }*/
        int completeNum = 0;

        //String subCode = pubSubstationMapper.getSubCode();

        for (UniPointImportExcel point : points) {
            try {
                // 针对填写的每行点位数据新增点位信息
                uniPointService.importPoint(point);
                completeNum++;
            } catch (Exception e) {
                log.error("[配置管理-点位标准库-导入点位文件]同步过程中，出现异常，填写的数据:{}，异常:{}", point, e.getMessage());
            }
        }
        excel.setImportStatus(5);
        excel.setImportSuccessCount(completeNum);
        this.updateById(excel);
    }
}
