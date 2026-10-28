-- Run after construction_area, construction_plan and construction_dock_coverage migrations.
CREATE TABLE IF NOT EXISTS construction_overall_route (
 id varchar(64) NOT NULL,
 workspace_id varchar(64) NOT NULL,
 dock_sn varchar(64) NOT NULL,
 wayline_id varchar(64) NOT NULL,
 version int NOT NULL,
 snapshot_json longtext NOT NULL,
 updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY (id), UNIQUE KEY uq_overall_dock_file (workspace_id,dock_sn,wayline_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS construction_overall_plan_area (
 workspace_id varchar(64) NOT NULL,
 plan_id varchar(64) NOT NULL,
 overall_id varchar(64) NOT NULL,
 area_id varchar(64) NOT NULL,
 PRIMARY KEY (workspace_id,plan_id,area_id),
 KEY ix_overall_plans (workspace_id,overall_id),
 KEY ix_overall_area (workspace_id,area_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
