CREATE TABLE IF NOT EXISTS backend.notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     TEXT        NOT NULL,
    recipient   TEXT,
    subject     TEXT        NOT NULL,
    body        TEXT,
    channel     TEXT        NOT NULL,
    is_read     BOOLEAN     NOT NULL DEFAULT FALSE,
    qr_code_id  TEXT,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

-- Primary query path: fetch all notifications for a user ordered newest first
CREATE INDEX IF NOT EXISTS idx_notifications_user_id_created_at
    ON backend.notifications (user_id, created_at DESC);

-- Used by unread count query
CREATE INDEX IF NOT EXISTS idx_notifications_user_id_is_read
    ON backend.notifications (user_id, is_read);
