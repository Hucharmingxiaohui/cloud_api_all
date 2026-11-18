-- 部署前由维护人员在测试库验证，再执行目标库；本文件不修改区域或飞行数据。
CREATE TABLE IF NOT EXISTS construction_line (
  id varchar(64) NOT NULL,
  workspace_id varchar(64) NOT NULL,
  name varchar(100) NOT NULL,
  type varchar(32) NOT NULL DEFAULT '其他',
  color varchar(7) NOT NULL DEFAULT '#FF9F43',
  enabled tinyint(1) NOT NULL DEFAULT 1,
  notes varchar(500) DEFAULT NULL,
  tower_ids json DEFAULT NULL COMMENT '杆塔链：按顺序引用 construction_tower.id；手绘线路为 NULL',
  current_version int NOT NULL DEFAULT 1,
  archived tinyint(1) NOT NULL DEFAULT 0,
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_construction_line_ws_state (workspace_id, archived, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='独立施工线路，不是无人机航线';

CREATE TABLE IF NOT EXISTS construction_line_version (
  id varchar(64) NOT NULL,
  line_id varchar(64) NOT NULL,
  version int NOT NULL,
  name varchar(100) NOT NULL,
  type varchar(32) NOT NULL,
  color varchar(7) NOT NULL,
  enabled tinyint(1) NOT NULL,
  notes varchar(500) DEFAULT NULL,
  archived tinyint(1) NOT NULL,
  points json NOT NULL COMMENT 'WGS84折点，2至200点',
  tower_nos json DEFAULT NULL COMMENT '该版本折点对应的杆塔编号快照，如 ["A1","A2"]；手绘线路为 NULL',
  length_m double NOT NULL COMMENT '服务端球面分段长度之和（米）',
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_construction_line_version (line_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='施工线路完整不可变快照';

-- 若此前已按旧版脚本建过上述两张表（无杆塔链字段），手动执行以下增量后即可使用杆塔功能：
-- ALTER TABLE construction_line ADD COLUMN tower_ids json DEFAULT NULL COMMENT '杆塔链：按顺序引用 construction_tower.id';
-- ALTER TABLE construction_line_version ADD COLUMN tower_nos json DEFAULT NULL COMMENT '该版本折点对应的杆塔编号快照';
