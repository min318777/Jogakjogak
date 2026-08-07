ALTER TABLE notification
    ADD COLUMN status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    ADD COLUMN attempt_count  INT          NOT NULL DEFAULT 0,
    ADD COLUMN sent_at        DATETIME     NULL,
    ADD COLUMN error_message  VARCHAR(500) NULL;

UPDATE notification SET status = 'SENT', sent_at = created_at, attempt_count = 1 WHERE sent = 1;

CREATE INDEX idx_notification_status ON notification (status, created_at);
