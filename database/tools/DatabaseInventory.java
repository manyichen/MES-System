import com.example.messystem.common.Db;
import com.example.messystem.common.DbConfig;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** 只读盘点云端 MES 数据库，为数据清理提供表规模、状态分布和外键依据。 */
public class DatabaseInventory {
    private static final List<String> SCENARIO_COLUMNS = List.of(
            "status", "type", "result", "judgement", "decision", "enabled", "read_status");

    public static void main(String[] args) throws Exception {
        StringBuilder report = new StringBuilder();
        try (Connection connection = Db.getConnection()) {
            append(report, "MES 数据库盘点时间: " + OffsetDateTime.now());
            append(report, "数据库: " + DbConfig.HOST + ":" + DbConfig.PORT + "/" + DbConfig.DATABASE);
            append(report, "只读状态: " + connection.isReadOnly());
            append(report, "");

            List<String> tables = tables(connection);
            append(report, "表数量: " + tables.size());
            for (String table : tables) {
                long count = scalar(connection, "select count(*) from " + quote(table));
                append(report, "\n[" + table + "] rows=" + count + " pk=" + primaryKeys(connection, table));
                append(report, "  columns: " + columns(connection, table));
                for (String column : scenarioColumns(connection, table)) {
                    append(report, "  " + column + ": " + distribution(connection, table, column));
                }
            }

            append(report, "\n[外键关系]");
            appendForeignKeys(connection, report);
            append(report, "\n[角色账号]");
            appendUserRoles(connection, report);
        }

        boolean quiet = args.length > 1 && "--quiet".equals(args[1]);
        if (!quiet) System.out.print(report);
        if (args.length > 0 && !args[0].isBlank()) {
            Path output = Path.of(args[0]).toAbsolutePath().normalize();
            Files.createDirectories(output.getParent());
            Files.writeString(output, report.toString(), StandardCharsets.UTF_8);
            System.out.println("\n盘点文件: " + output);
        }
    }

    private static List<String> tables(Connection connection) throws Exception {
        String sql = """
                select table_name from information_schema.tables
                where table_schema = current_schema() and table_type = 'BASE TABLE'
                  and table_name like 'mes_%'
                order by table_name
                """;
        List<String> values = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) values.add(rs.getString(1));
        }
        return values;
    }

    private static List<String> primaryKeys(Connection connection, String table) throws Exception {
        String sql = """
                select kcu.column_name
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on kcu.constraint_name = tc.constraint_name and kcu.table_schema = tc.table_schema
                where tc.table_schema = current_schema() and tc.table_name = ?
                  and tc.constraint_type = 'PRIMARY KEY'
                order by kcu.ordinal_position
                """;
        List<String> values = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) values.add(rs.getString(1));
            }
        }
        return values;
    }

    private static List<String> scenarioColumns(Connection connection, String table) throws Exception {
        String sql = """
                select column_name from information_schema.columns
                where table_schema = current_schema() and table_name = ?
                order by ordinal_position
                """;
        List<String> values = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String column = rs.getString(1).toLowerCase();
                    if (SCENARIO_COLUMNS.stream().anyMatch(column::contains)) values.add(column);
                }
            }
        }
        return values;
    }

    private static List<String> columns(Connection connection, String table) throws Exception {
        String sql = """
                select column_name, data_type
                from information_schema.columns
                where table_schema = current_schema() and table_name = ?
                order by ordinal_position
                """;
        List<String> values = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) values.add(rs.getString(1) + ":" + rs.getString(2));
            }
        }
        return values;
    }

    private static String distribution(Connection connection, String table, String column) throws Exception {
        String sql = "select coalesce(" + quote(column) + "::text, '<NULL>'), count(*) from "
                + quote(table) + " group by " + quote(column) + " order by 1";
        List<String> values = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) values.add(rs.getString(1) + "=" + rs.getLong(2));
        }
        return values.toString();
    }

    private static void appendForeignKeys(Connection connection, StringBuilder report) throws Exception {
        String sql = """
                select tc.table_name, kcu.column_name, ccu.table_name, ccu.column_name,
                       rc.delete_rule
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on kcu.constraint_name = tc.constraint_name and kcu.constraint_schema = tc.constraint_schema
                join information_schema.constraint_column_usage ccu
                  on ccu.constraint_name = tc.constraint_name and ccu.constraint_schema = tc.constraint_schema
                join information_schema.referential_constraints rc
                  on rc.constraint_name = tc.constraint_name and rc.constraint_schema = tc.constraint_schema
                where tc.constraint_type = 'FOREIGN KEY' and tc.table_schema = current_schema()
                  and tc.table_name like 'mes_%'
                order by tc.table_name, kcu.column_name
                """;
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                append(report, "  " + rs.getString(1) + "." + rs.getString(2)
                        + " -> " + rs.getString(3) + "." + rs.getString(4)
                        + " on delete " + rs.getString(5));
            }
        }
    }

    private static void appendUserRoles(Connection connection, StringBuilder report) throws Exception {
        String sql = """
                select coalesce(role_code, '<NULL>'), count(*), string_agg(username, ', ' order by username)
                from mes_user group by role_code order by role_code
                """;
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) append(report, "  " + rs.getString(1) + "=" + rs.getLong(2) + " [" + rs.getString(3) + "]");
        }
    }

    private static long scalar(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static void append(StringBuilder report, String line) {
        report.append(line).append(System.lineSeparator());
    }
}
