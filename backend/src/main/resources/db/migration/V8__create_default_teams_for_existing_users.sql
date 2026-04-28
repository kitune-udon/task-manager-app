INSERT INTO teams (name, description, created_by, created_at, updated_at)
SELECT
    CONCAT(u.name, 'のチーム') AS name,
    NULL AS description,
    u.id AS created_by,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u;
