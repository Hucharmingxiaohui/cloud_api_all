-- EUA点位选点计划升级；先核验/执行 upgrade_20261022_cq_task_point_snapshot.sql。
-- 仅在已确认的目标数据库执行；旧计划字段保持NULL，重新选点保存前禁止执行。
CREATE TABLE IF NOT EXISTS df_cq_eua_plan (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 plan_name VARCHAR(128) NOT NULL,
 bay_id VARCHAR(128) NULL,
 bay_name VARCHAR(255) NULL,
 route_id VARCHAR(128) NULL,
 route_name VARCHAR(255) NULL,
 sub_code VARCHAR(128) NULL,
 point_codes TEXT NULL,
 remark VARCHAR(512) NULL,
 enabled TINYINT DEFAULT 1,
 last_task_code VARCHAR(128) NULL,
 last_eua_task_id VARCHAR(128) NULL,
 last_execute_time DATETIME NULL,
 last_execute_status TINYINT NULL,
 last_execute_msg VARCHAR(1024) NULL,
 create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY idx_cq_eua_plan_bay_id (bay_id),
 KEY idx_cq_eua_plan_route_id (route_id),
 KEY idx_cq_eua_plan_last_task_code (last_task_code)
) COMMENT='重庆EUA平台计划';

SET @eua_ddl = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE df_uni_point ADD COLUMN eua_route_name VARCHAR(255) NULL COMMENT ''EUA航线展示名称；真实ID仍为wayline_id''',
 'SELECT 1') FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'df_uni_point' AND COLUMN_NAME = 'eua_route_name');
PREPARE eua_stmt FROM @eua_ddl;
EXECUTE eua_stmt;
DEALLOCATE PREPARE eua_stmt;

SET @eua_ddl = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE df_cq_eua_plan ADD COLUMN route_name VARCHAR(255) NULL COMMENT ''保存时EUA航线名称''',
 'SELECT 1') FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'df_cq_eua_plan' AND COLUMN_NAME = 'route_name');
PREPARE eua_stmt FROM @eua_ddl;
EXECUTE eua_stmt;
DEALLOCATE PREPARE eua_stmt;

SET @eua_ddl = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE df_cq_eua_plan ADD COLUMN sub_code VARCHAR(128) NULL COMMENT ''所选点位场站编码''',
 'SELECT 1') FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'df_cq_eua_plan' AND COLUMN_NAME = 'sub_code');
PREPARE eua_stmt FROM @eua_ddl;
EXECUTE eua_stmt;
DEALLOCATE PREPARE eua_stmt;

SET @eua_ddl = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE df_cq_eua_plan ADD COLUMN point_codes TEXT NULL COMMENT ''所选point_code集合JSON；NULL为待重新选点的旧计划''',
 'SELECT 1') FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'df_cq_eua_plan' AND COLUMN_NAME = 'point_codes');
PREPARE eua_stmt FROM @eua_ddl;
EXECUTE eua_stmt;
DEALLOCATE PREPARE eua_stmt;

SET @eua_ddl = (SELECT IF(COUNT(*) > 0,
 'ALTER TABLE df_cq_eua_plan MODIFY COLUMN bay_id VARCHAR(128) NULL COMMENT ''历史间隔ID，新式选点计划不使用''',
 'SELECT 1') FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'df_cq_eua_plan' AND COLUMN_NAME = 'bay_id' AND IS_NULLABLE = 'NO');
PREPARE eua_stmt FROM @eua_ddl;
EXECUTE eua_stmt;
DEALLOCATE PREPARE eua_stmt;
