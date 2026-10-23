-- 手动执行增量建表；施工区域真实标定（真实地图绘制入库），不影响现有表。
-- 坐标统一存储 WGS84 经纬度（度）；高德地图(GCJ02)绘制结果须先转换为 WGS84 再保存。
CREATE TABLE IF NOT EXISTS construction_area (
  id varchar(64) NOT NULL COMMENT '区域ID，UUID',
  workspace_id varchar(64) NOT NULL COMMENT '工作空间ID',
  name varchar(100) NOT NULL COMMENT '区域名称',
  category varchar(32) NOT NULL DEFAULT '基础施工' COMMENT '施工类型',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0暂停',
  notes varchar(500) DEFAULT NULL COMMENT '备注',
  current_version int NOT NULL DEFAULT 1 COMMENT '当前版本号，从1起递增',
  archived tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否归档：1归档；归档不删除历史数据',
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_construction_area_ws (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工地巡检施工区域主表';

CREATE TABLE IF NOT EXISTS construction_area_version (
  id varchar(64) NOT NULL COMMENT '版本行ID，UUID',
  area_id varchar(64) NOT NULL COMMENT '区域ID',
  version int NOT NULL COMMENT '版本号，(area_id, version) 唯一',
  boundary json NOT NULL COMMENT '边界顶点，WGS84：[{"lng":经度,"lat":纬度},...]，3-100点',
  area_sqm double NOT NULL COMMENT '边界面积，平方米（服务端计算）',
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '版本创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_construction_area_version (area_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工地巡检施工区域版本历史';
