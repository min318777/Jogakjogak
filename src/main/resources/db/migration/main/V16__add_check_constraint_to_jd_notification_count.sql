ALTER TABLE job_description
    ADD CONSTRAINT ck_jd_notification_count_range CHECK (notification_count BETWEEN 0 AND 3);
