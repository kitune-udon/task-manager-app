CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_teams_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT uk_teams_created_by_name
        UNIQUE (created_by, name)
);

CREATE TABLE team_members (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_team_members_team
        FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_team_members_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_team_members_team_user
        UNIQUE (team_id, user_id),
    CONSTRAINT chk_team_members_role
        CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'))
);

CREATE UNIQUE INDEX uk_team_members_owner_per_team
ON team_members(team_id)
WHERE role = 'OWNER';
