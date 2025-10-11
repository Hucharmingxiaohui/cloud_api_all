package com.df.server.service.uni;


import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.extension.service.IService;
import com.df.framework.vo.PageVO;
import com.df.framework.vo.Tree;
import com.df.server.dto.uniPoint.*;
import com.df.server.entity.uni.UniPointEntity;
import com.df.server.vo.UniPoint.UniPointVO;

import java.util.List;

/**
 * 巡检点位接口
 * <p>
 * Created by lianyc on 2025-02-11
 */
public interface UniPointService extends IService<UniPointEntity> {


    /**
     * 点位树
     *
     * @param paramsDto
     * @return
     */
    List<Tree> pointTree(UniPointPageDTO paramsDto);

    PageVO<UniPointVO> customPage(UniPointPageDTO pageDTO);

    List<UniPointImportExcel> listPointExport(UniPointPageDTO dto);

    void addPoint(UniPointAddDTO addDTO);

    UniPointEntity getPointByCode(String subCode, String pointCode);

    void modifyPoint(UniPointUpdateDTO updateDTO);

    JSONArray getPointACTTypeByReg();

    List<Tree> pointLevelTree(UniTreeDTO dto);

    void importPoint(UniPointImportExcel point);
}

