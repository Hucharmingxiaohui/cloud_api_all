package com.df.server.mapper.uni;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.dto.UniTaskPlanItemPoint.UniTaskPlanItemPointPageDTO;
import com.df.server.entity.uni.UniTaskPlanItemPointEntity;
import com.df.server.vo.UniTaskPlanItemPoint.UniTaskPlanItemPointVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 机器人巡检任务接口
 * <p>
 * Created by lianyc on 2025-05-20
 */
@Mapper
public interface UniTaskPlanItemPointMapper extends BaseMapper<UniTaskPlanItemPointEntity> {
    //自定义分页查询
    List<UniTaskPlanItemPointVO> getPageList(UniTaskPlanItemPointPageDTO pageDTO);

    Integer getPageTotal(UniTaskPlanItemPointPageDTO pageDTO);

    void deletePlanPoints(String planNo);
}