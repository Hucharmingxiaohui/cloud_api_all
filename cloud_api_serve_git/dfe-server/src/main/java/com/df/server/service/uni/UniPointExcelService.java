package com.df.server.service.uni;


import com.baomidou.mybatisplus.extension.service.IService;
import com.df.server.entity.uni.UniPointExcelEntity;

/**
 * Excel 点位上传记录接口
 * <p>
 * Created by lianyc on 2023-11-11
 */
public interface UniPointExcelService extends IService<UniPointExcelEntity> {

    /**
     * 只允许只有一个Excel在工作
     *
     * @return
     */
    UniPointExcelEntity getExcel();

    /**
     * 错误检查
     *
     * @param id
     */
    void importCheck(Long id);

    /**
     * 导入点位
     *
     * @param id
     */
    void importExcel(Long id);

    void clearExcel();

}

