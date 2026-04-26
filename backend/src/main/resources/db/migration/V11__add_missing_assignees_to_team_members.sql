INSERT INTO team_members (team_id, user_id, role, joined_at)
SELECT DISTINCT
    ta.team_id,
    ta.assigned_user_id,
    'MEMBER',
    CURRENT_TIMESTAMP
FROM tasks ta
LEFT JOIN team_members tm
  ON tm.team_id = ta.team_id
 AND tm.user_id = ta.assigned_user_id
WHERE ta.assigned_user_id IS NOT NULL
  AND ta.team_id IS NOT NULL
  AND tm.id IS NULL;
