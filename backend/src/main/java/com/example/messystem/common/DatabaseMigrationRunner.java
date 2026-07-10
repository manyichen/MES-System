package com.example.messystem.common;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseMigrationRunner {
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
