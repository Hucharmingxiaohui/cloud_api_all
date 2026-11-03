-- Preserve source KMZ and reuse the generated execution version for the same source revision.
CREATE TABLE IF NOT EXISTS construction_wayline_preparation (
  workspace_id VARCHAR(64) NOT NULL,
  source_wayline_id VARCHAR(64) NOT NULL,
  source_sign VARCHAR(64) NOT NULL,
  prepared_wayline_id VARCHAR(64) NOT NULL,
  prepared_sign VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (workspace_id, source_wayline_id, source_sign),
  UNIQUE KEY uk_prepared_wayline (workspace_id, prepared_wayline_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
