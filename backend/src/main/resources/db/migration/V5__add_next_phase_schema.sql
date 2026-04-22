ALTER TABLE users
ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'MEMBER';

ALTER TABLE users
ADD CONSTRAINT chk_users_role
CHECK (role IN ('MEMBER', 'ADMIN'));

ALTER TABLE tasks
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE tasks
ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE tasks
ADD COLUMN deleted_by BIGINT;

ALTER TABLE tasks
ADD CONSTRAINT fk_tasks_deleted_by
FOREIGN KEY (deleted_by) REFERENCES users(id);

CREATE INDEX idx_tasks_deleted_at
ON tasks(deleted_at);

CREATE TABLE task_comments (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    CONSTRAINT fk_task_comments_task
        FOREIGN KEY (task_id) REFERENCES tasks(id),
    CONSTRAINT fk_task_comments_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_task_comments_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT fk_task_comments_deleted_by
        FOREIGN KEY (deleted_by) REFERENCES users(id)
);

CREATE INDEX idx_task_comments_task_created_at
ON task_comments(task_id, created_at DESC);

CREATE INDEX idx_task_comments_created_by
ON task_comments(created_by);

CREATE TABLE task_attachments (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(512) NOT NULL UNIQUE,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_type VARCHAR(20) NOT NULL,
    checksum_sha256 VARCHAR(64),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    CONSTRAINT fk_task_attachments_task
        FOREIGN KEY (task_id) REFERENCES tasks(id),
    CONSTRAINT fk_task_attachments_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_task_attachments_deleted_by
        FOREIGN KEY (deleted_by) REFERENCES users(id),
    CONSTRAINT chk_task_attachments_storage_type
        CHECK (storage_type IN ('LOCAL', 'S3')),
    CONSTRAINT chk_task_attachments_file_size
        CHECK (file_size >= 0)
);

CREATE INDEX idx_task_attachments_task_created_at
ON task_attachments(task_id, created_at DESC);

CREATE INDEX idx_task_attachments_created_by
ON task_attachments(created_by);

CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    actor_user_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    task_id BIGINT,
    summary VARCHAR(255) NOT NULL,
    detail_json JSONB,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_activity_logs_actor_user
        FOREIGN KEY (actor_user_id) REFERENCES users(id),
    CONSTRAINT fk_activity_logs_task
        FOREIGN KEY (task_id) REFERENCES tasks(id),
    CONSTRAINT chk_activity_logs_event_type
        CHECK (event_type IN (
            'TASK_CREATED',
            'TASK_UPDATED',
            'TASK_DELETED',
            'COMMENT_CREATED',
            'COMMENT_UPDATED',
            'COMMENT_DELETED',
            'ATTACHMENT_UPLOADED',
            'ATTACHMENT_DELETED'
        )),
    CONSTRAINT chk_activity_logs_target_type
        CHECK (target_type IN ('TASK', 'COMMENT', 'ATTACHMENT'))
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL,
    activity_log_id BIGINT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_recipient_user
        FOREIGN KEY (recipient_user_id) REFERENCES users(id),
    CONSTRAINT fk_notifications_activity_log
        FOREIGN KEY (activity_log_id) REFERENCES activity_logs(id),
    CONSTRAINT uk_notifications_recipient_activity
        UNIQUE (recipient_user_id, activity_log_id)
);

CREATE INDEX idx_notifications_recipient_read_created
ON notifications(recipient_user_id, is_read, created_at DESC);

CREATE INDEX idx_notifications_recipient_created
ON notifications(recipient_user_id, created_at DESC);

CREATE INDEX idx_notifications_activity_log
ON notifications(activity_log_id);

CREATE INDEX idx_activity_logs_task_created
ON activity_logs(task_id, created_at DESC);

CREATE INDEX idx_activity_logs_actor_created
ON activity_logs(actor_user_id, created_at DESC);

CREATE INDEX idx_activity_logs_event_created
ON activity_logs(event_type, created_at DESC);
