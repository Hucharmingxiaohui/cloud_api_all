-- 点位标准库新增独立的航线点位名称；仅在已确认的目标数据库执行。
SET @wayline_point_ddl = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE df_uni_point ADD COLUMN wayline_point_name VARCHAR(255) NULL COMMENT ''航线点位名称'' AFTER point_name',
 'SELECT 1') FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'df_uni_point' AND COLUMN_NAME = 'wayline_point_name');
PREPARE wayline_point_stmt FROM @wayline_point_ddl;
EXECUTE wayline_point_stmt;
DEALLOCATE PREPARE wayline_point_stmt;
