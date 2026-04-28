UPDATE tasks ta
SET team_id = t.id
FROM teams t
JOIN users u ON u.id = t.created_by
WHERE t.name = CONCAT(u.name, 'のチーム')
  AND t.created_by = ta.created_by
  AND ta.team_id IS NULL;
