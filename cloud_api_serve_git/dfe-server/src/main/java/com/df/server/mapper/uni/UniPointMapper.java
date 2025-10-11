package com.df.server.mapper.uni;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.framework.vo.Tree;
import com.df.server.dto.JobPlan.JobPlanItemPointDTO;
import com.df.server.dto.uniPoint.UniPointImportExcel;
import com.df.server.dto.uniPoint.UniPointPageDTO;
import com.df.server.dto.uniPoint.UniTreeDTO;
import com.df.server.entity.uni.UniPointEntity;
import com.df.server.entity.uni.UniTaskPlanItemPointEntity;
import com.df.server.vo.UniPoint.UniPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 巡检点位接口
 * <p>
 * Created by lianyc on 2025-02-11
 */
@Mapper
public interface UniPointMapper extends BaseMapper<UniPointEntity> {
    /**
     * 基准分页查询
     *
     * @param pageDTO
     * @return
     */
    List<UniPointVO> getPageList(UniPointPageDTO pageDTO);

    Integer getPageTotal(UniPointPageDTO pageDTO);

    /**
     * 组合点位树
     *
     * @param paramsDto
     * @return
     */
    List<Tree> pointTree(UniPointPageDTO paramsDto);

    /**
     * 查询点位
     *
     * @param subCode
     * @param pointCode
     * @return
     */
    UniPointEntity getPointEntityByCode(@Param("subCode") String subCode,
                                        @Param("pointCode") String pointCode);

    /**
     * 点位导出
     *
     * @param dto
     * @return
     */
    List<UniPointImportExcel> listPointExport(UniPointPageDTO dto);

    List<Tree> pointLevelTree(UniTreeDTO dto);

    void updatePointRobotPos(@Param("subCode") String subCode,
                             @Param("pointCode") String pointCode,
                             @Param("videoPos") String videoPos,
                             @Param("devicePos") String devicePos,
                             @Param("robotCode") String robotCode);

    void updatePointUavPos(  @Param("subCode") String subCode,
                             @Param("pointCode") String pointCode,
                             @Param("waylinePos") String waylinePos,
                             @Param("waylinePointPos") String waylinePointPos);

    void clearRobotPos(@Param("devicePos") String devicePos, @Param("robotCode") String robotCode);

    void clearUavPos(@Param("waylinePos") String waylinePos, @Param("waylinePointPos") String waylinePointPos);

    List<UniTaskPlanItemPointEntity> listPlanPoint(@Param("deviceLevel") Integer deviceLevel,
                                                   @Param("deviceList") List<String> deviceList);

    List<JobPlanItemPointDTO> listPlanPoint2(@Param("deviceLevel") Integer deviceLevel,
                                             @Param("deviceList") List<String> deviceList);

    @Select("select point_code from df_uni_point")
    List<String> test1();

    List<JobPlanItemPointDTO> choseWaylinePos();

}
