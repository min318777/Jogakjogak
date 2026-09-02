SET @fk_name = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_description'
      AND COLUMN_NAME = 'notification_id'
      AND REFERENCED_TABLE_NAME = 'notification'
);

SET @drop_fk_sql = CONCAT('ALTER TABLE job_description DROP FOREIGN KEY ', @fk_name);
PREPARE stmt FROM @drop_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE job_description DROP COLUMN notification_id;
