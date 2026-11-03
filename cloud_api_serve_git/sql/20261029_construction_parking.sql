-- Reconstructed from SHOW CREATE TABLE on the configured project database (2026-11-02).
-- MySQL 8.0 collation matches the inspected deployment; contains no location data.
CREATE TABLE IF NOT EXISTS construction_parking_point (
  id varchar(36) NOT NULL,
  workspace_id varchar(64) NOT NULL,
  name varchar(80) NOT NULL,
  lng double NOT NULL,
  lat double NOT NULL,
  radius_m int NOT NULL,
  version int NOT NULL DEFAULT '1',
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_parking_workspace (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
