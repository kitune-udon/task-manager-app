package com.example.task;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 既存データがあるschemaへチーム管理 migration を適用した後の整合性を検証する。
 */
@SpringBootTest
@ActiveProfiles("test")
class TeamMigrationVerificationTests {

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private String schema;

    @BeforeEach
    void setUpLegacySchema() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        schema = "team_migration_" + UUID.randomUUID().toString().replace("-", "");

        migrateToVersion("5");
        insertLegacyUsersAndTasks();
        migrateToLatest();
    }

    @AfterEach
    void dropLegacySchema() {
        if (schema != null) {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    @DisplayName("MIG-01: 既存ユーザーごとに初期チームが作成される")
    void migrationCreatesInitialTeamPerExistingUser() {
        assertEquals(queryLong("select count(*) from " + table("users")), queryLong("select count(*) from " + table("teams")));
    }

    @Test
    @DisplayName("MIG-02 TEAM-L-04: 初期チーム名は <displayName>のチーム になる")
    void migratedInitialTeamUsesDisplayNameLabel() {
        assertEquals(1L, queryLong("select count(*) from " + table("teams") + " where name = 'Aliceのチーム'"));
        assertEquals(1L, queryLong("select count(*) from " + table("teams") + " where name = 'Bobのチーム'"));
    }

    @Test
    @DisplayName("MIG-03: 初期チームのdescriptionはnullになる")
    void migratedInitialTeamDescriptionIsNull() {
        assertEquals(0L, queryLong("select count(*) from " + table("teams") + " where description is not null"));
    }

    @Test
    @DisplayName("MIG-04: 初期チームには作成者本人がOWNERとして登録される")
    void migrationAddsCreatorAsOwnerToInitialTeam() {
        assertEquals(2L, queryLong("""
                select count(*)
                from %s tm
                join %s t on t.id = tm.team_id
                where tm.role = 'OWNER'
                  and tm.user_id = t.created_by
                """.formatted(table("team_members"), table("teams"))));
    }

    @Test
    @DisplayName("MIG-05 MIG-10: 既存タスクのteam_idが補完されnull残件がない")
    void migrationBackfillsTaskTeamId() {
        assertEquals(2L, queryLong("select count(*) from " + table("tasks")));
        assertEquals(0L, queryLong("select count(*) from " + table("tasks") + " where team_id is null"));
    }

    @Test
    @DisplayName("MIG-06: 既存タスクのteam_idは作成者の初期チームと一致する")
    void migrationBackfilledTaskTeamMatchesCreatorInitialTeam() {
        assertEquals(0L, queryLong("""
                select count(*)
                from %s ta
                join %s t on t.id = ta.team_id
                join %s u on u.id = ta.created_by
                where t.created_by <> ta.created_by
                   or t.name <> concat(u.name, 'のチーム')
                """.formatted(table("tasks"), table("teams"), table("users"))));
    }

    @Test
    @DisplayName("MIG-07: migration後も既存タスクの主要項目は保持される")
    void existingTaskFeaturesRemainUsableAfterMigration() {
        assertEquals("Legacy task assigned to Bob", jdbcTemplate.queryForObject(
                "select title from " + table("tasks") + " where id = 101",
                String.class
        ));
        assertEquals("TODO", jdbcTemplate.queryForObject(
                "select status from " + table("tasks") + " where id = 101",
                String.class
        ));
    }

    @Test
    @DisplayName("MIG-11: team外担当者の不整合が残らない")
    void migrationLeavesNoInvalidAssigneeMembership() {
        assertEquals(0L, queryLong("""
                select count(*)
                from %s ta
                left join %s tm
                  on tm.team_id = ta.team_id
                 and tm.user_id = ta.assigned_user_id
                where ta.assigned_user_id is not null
                  and tm.id is null
                """.formatted(table("tasks"), table("team_members"))));
    }

    @Test
    @DisplayName("MIG-12: teamごとのOWNER複数件が残らない")
    void migrationLeavesNoDuplicateOwnersPerTeam() {
        assertEquals(0L, queryLong("""
                select count(*)
                from (
                    select team_id
                    from %s
                    where role = 'OWNER'
                    group by team_id
                    having count(*) > 1
                ) duplicate_owners
                """.formatted(table("team_members"))));
    }

    @Test
    @DisplayName("MIG-13: OWNER 0件のteamが残らない")
    void migrationLeavesNoOwnerlessTeams() {
        assertEquals(0L, queryLong("""
                select count(*)
                from %s t
                where not exists (
                    select 1
                    from %s tm
                    where tm.team_id = t.id
                      and tm.role = 'OWNER'
                )
                """.formatted(table("teams"), table("team_members"))));
    }

    @Test
    @DisplayName("MIG-14: OWNER最大1名を担保する部分ユニークインデックスが存在する")
    void ownerUniqueIndexExistsAfterMigration() {
        assertEquals(1L, queryLong("""
                select count(*)
                from pg_indexes
                where schemaname = '%s'
                  and indexname = 'uk_team_members_owner_per_team'
                """.formatted(schema)));
    }

    private void migrateToVersion(String targetVersion) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .schemas(schema)
                .defaultSchema(schema)
                .target(targetVersion)
                .load()
                .migrate();
    }

    private void migrateToLatest() {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .schemas(schema)
                .defaultSchema(schema)
                .load()
                .migrate();
    }

    private void insertLegacyUsersAndTasks() {
        jdbcTemplate.update("""
                insert into %s (id, name, email, password, created_at, updated_at)
                values
                    (1, 'Alice', 'alice-legacy@example.com', 'password', current_timestamp, current_timestamp),
                    (2, 'Bob', 'bob-legacy@example.com', 'password', current_timestamp, current_timestamp)
                """.formatted(table("users")));
        jdbcTemplate.update("""
                insert into %s (
                    id,
                    title,
                    description,
                    status,
                    priority,
                    due_date,
                    assigned_user_id,
                    created_by,
                    created_at,
                    updated_at
                )
                values
                    (101, 'Legacy task assigned to Bob', 'legacy description', 'TODO', 'HIGH', current_date, 2, 1, current_timestamp, current_timestamp),
                    (102, 'Legacy task without assignee', null, 'DONE', 'LOW', current_date, null, 2, current_timestamp, current_timestamp)
                """.formatted(table("tasks")));
    }

    private long queryLong(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private String table(String tableName) {
        return schema + "." + tableName;
    }
}
