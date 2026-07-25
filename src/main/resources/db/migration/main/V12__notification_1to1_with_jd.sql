ALTER TABLE notification
    ADD COLUMN jd_id BIGINT NULL,
    ADD COLUMN sent  TINYINT(1) NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_notification_jd FOREIGN KEY (jd_id) REFERENCES job_description (id);