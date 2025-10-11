package com.df.server.service.uni;


import com.baomidou.mybatisplus.extension.service.IService;
import com.df.framework.vo.PageVO;
import com.df.server.dto.uniPoint.UniPointExcelImportErrorDTO;
import com.df.server.entity.uni.UniPointExcelEntity;
import com.df.server.entity.uni.UniPointExcelImportErrorEntity;

import java.util.List;

/**
 * Excel 检查、导入错误信息接口
 * <p>
 * Created by lianyc on 2023-11-11
 */
public interface UniPointExcelImportErrorService extends IService<UniPointExcelImportErrorEntity> {

    /**
     * 向队列增加一个检查任务
     *
     * @param excel
     */
    void addNewCheckExcel(UniPointExcelEntity excel);

    PageVO<UniPointExcelImportErrorEntity> getPage(UniPointExcelImportErrorDTO dto);

    List<UniPointExcelImportErrorEntity> listError(UniPointExcelImportErrorDTO dto);

    void clearErrorInfo();
}

