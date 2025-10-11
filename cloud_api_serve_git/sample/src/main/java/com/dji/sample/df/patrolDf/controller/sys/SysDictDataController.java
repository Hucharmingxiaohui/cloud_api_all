package com.dji.sample.df.patrolDf.controller.sys;


import com.df.framework.utils.ParamsUtils;
import com.df.framework.vo.Result;
import com.df.server.dto.sysDictData.SysDictDataParamsDTO;
import com.df.server.entity.sys.SysDictDataEntity;
import com.df.server.service.sys.SysDictDataService;
import com.dji.sample.component.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典数据接口
 * <p>
 * Created by lianyc on 2022-10-24
 */
@RestController
@RequestMapping("/sysDictData")
@AuthInterceptor.IgnoreAuth
public class SysDictDataController {

    @Autowired
    private SysDictDataService sysDictDataService;

    /**
     * 根据类型查询字典项
     *
     * @return
     */
    @PostMapping("/getByType")
    public Result<List<SysDictDataEntity>> getListByType(@RequestBody SysDictDataParamsDTO dict) {
        ParamsUtils.isBlank(dict, "dictType");
        String dictType = dict.getDictType();
        List<SysDictDataEntity> list = sysDictDataService.listDictEntityByDiceType(dictType);
        return Result.success(list);
    }
}

