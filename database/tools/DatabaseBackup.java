import com.example.messystem.common.Db;
import com.example.messystem.common.DbConfig;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.postgresql.PGConnection;

/** 将清理前的全部 MES 表按 JSON Lines 格式压缩备份到本地文件。 */
public class DatabaseBackup {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || args[0].isBlank()) throw new IllegalArgumentException("必须指定备份文件路径");
        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());
        try (Connection connection = Db.getConnection();
             OutputStream file = Files.newOutputStream(output);
             ZipOutputStream zip = new ZipOutputStream(file, StandardCharsets.UTF_8)) {
            List<String> tables = tables(connection);
            zip.putNextEntry(new ZipEntry("_manifest.txt"));
            String manifest = "time=" + OffsetDateTime.now() + "\n"
                    + "database=" + DbConfig.HOST + ":" + DbConfig.PORT + "/" + DbConfig.DATABASE + "\n"
                    + "format=one PostgreSQL row_to_json object per line\n"
                    + "tables=" + tables.size() + "\n";
            zip.write(manifest.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            PGConnection pg = connection.unwrap(PGConnection.class);
            for (String table : tables) {
                zip.putNextEntry(new ZipEntry(table + ".jsonl"));
                String sql = "copy (select row_to_json(t)::text from " + quote(table) + " t) to stdout";
                long rows = pg.getCopyAPI().copyOut(sql, zip);
                zip.closeEntry();
                System.out.println(table + "=" + rows);
            }
        }
        System.out.println("备份完成: " + output + " bytes=" + Files.size(output));
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

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
