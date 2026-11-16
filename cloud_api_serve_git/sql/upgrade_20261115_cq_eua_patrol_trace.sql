-- Run after the EUA plan/point snapshot upgrades, against an explicitly approved database only.
-- No historical source guessing or business_id rewriting. Safe to re-run.
SET @ddl = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE df_cq_dock_task_picture ADD COLUMN analysis_report_to_patrol TINYINT NULL COMMENT ''Current analysis generation report intent; NULL keeps legacy behavior''', 'SELECT 1')
 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='df_cq_dock_task_picture' AND COLUMN_NAME='analysis_report_to_patrol');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE df_cq_eua_plan ADD COLUMN plan_source VARCHAR(16) NOT NULL DEFAULT ''PLATFORM''', 'SELECT 1')
 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='df_cq_eua_plan' AND COLUMN_NAME='plan_source');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE df_cq_eua_plan ADD COLUMN patrol_task_code VARCHAR(128) NULL', 'SELECT 1')
 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='df_cq_eua_plan' AND COLUMN_NAME='patrol_task_code');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE df_cq_eua_plan ADD UNIQUE KEY uk_eua_patrol_identity (sub_code, patrol_task_code)', 'SELECT 1')
 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='df_cq_eua_plan' AND INDEX_NAME='uk_eua_patrol_identity');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE df_cq_dock_task_record ADD COLUMN task_source VARCHAR(16) NOT NULL DEFAULT ''PLATFORM''', 'SELECT 1')
 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='df_cq_dock_task_record' AND COLUMN_NAME='task_source');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE df_cq_dock_task_record ADD COLUMN plan_id BIGINT NULL', 'SELECT 1')
 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='df_cq_dock_task_record' AND COLUMN_NAME='plan_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE df_cq_dock_task_record ADD KEY idx_eua_task_plan_id (plan_id)', 'SELECT 1')
 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='df_cq_dock_task_record' AND INDEX_NAME='idx_eua_task_plan_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
