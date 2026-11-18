-- 手动标定的集电线路杆塔：线路按“起始杆塔→结束杆塔”引用杆塔链生成折线；坐标 WGS84。
CREATE TABLE IF NOT EXISTS construction_tower (
  id VARCHAR(36) NOT NULL,
  workspace_id VARCHAR(64) NOT NULL,
  tower_no VARCHAR(32) NOT NULL COMMENT '杆塔编号，如 A1、A2；同工作空间唯一',
  lng DOUBLE NOT NULL,
  lat DOUBLE NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_construction_tower_no (workspace_id, tower_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工地巡检集电线路杆塔标定';
