package com.lab.taskmanager.config;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class LocalH2SchemaCompatibilityRunner implements ApplicationRunner {

    private static final String TASKS_TABLE = "TASKS";
    private static final String SCOPE_COLUMN = "SCOPE";
    private static final String ASSIGNEE_COLUMN = "ASSIGNEE_ID";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists(TASKS_TABLE)) {
            return;
        }

        Set<String> columns = loadColumnNames(TASKS_TABLE);
        ensureScopeColumn(columns);
        ensureAssigneeColumn(columns);
        backfillLegacyTaskRows();
    }

    private void ensureScopeColumn(Set<String> columns) {
        if (columns.contains(SCOPE_COLUMN)) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE tasks ADD COLUMN scope VARCHAR(20)");
        log.info("Added missing tasks.scope column for local H2 compatibility");
    }

    private void ensureAssigneeColumn(Set<String> columns) {
        if (columns.contains(ASSIGNEE_COLUMN)) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE tasks ADD COLUMN assignee_id BIGINT");
        log.info("Added missing tasks.assignee_id column for local H2 compatibility");
    }

    private void backfillLegacyTaskRows() {
        jdbcTemplate.update("UPDATE tasks SET scope = 'PERSONAL' WHERE scope IS NULL");
        jdbcTemplate.update("""
                UPDATE tasks
                   SET assignee_id = owner_id
                 WHERE assignee_id IS NULL
                   AND team_id IS NULL
                """);
        jdbcTemplate.execute("ALTER TABLE tasks ALTER COLUMN scope SET NOT NULL");
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM INFORMATION_SCHEMA.TABLES
                 WHERE TABLE_SCHEMA = 'PUBLIC'
                   AND TABLE_NAME = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private Set<String> loadColumnNames(String tableName) {
        return jdbcTemplate.queryForList("""
                SELECT COLUMN_NAME
                  FROM INFORMATION_SCHEMA.COLUMNS
                 WHERE TABLE_SCHEMA = 'PUBLIC'
                   AND TABLE_NAME = ?
                """, String.class, tableName)
                .stream()
                .map(column -> column.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
