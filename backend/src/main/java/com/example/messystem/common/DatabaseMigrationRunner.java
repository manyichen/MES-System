package com.example.messystem.common;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseMigrationRunner {
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";
    private static final String DEFAULT_ADMIN_ROLE = "SYSTEM_ADMIN";
    private static final String DEFAULT_SUPER_ADMIN_USERNAME = "superadmin";
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    public static void main(String[] args) throws Exception {
        Path migrationFile = args.length > 0
                ? Path.of(args[0])
                : Path.of("../database/design_output/mes_shared_field_migration.sql");

        if (!Files.isRegularFile(migrationFile)) {
            throw new IllegalArgumentException("Migration file not found: " + migrationFile.toAbsolutePath());
        }

        List<String> sqlStatements = splitSql(Files.readString(migrationFile, StandardCharsets.UTF_8));
        System.out.println("Database: " + DbConfig.getJdbcUrl());
        System.out.println("Migration file: " + migrationFile.toAbsolutePath().normalize());
        System.out.println("Statements: " + sqlStatements.size());

        try (Connection connection = Db.getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : sqlStatements) {
                System.out.println("Executing: " + firstLine(sql));
                statement.execute(sql);
            }
            System.out.println("Column mes_work_order.product_id: " + columnExists(connection, "mes_work_order", "product_id"));
            System.out.println("Column mes_work_order.batch_no: " + columnExists(connection, "mes_work_order", "batch_no"));
            System.out.println("Column mes_work_report.batch_no: " + columnExists(connection, "mes_work_report", "batch_no"));
            System.out.println("Column mes_quality_inspection.work_report_id: "
                    + columnExists(connection, "mes_quality_inspection", "work_report_id"));
            if (migrationFile.getFileName().toString().contains("auth_user_migration")) {
                ensureSystemAdmin(connection);
                printUserTableSummary(connection);
            }
            if (migrationFile.getFileName().toString().contains("super_admin")) {
                ensureSuperAdmin(connection);
                printUserTableSummary(connection);
            }
        }

        System.out.println("Migration completed.");
    }

    public static boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        String sql = """
                select 1
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = '%s'
                  and column_name = '%s'
                """.formatted(tableName, columnName);
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            return rs.next();
        }
    }

    private static void ensureSystemAdmin(Connection connection) throws Exception {
        String passwordHash = PasswordHasher.hash(DEFAULT_ADMIN_PASSWORD);
        String sql = """
                insert into mes_user
                    (username, real_name, role_code, department, enabled, password_hash, updated_at)
                values (?, ?, ?, ?, 1, ?, current_timestamp)
                on conflict (username) do update
                set real_name = excluded.real_name,
                    role_code = excluded.role_code,
                    department = excluded.department,
                    enabled = 1,
                    password_hash = excluded.password_hash,
                    updated_at = current_timestamp
                returning user_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DEFAULT_ADMIN_USERNAME);
            statement.setString(2, "System Administrator");
            statement.setString(3, DEFAULT_ADMIN_ROLE);
            statement.setString(4, "System Administration");
            statement.setString(5, passwordHash);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Admin user ensured: username=" + DEFAULT_ADMIN_USERNAME
                            + ", user_id=" + rs.getLong("user_id")
                            + ", role_code=" + DEFAULT_ADMIN_ROLE);
                }
            }
        }
    }

    /** Provisions the all-access account without committing a plaintext password to source control. */
    private static void ensureSuperAdmin(Connection connection) throws Exception {
        String username = DbConfig.getValue("MES_SUPER_ADMIN_USERNAME", DEFAULT_SUPER_ADMIN_USERNAME).trim();
        String password = DbConfig.getValue("MES_SUPER_ADMIN_PASSWORD", "");
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "MES_SUPER_ADMIN_PASSWORD is required when applying a super_admin migration");
        }
        if (password.length() < 12) {
            throw new IllegalArgumentException("MES_SUPER_ADMIN_PASSWORD must contain at least 12 characters");
        }

        long userId;
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into mes_user
                    (username, real_name, role_code, department, account_type, position_name,
                     enabled, password_hash, password_updated_at, failed_login_count, locked_until, updated_at)
                values (?, ?, ?, ?, 'SYSTEM', ?, 1, ?, current_timestamp, 0, null, current_timestamp)
                on conflict (username) do update
                set real_name = excluded.real_name,
                    role_code = excluded.role_code,
                    department = excluded.department,
                    account_type = excluded.account_type,
                    position_name = excluded.position_name,
                    enabled = 1,
                    password_hash = excluded.password_hash,
                    password_updated_at = current_timestamp,
                    failed_login_count = 0,
                    locked_until = null,
                    updated_at = current_timestamp
                returning user_id
                """)) {
            statement.setString(1, username);
            statement.setString(2, "Super Administrator");
            statement.setString(3, SUPER_ADMIN_ROLE);
            statement.setString(4, "System Administration");
            statement.setString(5, "Super Administrator");
            statement.setString(6, PasswordHasher.hash(password));
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                userId = rs.getLong(1);
            }
        }

        try (PreparedStatement delete = connection.prepareStatement(
                "delete from mes_user_role where user_id = ?")) {
            delete.setLong(1, userId);
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement("""
                insert into mes_user_role (user_id, role_id, assigned_by, assigned_at)
                select ?, role_id, ?, current_timestamp from mes_role
                where role_code = ? and enabled = 1
                """)) {
            insert.setLong(1, userId);
            insert.setLong(2, userId);
            insert.setString(3, SUPER_ADMIN_ROLE);
            if (insert.executeUpdate() != 1) {
                throw new IllegalStateException("SUPER_ADMIN role was not created by the migration");
            }
        }
        System.out.println("Super admin ensured: username=" + username + ", user_id=" + userId);
    }

    private static void printUserTableSummary(Connection connection) throws Exception {
        System.out.println("mes_user primary key: " + primaryKeyColumns(connection, "mes_user"));
        String[] coreColumns = {
                "user_id",
                "username",
                "password_hash",
                "role_code",
                "real_name",
                "department",
                "phone",
                "enabled",
                "created_at",
                "updated_at",
                "last_login_at"
        };
        for (String column : coreColumns) {
            System.out.println("Column mes_user." + column + ": " + columnExists(connection, "mes_user", column));
        }
    }

    private static String primaryKeyColumns(Connection connection, String tableName) throws Exception {
        String sql = """
                select kcu.column_name
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on tc.constraint_name = kcu.constraint_name
                 and tc.table_schema = kcu.table_schema
                where tc.table_schema = current_schema()
                  and tc.table_name = ?
                  and tc.constraint_type = 'PRIMARY KEY'
                order by kcu.ordinal_position
                """;
        List<String> columns = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    columns.add(rs.getString("column_name"));
                }
            }
        }
        return columns.isEmpty() ? "(none)" : String.join(", ", columns);
    }

    private static List<String> splitSql(String rawSql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : rawSql.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            current.append(line).append(System.lineSeparator());
            if (trimmed.endsWith(";")) {
                String sql = current.toString().trim();
                statements.add(sql.substring(0, sql.length() - 1).trim());
                current.setLength(0);
            }
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            statements.add(tail);
        }
        return statements;
    }

    private static String firstLine(String sql) {
        String[] lines = sql.split("\\R", 2);
        return lines[0].trim();
    }
}
