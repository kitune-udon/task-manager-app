ALTER TABLE tasks
ALTER COLUMN team_id SET NOT NULL;

ALTER TABLE tasks
ADD CONSTRAINT fk_tasks_team
FOREIGN KEY (team_id) REFERENCES teams(id);

CREATE INDEX idx_teams_created_by
ON teams(created_by);

CREATE INDEX idx_team_members_team_id
ON team_members(team_id);

CREATE INDEX idx_team_members_user_id
ON team_members(user_id);

CREATE INDEX idx_team_members_team_role
ON team_members(team_id, role);

CREATE INDEX idx_tasks_team_id
ON tasks(team_id);

CREATE INDEX idx_tasks_team_status
ON tasks(team_id, status);

CREATE INDEX idx_tasks_team_assigned_user
ON tasks(team_id, assigned_user_id);
