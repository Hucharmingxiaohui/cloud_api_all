-- 手动执行增量建表；不修改现有变电站点位表。
CREATE TABLE IF NOT EXISTS manage_solar_accessory (
  id varchar(64) NOT NULL,
  orthophoto_id varchar(64) NOT NULL,
  name varchar(100) NOT NULL,
  device_type varchar(32) NOT NULL,
  longitude double NOT NULL,
  latitude double NOT NULL,
  height double NOT NULL COMMENT '绝对椭球高，米',
  enabled tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_accessory_orthophoto (orthophoto_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS manage_solar_accessory_waypoint (
  id varchar(64) NOT NULL,
  device_id varchar(64) NOT NULL,
  name varchar(100) NOT NULL,
  sort_order int NOT NULL,
  longitude double NOT NULL,
  latitude double NOT NULL,
  height double NOT NULL COMMENT '绝对椭球高，米',
  heading double NOT NULL COMMENT '机头偏航角，度',
  pitch double NOT NULL COMMENT '云台俯仰角，度',
  focal_length double NOT NULL COMMENT '焦距，毫米',
  action varchar(32) NOT NULL,
  image_format varchar(16) NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_accessory_waypoint_order (device_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
