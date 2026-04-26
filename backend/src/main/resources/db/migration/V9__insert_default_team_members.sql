INSERT INTO team_members (team_id, user_id, role, joined_at)
SELECT
    t.id,
    u.id,
    'OWNER',
    CURRENT_TIMESTAMP
FROM users u
JOIN teams t
  ON t.created_by = u.id
 AND t.name = CONCAT(u.name, 'のチーム');
