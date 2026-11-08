-- 人工审核并执行；程序不执行DDL。兼容已存在的普通任务表，可重复执行。
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='df_center_ordinary_task' AND COLUMN_NAME='status_report_key')=0,
  'ALTER TABLE df_center_ordinary_task ADD COLUMN status_report_key VARCHAR(64) NULL', 'SELECT 1');
PREPARE ordinary_status_ddl FROM @ddl;
EXECUTE ordinary_status_ddl;
DEALLOCATE PREPARE ordinary_status_ddl;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='df_center_ordinary_task' AND COLUMN_NAME='status_report_time')=0,
  'ALTER TABLE df_center_ordinary_task ADD COLUMN status_report_time BIGINT NULL', 'SELECT 1');
PREPARE ordinary_status_ddl FROM @ddl;
EXECUTE ordinary_status_ddl;
DEALLOCATE PREPARE ordinary_status_ddl;
