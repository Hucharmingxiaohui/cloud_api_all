package com.df.server.service.uni;


import com.baomidou.mybatisplus.extension.service.IService;
import com.df.framework.vo.PageVO;
import com.df.server.dto.UniTaskPlan.UniTaskPlanPageDTO;
import com.df.server.dto.UniTaskPlan.UniTaskPlanParamsDTO;
import com.df.server.entity.his.HisUniTaskEntity;
import com.df.server.entity.uni.UniTaskPlanEntity;
import com.df.server.vo.UniTaskPlan.UniTaskPlanVO;

import java.util.Date;
import java.util.List;

/**
 * 巡检任务接口
 * <p>
 * Created by lianyc on 2025-05-20
 */
public interface UniTaskPlanService extends IService<UniTaskPlanEntity> {


    //自定义分页查询（可写SQL自己关联表）
    PageVO<UniTaskPlanVO> customPage(UniTaskPlanPageDTO pageDTO);

    void saveNewPlan(UniTaskPlanEntity entity);

    void updatePlan(UniTaskPlanEntity entity);

    UniTaskPlanVO getInfoByParams(UniTaskPlanParamsDTO paramsDTO);

    void deleteByParams(UniTaskPlanParamsDTO paramsDTO);

    void enableByPlanNo(UniTaskPlanParamsDTO paramsDTO);

    void auditByPlanNo(UniTaskPlanParamsDTO paramsDTO);

    List<UniTaskPlanEntity> getScheduledPlan();

    void updatePlanLastExecuteTime(String planNo, Date planStartTime);
}

