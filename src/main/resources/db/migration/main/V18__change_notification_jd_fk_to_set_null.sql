ALTER TABLE notification
    DROP FOREIGN KEY fk_notification_jd;

ALTER TABLE notification
    ADD CONSTRAINT fk_notification_jd FOREIGN KEY (jd_id) REFERENCES job_description (id) ON DELETE SET NULL;
