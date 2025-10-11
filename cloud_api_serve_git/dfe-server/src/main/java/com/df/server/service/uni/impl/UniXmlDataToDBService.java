package com.df.server.service.uni.impl;

import com.df.server.entity.uni.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class UniXmlDataToDBService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * xml模型点位
     *
     * @param addList
     */
    public void saveUniPoint(List<UniPointEntity> addList) {
        long before = System.currentTimeMillis();
        Connection conn = DataSourceUtils.getConnection(Objects.requireNonNull(jdbcTemplate.getDataSource()));
        log.info("Function:xml点位入库 Connection has opened");
        String sql = "insert into df_uni_point " +
                "(point_code," +
                "point_name," +
                "point_type," +
                "sub_code," +
                "area_id," +
                "area_name," +
                "bay_id," +
                "bay_name," +
                "device_id," +
                "device_name," +
                "component_id," +
                "component_name," +
                "device_type," +
                "meter_type," +
                "appearance_type," +
                "save_type_list," +
                "recognition_type_list," +
                "phase," +
                "video_pos," +
                "task_type," +
                "level," +
                "point_analyse_category," +
                "point_analyse_type) " +
                "VALUES  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)" +
                " on duplicate key update " +
                "point_code = values(point_code)," +
                "point_name = values(point_name)," +
                "point_type = values(point_type)," +
                "sub_code = values(sub_code)," +
                "sys_code = values(sys_code)," +
                "area_id = values(area_id)," +
                "area_name = values(area_name)," +
                "bay_id = values(bay_id)," +
                "bay_name = values(bay_name)," +
                "device_id = values(device_id)," +
                "device_name = values(device_name)," +
                "component_id = values(component_id)," +
                "component_name = values(component_name)," +
                "device_type = values(device_type)," +
                "meter_type = values(meter_type)," +
                "appearance_type = values(appearance_type)," +
                "save_type_list = values(save_type_list)," +
                "recognition_type_list =values(recognition_type_list)," +
                "phase = values(phase)," +
                "video_pos = values(video_pos)," +
                "task_type = values(task_type)," +
                "level = values(level)," +
                "point_analyse_category =values(point_analyse_category)," +
                "point_analyse_type = values(point_analyse_type)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int batchCount = 0;

            for (UniPointEntity entity : addList) {
                // 使用工具类简化参数设置
                setPointParams(pstmt, entity);
                pstmt.addBatch();

                if (++batchCount % 1000 == 0) {
                    executeBatch(pstmt, conn, batchCount);
                }
            }
            // 处理剩余批次
            executeBatch(pstmt, conn, batchCount);
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

    private void setPointParams(PreparedStatement pstmt, UniPointEntity entity) throws SQLException {
        setStringSafe(pstmt, 1, entity.getPointCode());
        setStringSafe(pstmt, 2, entity.getPointName());
        setSafe(pstmt, 3, entity.getPointType(), Types.INTEGER);
        setStringSafe(pstmt, 4, entity.getSubCode());
        setStringSafe(pstmt, 5, entity.getAreaId());
        setStringSafe(pstmt, 6, entity.getAreaName());
        setStringSafe(pstmt, 7, entity.getBayId());
        setStringSafe(pstmt, 8, entity.getBayName());
        setStringSafe(pstmt, 9, entity.getDeviceId());
        setStringSafe(pstmt, 10, entity.getDeviceName());
        setStringSafe(pstmt, 11, entity.getComponentId());
        setStringSafe(pstmt, 12, entity.getComponentName());
        setSafe(pstmt, 13, entity.getDeviceType(), Types.INTEGER);
        setSafe(pstmt, 14, entity.getMeterType(), Types.INTEGER);
        setSafe(pstmt, 15, entity.getAppearanceType(), Types.INTEGER);
        setStringSafe(pstmt, 16, entity.getSaveTypeList());
        setStringSafe(pstmt, 17, entity.getRecognitionTypeList());
        setStringSafe(pstmt, 18, entity.getPhase());
        setStringSafe(pstmt, 19, entity.getVideoPos());
        setStringSafe(pstmt, 20, entity.getTaskType());
        setSafe(pstmt, 21, entity.getLevel(), Types.INTEGER);
        setSafe(pstmt, 22, entity.getPointAnalyseCategory(), Types.INTEGER);
        setStringSafe(pstmt, 23, entity.getPointAnalyseType());
    }

    /**
     * xml模型点位入中间表
     *
     * @param pointList
     */
    public void saveCorrectionPoints(List<UniPointEntity> pointList) {
        long before = System.currentTimeMillis();
        Connection conn = DataSourceUtils.getConnection(Objects.requireNonNull(jdbcTemplate.getDataSource()));
        log.info("Function:xml模型点位入中间表 Connection has opened");
        String sql = "insert into df_correction_point " +
                "(sub_code," +
                "point_code) " +
                "VALUES  (?,?)" +
                " on duplicate key update " +
                "point_code = values(point_code)," +
                "sub_code = values(sub_code)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int batchCount = 0;

            for (UniPointEntity entity : pointList) {
                // 使用工具类简化参数设置
                setCorrectionPointParams(pstmt, entity);
                pstmt.addBatch();
                if (++batchCount % 1000 == 0) {
                    executeBatch(pstmt, conn, batchCount);
                }
            }

            // 处理剩余批次
            executeBatch(pstmt, conn, batchCount);
            conn.commit();

        } catch (SQLException e) {
            log.error("Database error", e);
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
        } finally {
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
            log.info("Function:xml模型点位入中间表 Connection has released");
        }
        long after = System.currentTimeMillis();
        log.info("xml模型点位入中间表 Time-consuming: {} ms", after - before);
    }

    private void setCorrectionPointParams(PreparedStatement pstmt, UniPointEntity entity) throws SQLException {
        setStringSafe(pstmt, 1, entity.getSubCode());
        setStringSafe(pstmt, 2, entity.getPointCode());
    }

    /**
     * xml模型区域
     *
     * @param addList
     */
    public void saveUniArea(Collection<UniAreaEntity> addList) {
        long before = System.currentTimeMillis();
        Connection conn = DataSourceUtils.getConnection(Objects.requireNonNull(jdbcTemplate.getDataSource()));
        log.info("Function:xml区域入库 Connection has opened");
        String sql = "insert into df_uni_area " +
                "(area_id," +
                "area_name," +
                "sub_code) " +
                "VALUES  (?,?,?)" +
                " on duplicate key update " +
                "area_id = values(area_id)," +
                "area_name = values(area_name)," +
                "sub_code = values(sub_code)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int batchCount = 0;

            for (UniAreaEntity entity : addList) {
                // 使用工具类简化参数设置
                setAreaParams(pstmt, entity);
                pstmt.addBatch();

                if (++batchCount % 1000 == 0) {
                    executeBatch(pstmt, conn, batchCount);
                }
            }

            // 处理剩余批次
            executeBatch(pstmt, conn, batchCount);
            conn.commit();

        } catch (SQLException e) {
            log.error("Database error", e);
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
        } finally {
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
            log.info("Function:xml区域入库 Connection has released");
        }
        long after = System.currentTimeMillis();
        log.info("xml区域入库 Time-consuming: {} ms", after - before);
    }

    private void setAreaParams(PreparedStatement pstmt, UniAreaEntity entity) throws SQLException {
        setStringSafe(pstmt, 1, entity.getAreaId());
        setStringSafe(pstmt, 2, entity.getAreaName());
        setStringSafe(pstmt, 3, entity.getSubCode());
    }

    /**
     * xml模型间隔
     *
     * @param uniBayEntities
     */
    public void saveUniBay(Collection<UniBayEntity> uniBayEntities) {
        long before = System.currentTimeMillis();
        Connection conn = DataSourceUtils.getConnection(Objects.requireNonNull(jdbcTemplate.getDataSource()));
        log.info("Function:xml模型间隔 Connection has opened");
        String sql = "insert into df_uni_bay " +
                "(bay_id," +
                "bay_name," +
                "sub_code," +
                "area_id) " +
                "VALUES  (?,?,?,?)" +
                " on duplicate key update " +
                "bay_id = values(bay_id)," +
                "bay_name = values(bay_name)," +
                "sub_code = values(sub_code)," +
                "area_id = values(area_id)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int batchCount = 0;
            for (UniBayEntity entity : uniBayEntities) {
                // 使用工具类简化参数设置
                setBayParams(pstmt, entity);
                pstmt.addBatch();
                if (++batchCount % 1000 == 0) {
                    executeBatch(pstmt, conn, batchCount);
                }
            }
            // 处理剩余批次
            executeBatch(pstmt, conn, batchCount);
            conn.commit();
        } catch (SQLException e) {
            log.error("Database error", e);
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
        } finally {
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
            log.info("Function:xml模型间隔 Connection has released");
        }
        long after = System.currentTimeMillis();
        log.info("xml模型间隔 Time-consuming: {} ms", after - before);
    }

    private void setBayParams(PreparedStatement pstmt, UniBayEntity entity) throws SQLException {
        setStringSafe(pstmt, 1, entity.getBayId());
        setStringSafe(pstmt, 2, entity.getBayName());
        setStringSafe(pstmt, 3, entity.getSubCode());
        setStringSafe(pstmt, 4, entity.getAreaId());
    }

    /**
     * xml模型设备
     *
     * @param uniDeviceEntities
     */
    public void saveUniDevice(Collection<UniDeviceEntity> uniDeviceEntities) {
        long before = System.currentTimeMillis();
        Connection conn = DataSourceUtils.getConnection(Objects.requireNonNull(jdbcTemplate.getDataSource()));
        log.info("Function:xml模型设备 Connection has opened");
        String sql = "insert into df_uni_device " +
                "(device_id," +
                "device_name," +
                "device_type," +
                "sub_code," +
                "bay_id) " +
                "VALUES  (?,?,?,?,?)" +
                " on duplicate key update " +
                "device_id = values(device_id)," +
                "device_name = values(device_name)," +
                "device_type = values(device_type)," +
                "sub_code = values(sub_code)," +
                "bay_id = values(bay_id)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int batchCount = 0;
            for (UniDeviceEntity entity : uniDeviceEntities) {
                // 使用工具类简化参数设置
                setDeviceParams(pstmt, entity);
                pstmt.addBatch();
                if (++batchCount % 1000 == 0) {
                    executeBatch(pstmt, conn, batchCount);
                }
            }
            // 处理剩余批次
            executeBatch(pstmt, conn, batchCount);
            conn.commit();
        } catch (SQLException e) {
            log.error("Database error", e);
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
        } finally {
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
            log.info("Function:xml模型设备 Connection has released");
        }
        long after = System.currentTimeMillis();
        log.info("xml模型设备 Time-consuming: {} ms", after - before);
    }

    private void setDeviceParams(PreparedStatement pstmt, UniDeviceEntity entity) throws SQLException {
        setStringSafe(pstmt, 1, entity.getDeviceId());
        setStringSafe(pstmt, 2, entity.getDeviceName());
        setSafe(pstmt, 3, entity.getDeviceType(), Types.INTEGER);
        setStringSafe(pstmt, 4, entity.getSubCode());
        setStringSafe(pstmt, 5, entity.getBayId());
    }

    /**
     * xml模型部件
     *
     * @param uniComponentEntities
     */
    public void saveUniComponent(Collection<UniComponentEntity> uniComponentEntities) {
        long before = System.currentTimeMillis();
        Connection conn = DataSourceUtils.getConnection(Objects.requireNonNull(jdbcTemplate.getDataSource()));
        log.info("Function:xml模型部件 Connection has opened");
        String sql = "insert into df_uni_component " +
                "(component_id," +
                "component_name," +
                "sub_code," +
                "device_id) " +
                "VALUES  (?,?,?,?)" +
                " on duplicate key update " +
                "component_id = values(component_id)," +
                "component_name = values(component_name)," +
                "sub_code = values(sub_code)," +
                "device_id = values(device_id)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int batchCount = 0;
            for (UniComponentEntity entity : uniComponentEntities) {
                // 使用工具类简化参数设置
                setComponentParams(pstmt, entity);
                pstmt.addBatch();
                if (++batchCount % 1000 == 0) {
                    executeBatch(pstmt, conn, batchCount);
                }
            }
            // 处理剩余批次
            executeBatch(pstmt, conn, batchCount);
            conn.commit();
        } catch (SQLException e) {
            log.error("Database error", e);
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
        } finally {
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
            log.info("Function:xml模型部件 Connection has released");
        }
        long after = System.currentTimeMillis();
        log.info("xml模型部件 Time-consuming: {} ms", after - before);
    }

    private void setComponentParams(PreparedStatement pstmt, UniComponentEntity entity) throws SQLException {
        setStringSafe(pstmt, 1, entity.getComponentId());
        setStringSafe(pstmt, 2, entity.getComponentName());
        setStringSafe(pstmt, 3, entity.getSubCode());
        setStringSafe(pstmt, 4, entity.getDeviceId());
    }

    /**
     * xml模型预置位
     *
     * @param newPosList
     */
    public void savePointVideoPos(List<UniPointVideoPosEntity> newPosList) {
        long before = System.currentTimeMillis();
        Connection conn = DataSourceUtils.getConnection(Objects.requireNonNull(jdbcTemplate.getDataSource()));
        log.info("Function:xml模型预置位 Connection has opened");
        String sql = "insert into df_uni_point_video_pos " +
                "(point_code," +
                "sub_code," +
                "data_type," +
                "device_code," +
                "device_pos," +
                "rel_type) " +
                "VALUES  (?,?,?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int batchCount = 0;
            for (UniPointVideoPosEntity entity : newPosList) {
                // 使用工具类简化参数设置
                setPointVideoPosParams(pstmt, entity);
                pstmt.addBatch();
                if (++batchCount % 1000 == 0) {
                    executeBatch(pstmt, conn, batchCount);
                }
            }
            // 处理剩余批次
            executeBatch(pstmt, conn, batchCount);
            conn.commit();
        } catch (SQLException e) {
            log.error("Database error", e);
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
        } finally {
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
            log.info("Function:xml模型预置位 Connection has released");
        }
        long after = System.currentTimeMillis();
        log.info("xml模型预置位 Time-consuming: {} ms", after - before);
    }

    private void setPointVideoPosParams(PreparedStatement pstmt, UniPointVideoPosEntity entity) throws SQLException {
        setStringSafe(pstmt, 1, entity.getPointCode());
        setStringSafe(pstmt, 2, entity.getSubCode());
        setSafe(pstmt, 3, entity.getDataType(), Types.INTEGER);
        setStringSafe(pstmt, 4, entity.getDeviceCode());
        setSafe(pstmt, 5, entity.getDevicePos(), Types.INTEGER);
        setSafe(pstmt, 6, entity.getRelType(), Types.INTEGER);
    }

    /**
     * xml模型相机预置位
     *
     * @param videoPresetNews
     */
    public void saveVideoPreset(Collection<VideoPresetEntity> videoPresetNews) {
        long before = System.currentTimeMillis();
        Connection conn = DataSourceUtils.getConnection(Objects.requireNonNull(jdbcTemplate.getDataSource()));
        log.info("Function:xml模型相机预置位 Connection has opened");
        String sql = "insert into df_video_preset " +
                "(code," +
                "preset_name," +
                "preset_des," +
                "camera_code," +
                "preset_no) " +
                "VALUES  (?,?,?,?,?)" +
                " on duplicate key update " +
                "code = values(code)," +
                "preset_name = values(preset_name)," +
                "preset_des = values(preset_des)," +
                "camera_code = values(camera_code)," +
                "preset_no = values(preset_no)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int batchCount = 0;
            for (VideoPresetEntity entity : videoPresetNews) {
                // 使用工具类简化参数设置
                setVideoPresetParams(pstmt, entity);
                pstmt.addBatch();
                if (++batchCount % 1000 == 0) {
                    executeBatch(pstmt, conn, batchCount);
                }
            }
            // 处理剩余批次
            executeBatch(pstmt, conn, batchCount);
            conn.commit();
        } catch (SQLException e) {
            log.error("Database error", e);
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
        } finally {
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
            log.info("Function:xml模型相机预置位 Connection has released");
        }
        long after = System.currentTimeMillis();
        log.info("xml模型相机预置位 Time-consuming: {} ms", after - before);
    }

    private void setVideoPresetParams(PreparedStatement pstmt, VideoPresetEntity entity) throws SQLException {
        setStringSafe(pstmt, 1, entity.getCode());
        setStringSafe(pstmt, 2, entity.getPresetName());
        setStringSafe(pstmt, 3, entity.getPresetDes());
        setStringSafe(pstmt, 4, entity.getCameraCode());
        setSafe(pstmt, 5, entity.getPresetNo(), Types.INTEGER);
    }

    public void setStringSafe(PreparedStatement pstmt, int index, String value) throws SQLException {
        pstmt.setString(index, value == null ? "" : value);
    }
    // 工具方法：安全设置字符串（自动处理null）

    // 工具方法：安全设置数值类型
    public void setSafe(PreparedStatement pstmt, int index, Number value, int sqlType) throws SQLException {
        if (value == null) {
            pstmt.setNull(index, sqlType);
        } else {
            switch (sqlType) {
                case Types.INTEGER:
                    pstmt.setInt(index, value.intValue());
                    break;
                // 扩展其他数值类型...
                default:
                    throw new IllegalArgumentException("Unsupported type: " + sqlType);
            }
        }
    }

    // 批处理执行方法
    public void executeBatch(PreparedStatement pstmt, Connection conn, int batchCount) {
        try {
            pstmt.executeBatch();
        } catch (BatchUpdateException e) {
            handleBatchError(e, batchCount);
        } catch (SQLException e) {
            log.error("Batch execute error", e);
        }

        try {
            conn.commit();
        } catch (SQLException e) {
            log.error("Commit failed", e);
        }

        try {
            pstmt.clearBatch();
        } catch (SQLException e) {
            log.error("Clear batch failed", e);
        }
    }

    // 批处理错误处理
    public void handleBatchError(BatchUpdateException e, int batchCount) {
        int[] counts = e.getUpdateCounts();
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] == Statement.EXECUTE_FAILED) {
                log.warn("Batch item {} failed: {}", batchCount * 1000 + i, e.getMessage());
            }
        }
    }


}
