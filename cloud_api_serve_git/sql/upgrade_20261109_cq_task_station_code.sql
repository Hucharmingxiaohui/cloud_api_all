-- 现场先执行本升级，再部署后端。历史记录保持NULL，不从配置/当前点位表自动回填。
-- 仅可依据核实过的原始上级任务报文，人工回填对应EUA任务记录的Code。
SET @cq_task_station_ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE df_cq_dock_task_record ADD COLUMN sub_code VARCHAR(64) NULL COMMENT ''接收任务时上级Code快照；历史NULL禁止上报'' AFTER business_id',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'df_cq_dock_task_record'
      AND COLUMN_NAME = 'sub_code'
);
PREPARE cq_task_station_stmt FROM @cq_task_station_ddl;
EXECUTE cq_task_station_stmt;
DEALLOCATE PREPARE cq_task_station_stmt;
