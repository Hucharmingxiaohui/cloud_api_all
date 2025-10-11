package com.df.server.mapper.uni;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.dto.UniTaskPlan.UniTaskPlanPageDTO;
import com.df.server.dto.UniTaskPlan.UniTaskPlanParamsDTO;
import com.df.server.entity.his.HisUniTaskEntity;
import com.df.server.entity.uni.UniTaskPlanEntity;
import com.df.server.vo.UniTaskPlan.UniTaskPlanVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 巡检任务接口
 * <p>
 * Created by lianyc on 2025-05-20
 */
@Mapper
public interface UniTaskPlanMapper extends BaseMapper<UniTaskPlanEntity> {
    //自定义分页查询
    List<UniTaskPlanVO> getPageList(UniTaskPlanPageDTO pageDTO);

    Integer getPageTotal(UniTaskPlanPageDTO pageDTO);

    void updatePlanType(@Param("planNo") String planNo, @Param("planType") String planType);

    UniTaskPlanVO getInfoByParams(UniTaskPlanParamsDTO paramsDTO);

    void deleteByParams(UniTaskPlanParamsDTO paramsDTO);

    void enableByPlanNo(UniTaskPlanParamsDTO paramsDTO);

    void auditByPlanNo(UniTaskPlanParamsDTO paramsDTO);

    List<UniTaskPlanEntity> getScheduledPlan();

    void updatePlanLastExecuteTime(@Param("planNo") String planNo,
                                   @Param("planStartTime") Date planStartTime);
}