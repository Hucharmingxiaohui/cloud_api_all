-- 部署前人工审核；程序不自动执行DDL。开关默认关闭。
CREATE TABLE IF NOT EXISTS df_center_ordinary_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  sub_code VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
  task_code VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
  task_name VARCHAR(512) NOT NULL,
  scheduled_time BIGINT NOT NULL,
  fixed_start_time VARCHAR(19) NOT NULL,
  status VARCHAR(32) NOT NULL,
  route_id VARCHAR(128) NOT NULL,
  plan_id VARCHAR(128) NOT NULL,
  plan_config LONGTEXT NOT NULL,
  job_id VARCHAR(128) COLLATE utf8mb4_bin NULL,
  point_codes LONGTEXT NOT NULL,
  point_snapshot LONGTEXT NOT NULL,
  result_snapshot LONGTEXT NULL,
  message VARCHAR(1024) NULL,
  claimed_time BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_center_ordinary_identity (sub_code, task_code, scheduled_time),
  UNIQUE KEY uk_center_ordinary_job (job_id),
  KEY idx_center_ordinary_due (status, scheduled_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
