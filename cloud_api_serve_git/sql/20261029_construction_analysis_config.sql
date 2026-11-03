-- Reconstructed from SHOW CREATE TABLE on the configured project database (2026-11-02).
-- Creates missing tables only; does not repair existing schemas or backfill task snapshots.
CREATE TABLE IF NOT EXISTS construction_analysis_config (
  workspace_id varchar(64) NOT NULL,
  revision int NOT NULL DEFAULT '0',
  config_json longtext NOT NULL,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
