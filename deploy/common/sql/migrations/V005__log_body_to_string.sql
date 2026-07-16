-- GitHub #39: log body can exceed VARCHAR(65533) UTF-8 bytes (esp. CJK).
-- STRING default soft limit is ~1MB (BE string_type_length_soft_limit_bytes).
-- Heavyweight schema change: rewrites tablets; monitor with SHOW ALTER TABLE COLUMN.
-- Fresh installs take STRING from databuff.sql; this upgrades existing DBs.

USE databuff;

ALTER TABLE log_dc_record MODIFY COLUMN `body` STRING COMMENT 'log message text (STRING; ingest truncates by Java String.length)';
