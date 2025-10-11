package com.df.server.service.uni.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.framework.thread.CustomExecutorFactory;
import com.df.framework.utils.PageUtils;
import com.df.framework.vo.PageVO;
import com.df.server.dto.UniTaskPlan.UniTaskPlanParamsDTO;
import com.df.server.dto.UniTaskPlanItemPoint.UniTaskPlanItemPointPageDTO;
import com.df.server.entity.uni.UniPointEntity;
import com.df.server.entity.uni.UniRobotEntity;
import com.df.server.entity.uni.UniTaskPlanItemPointEntity;
import com.df.server.mapper.uni.UniPointMapper;
import com.df.server.mapper.uni.UniRobotMapper;
import com.df.server.mapper.uni.UniTaskPlanItemPointMapper;
import com.df.server.mapper.uni.UniTaskPlanMapper;
import com.df.server.service.uni.UniTaskPlanItemPointService;
import com.df.server.vo.UniTaskPlanItemPoint.UniTaskPlanItemPointVO;
import com.mysql.cj.exceptions.ExceptionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service("uniTaskPlanItemPointService")
public class UniTaskPlanItemPointServiceImpl extends ServiceImpl<UniTaskPlanItemPointMapper, UniTaskPlanItemPointEntity> implements UniTaskPlanItemPointService {

    @Autowired
    private UniPointMapper uniPointMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UniRobotMapper uniRobotMapper;
    @Autowired
    private UniTaskPlanMapper uniTaskPlanMapper;
    @Autowired
    private UniXmlDataToDBService uniXmlDataToDBService;


    // 自定义分页查询
    @Override
    public PageVO<UniTaskPlanItemPointVO> customPage(UniTaskPlanItemPointPageDTO pageDTO) {
        List<UniTaskPlanItemPointVO> list = baseMapper.getPageList(pageDTO);
        Integer total = baseMapper.getPageTotal(pageDTO);
        return PageUtils.returnPageVOExtend(pageDTO, list, total);
    }

    @Override
    public void saveNewPlanPoints(String subCode, String planNo, Integer deviceLevel, String deviceList) {
        baseMapper.deletePlanPoints(planNo);
        List<UniTaskPlanItemPointEntity> list = getPointsOfLevel(subCode, planNo, deviceLevel, deviceList);
        Set<String> robotCodeList = list.stream().map(UniTaskPlanItemPointEntity::getRobotCode).collect(Collectors.toSet());
        if (!robotCodeList.isEmpty()) {
            List<UniRobotEntity> robotEntityList = uniRobotMapper.listRobotByCodeSet(robotCodeList);
            Set<Integer> robotTypeList = robotEntityList.stream().map(UniRobotEntity::getRobotType).collect(Collectors.toSet());
            String planType = StringUtils.join(robotTypeList, ",");
            uniTaskPlanMapper.updatePlanType(planNo, planType);
        }
        CustomExecutorFactory.dbHandlepool.execute(() -> saveListExecuteBatch(list, planNo));
    }

    @Override
    public void deleteByParams(UniTaskPlanParamsDTO paramsDTO) {
        baseMapper.deletePlanPoints(paramsDTO.getPlanNo());
    }

    @Override
    public Integer countPlanPointsNum(String planNo) {
        return this.lambdaQuery().eq(UniTaskPlanItemPointEntity::getPlanNo, planNo).count().intValue();
    }

    private List<UniTaskPlanItemPointEntity> getPointsOfLevel(String subCode, String planNo, Integer deviceLevel, String deviceListStr) {
        List<String> deviceList = Arrays.asList(deviceListStr.split(","));
        return uniPointMapper.listPlanPoint(deviceLevel, deviceList);
    }

    public void saveListExecuteBatch(List<UniTaskPlanItemPointEntity> addList, String planNo) {
        long before = System.currentTimeMillis();
        Connection conn = DataSourceUtils.getConnection(Objects.requireNonNull(jdbcTemplate.getDataSource()));
        log.info("Function:点位入库 Connection has opened");
        String sql = "insert into df_uni_task_plan_item_point " +
                "(sub_code," +
                "plan_no," +
                "data_type," +
                "file_type," +
                "point_code," +
                "robot_pos," +
                "robot_code) " +
                "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int batchCount = 0;
            for (UniTaskPlanItemPointEntity entity : addList) {
                // 使用工具类简化参数设置
                uniXmlDataToDBService.setStringSafe(pstmt, 1, entity.getSubCode());
                uniXmlDataToDBService.setStringSafe(pstmt, 2, planNo);
                uniXmlDataToDBService.setSafe(pstmt, 3, entity.getDataType(), Types.INTEGER);
                uniXmlDataToDBService.setSafe(pstmt, 4, entity.getFileType(), Types.INTEGER);
                uniXmlDataToDBService.setStringSafe(pstmt, 5, entity.getPointCode());
                uniXmlDataToDBService.setSafe(pstmt, 6, entity.getRobotPos(), Types.INTEGER);
                uniXmlDataToDBService.setStringSafe(pstmt, 7, entity.getRobotCode());
                pstmt.addBatch();
                if (++batchCount % 1000 == 0) {
                    uniXmlDataToDBService.executeBatch(pstmt, conn, batchCount);
                }
            }
            // 处理剩余批次
            uniXmlDataToDBService.executeBatch(pstmt, conn, batchCount);
            conn.commit();
        } catch (SQLException e) {
            log.error("Database error", e);
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
        } finally {
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
            log.info("Function:xml点位入库 Connection has released");
        }
        long after = System.currentTimeMillis();
        log.info("xml点位入库 Time-consuming: {} ms", after - before);
    }

}
