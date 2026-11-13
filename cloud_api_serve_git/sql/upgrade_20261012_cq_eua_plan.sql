-- 重庆EUA平台计划：本平台创建计划并立即下发到EUA，不向上级巡视上报
CREATE TABLE IF NOT EXISTS df_cq_eua_plan (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  plan_name VARCHAR(128) NOT NULL COMMENT '计划名称',
  bay_id VARCHAR(128) NOT NULL COMMENT '间隔ID，对应df_uni_point.bay_id',
  bay_name VARCHAR(255) DEFAULT NULL COMMENT '间隔名称',
  route_id VARCHAR(128) DEFAULT NULL COMMENT 'EUA航线ID，对应df_uni_point.wayline_id',
  remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
  enabled TINYINT DEFAULT 1 COMMENT '是否启用：1启用 0停用',
  last_task_code VARCHAR(128) DEFAULT NULL COMMENT '最近一次本地任务编码',
  last_eua_task_id VARCHAR(128) DEFAULT NULL COMMENT '最近一次EUA任务ID',
  last_execute_time DATETIME DEFAULT NULL COMMENT '最近执行时间',
  last_execute_status TINYINT DEFAULT NULL COMMENT '最近执行状态：0失败 1成功 2执行中',
  last_execute_msg VARCHAR(1024) DEFAULT NULL COMMENT '最近执行消息',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_cq_eua_plan_bay_id (bay_id),
  KEY idx_cq_eua_plan_route_id (route_id),
  KEY idx_cq_eua_plan_last_task_code (last_task_code)
) COMMENT='重庆EUA平台计划';

ALTER TABLE df_cq_dock_task_record
  ADD COLUMN report_to_patrol TINYINT DEFAULT 1 COMMENT '是否向上级巡视系统上报状态和图片：1上报 0不上报';
