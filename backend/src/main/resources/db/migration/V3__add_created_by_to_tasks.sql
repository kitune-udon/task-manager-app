ALTER TABLE tasks
ADD COLUMN created_by BIGINT;

UPDATE tasks
SET created_by = COALESCE(
    assigned_user_id,
    (SELECT id FROM users ORDER BY id LIMIT 1)
)
WHERE created_by IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM tasks WHERE created_by IS NULL) THEN
        RAISE EXCEPTION 'Unable to backfill tasks.created_by. Ensure at least one user exists before applying V3.';
    END IF;
END $$;

ALTER TABLE tasks
ALTER COLUMN created_by SET NOT NULL;

ALTER TABLE tasks
ADD CONSTRAINT fk_tasks_created_by
FOREIGN KEY (created_by) REFERENCES users(id);

CREATE INDEX idx_tasks_created_by
ON tasks(created_by);
