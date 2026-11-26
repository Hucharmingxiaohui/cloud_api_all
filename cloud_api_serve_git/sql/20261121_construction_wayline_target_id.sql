-- Run in the platform database before saving line waylines.
-- UUID area IDs remain unchanged. Namespaced line UUIDs require 41 characters.
-- CREATE TABLE IF NOT EXISTS does not upgrade existing tables.
ALTER TABLE construction_area_wayline MODIFY COLUMN area_id varchar(64) NOT NULL;
SHOW COLUMNS FROM construction_area_wayline LIKE 'area_id';