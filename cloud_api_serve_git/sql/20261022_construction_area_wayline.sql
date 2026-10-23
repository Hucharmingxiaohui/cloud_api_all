-- Apply after 20261021_construction_area.sql. Does not duplicate wayline_file records.
CREATE TABLE IF NOT EXISTS construction_area_wayline (
  id varchar(36) NOT NULL PRIMARY KEY,
  workspace_id varchar(64) NOT NULL,
  area_id varchar(36) NOT NULL,
  wayline_id varchar(64) NOT NULL,
  area_version int NOT NULL,
  file_sign varchar(64) NOT NULL,
  source varchar(16) NOT NULL,
  request_id varchar(64) NOT NULL,
  request_hash varchar(64) NOT NULL,
  points_json longtext NOT NULL,
  height_mode varchar(64) NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_construction_wayline (workspace_id, area_id, wayline_id),
  UNIQUE KEY uq_construction_request (workspace_id, area_id, request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
