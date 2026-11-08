-- 上级POINT=3任务精确点位快照。先执行此升级，再启动新后端；历史/平台任务保持NULL。
SET @cq_point_snapshot_ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE df_cq_dock_task_record ADD COLUMN point_codes TEXT NULL COMMENT ''上级任务请求point_code集合JSON快照；NULL使用间隔过滤'' AFTER bay_id',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'df_cq_dock_task_record'
      AND COLUMN_NAME = 'point_codes'
);
PREPARE cq_point_snapshot_stmt FROM @cq_point_snapshot_ddl;
EXECUTE cq_point_snapshot_stmt;
DEALLOCATE PREPARE cq_point_snapshot_stmt;
