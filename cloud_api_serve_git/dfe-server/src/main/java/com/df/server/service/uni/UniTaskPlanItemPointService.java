package com.df.server.service.uni;


import com.baomidou.mybatisplus.extension.service.IService;
import com.df.framework.vo.PageVO;
import com.df.server.dto.UniTaskPlan.UniTaskPlanParamsDTO;
import com.df.server.dto.UniTaskPlanItemPoint.UniTaskPlanItemPointPageDTO;
import com.df.server.entity.uni.UniTaskPlanItemPointEntity;
import com.df.server.vo.UniTaskPlanItemPoint.UniTaskPlanItemPointVO;

/**
 * 机器人巡检任务接口
 * <p>
 * Created by lianyc on 2025-05-20
 */
public interface UniTaskPlanItemPointService extends IService<UniTaskPlanItemPointEntity> {


    //自定义分页查询（可写SQL自己关联表）
    PageVO<UniTaskPlanItemPointVO> customPage(UniTaskPlanItemPointPageDTO pageDTO);

    void saveNewPlanPoints(String subCode, String planNo, Integer deviceLevel, String deviceList);

    void deleteByParams(UniTaskPlanParamsDTO paramsDTO);

    Integer countPlanPointsNum(String planNo);
}

