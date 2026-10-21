package com.fms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Backup and restore from the app (Administration -> Backup and restore).
 *
 * A backup is one .zip file containing:
 *   manifest.json        what it is, when, and the database version
 *   tables/NAME.json     every row of every IMS table
 *   files/...            invoice PDFs and the logo
 *
 * Restoring replaces all data in one transaction (all or nothing), then
 * resets the ID counters and number sequences, then restores the files.
 * A safety backup of the current data is saved first, under backups/.
 */
@Service
public class BackupService {

    /** Parents before children, so restoring inserts in this order and deletes in reverse. */
    private static final List<String> TABLES = List.of(
            "APP_CONFIG", "LOCATION", "APP_USER", "CASH_ACCOUNT", "COST_CENTER", "MERCHANT", "COMPANY", "CLIENT", "PROSPECT", "PRODUCT_CLASS", "PRODUCT",
            "CLIENT_CLASS_DISCOUNT", "ORDER_LINE", "ORDER_INFO", "PAYMENT", "STOCK_ADJUSTMENT", "SHELF", "SHELF_STOCK",
            "PURCHASE_ORDER", "PURCHASE_ORDER_LINE", "MONEY_MOVEMENT", "PAYROLL_TAX_BRACKET", "EMPLOYEE",
            "PAYROLL_RUN", "PAYROLL_LINE", "ASSET", "BOM_LINE", "PRODUCTION_ORDER", "PRODUCTION_LINE", "TAX_PAYMENT",
            "ACC_ACCOUNT", "ACC_ENTRY", "ACC_LINE", "USER_ACCESS", "USER_DASHBOARD", "STOCK_COUNT", "STOCK_COUNT_LINE",
            "REMINDER_LOG", "BUDGET_LINE", "PRICE_LIST", "PRICE_LIST_ITEM", "EXCHANGE_RATE", "STOCK_TRANSFER", "SERIAL_NUMBER", "AUDIT_LOG");
    private static final List<String> SEQUENCES = List.of("ORDER_CODE_SEQ", "PO_CODE_SEQ", "PRODUCTION_SEQ", "ACC_ENTRY_SEQ", "STOCK_COUNT_SEQ", "TRANSFER_SEQ");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

    public record Summary(String created, String schemaVersion, Map<String, Integer> rows, int files) {
    }

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final StorageService storage;
    private final AuditService audit;
    private final ObjectMapper json = new ObjectMapper();

    public BackupService(JdbcTemplate jdbc, TransactionTemplate tx, StorageService storage, AuditService audit) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.storage = storage;
        this.audit = audit;
    }

    public String fileName() {
        return "Arkon-backup_" + LocalDateTime.now().format(STAMP) + ".zip";
    }

    // ------------------------------------------------------------------ export

    /** Writes a complete backup to out. */
    public void export(OutputStream out) {
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("app", "Arkon");
            manifest.put("created", LocalDateTime.now().toString());
            manifest.put("schemaVersion", schemaVersion());
            Map<String, Long> seqs = new LinkedHashMap<>();
            for (String seq : SEQUENCES) {
                Long last = jdbc.query("SELECT LAST_NUMBER FROM USER_SEQUENCES WHERE SEQUENCE_NAME = ?",
                        rs -> rs.next() ? rs.getLong(1) : null, seq);
                if (last != null) {
                    seqs.put(seq, last);
                }
            }
            manifest.put("sequences", seqs);
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String table : tables()) {
                List<Map<String, Object>> rows = readTable(table);
                counts.put(table, rows.size());
                zip.putNextEntry(new ZipEntry("tables/" + table + ".json"));
                zip.write(json.writeValueAsBytes(rows));
                zip.closeEntry();
            }
            manifest.put("rows", counts);
            int files = 0;
            Path root = storage.root();
            for (String dir : List.of("invoices", "logo")) {
                Path d = root.resolve(dir);
                if (!Files.isDirectory(d)) {
                    continue;
                }
                try (Stream<Path> walk = Files.walk(d)) {
                    for (Path f : walk.filter(Files::isRegularFile).toList()) {
                        zip.putNextEntry(new ZipEntry("files/" + root.relativize(f).toString().replace('\\', '/')));
                        Files.copy(f, zip);
                        zip.closeEntry();
                        files++;
                    }
                }
            }
            manifest.put("files", files);
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(json.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest));
            zip.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The IMS tables in restore order, plus any table added later (except Flyway's). */
    private List<String> tables() {
        List<String> existing = jdbc.queryForList("SELECT TABLE_NAME FROM USER_TABLES", String.class);
        List<String> out = new ArrayList<>();
        for (String t : TABLES) {
            if (existing.contains(t)) {
                out.add(t);
            }
        }
        for (String t : existing) {
            if (!out.contains(t) && !t.toLowerCase(Locale.ROOT).contains("flyway") && !t.startsWith("BIN$")) {
                out.add(t);
            }
        }
        return out;
    }

    private Map<String, String> columnTypes(String table) {
        Map<String, String> m = new LinkedHashMap<>();
        jdbc.query("SELECT COLUMN_NAME, DATA_TYPE FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? ORDER BY COLUMN_ID",
                rs -> { m.put(rs.getString(1), rs.getString(2)); }, table);
        return m;
    }

    private List<Map<String, Object>> readTable(String table) {
        Map<String, String> types = columnTypes(table);
        return jdbc.query("SELECT * FROM " + table, (ResultSet rs, int n) -> {
            ResultSetMetaData md = rs.getMetaData();
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                String col = md.getColumnName(i);
                String type = types.getOrDefault(col, "VARCHAR2");
                Object v;
                if (type.startsWith("NUMBER") || type.equals("FLOAT")) {
                    BigDecimal b = rs.getBigDecimal(i);
                    v = b == null ? null : b.toPlainString();
                } else if (type.equals("DATE") || type.startsWith("TIMESTAMP")) {
                    Timestamp t = rs.getTimestamp(i);
                    v = t == null ? null : t.toLocalDateTime().toString();
                } else {
                    v = rs.getString(i);
                }
                row.put(col, v);
            }
            return row;
        });
    }

    private String schemaVersion() {
        return jdbc.query("SELECT \"version\" FROM \"flyway_schema_history\" WHERE \"success\" = 1 AND \"version\" IS NOT NULL"
                + " ORDER BY \"installed_rank\" DESC FETCH FIRST 1 ROWS ONLY", rs -> rs.next() ? rs.getString(1) : "?");
    }

    // ----------------------------------------------------------------- restore

    /**
     * Restores a backup. Checks it first; saves a safety backup of the current
     * data; replaces all data in one transaction; resets counters; restores files.
     * Returns what was restored.
     */
    public Summary restore(InputStream in, String username) {
        Map<String, Object> manifest = null;
        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        Path staged;
        try {
            staged = Files.createTempDirectory("ims-restore");
            try (ZipInputStream zip = new ZipInputStream(in)) {
                ZipEntry e;
                while ((e = zip.getNextEntry()) != null) {
                    String name = e.getName();
                    if (e.isDirectory()) {
                        continue;
                    }
                    if (name.equals("manifest.json")) {
                        manifest = json.readValue(zip.readAllBytes(), new TypeReference<Map<String, Object>>() { });
                    } else if (name.startsWith("tables/") && name.endsWith(".json")) {
                        String table = name.substring(7, name.length() - 5).toUpperCase(Locale.ROOT);
                        data.put(table, json.readValue(zip.readAllBytes(), new TypeReference<List<Map<String, Object>>>() { }));
                    } else if (name.startsWith("files/")) {
                        Path target = staged.resolve(name.substring(6)).normalize();
                        if (!target.startsWith(staged)) {
                            throw new OrderException("The backup file contains an unsafe path.");
                        }
                        Files.createDirectories(target.getParent());
                        Files.copy(zip, target);
                    }
                }
            }
        } catch (IOException e) {
            throw new OrderException("This isn't a valid Arkon backup file.");
        }
        if (manifest == null || !java.util.Set.of("IMS", "Kompas", "Arkon").contains(String.valueOf(manifest.get("app"))) || data.isEmpty()) {
            throw new OrderException("This isn't a valid Arkon backup file.");
        }
        String current = schemaVersion();
        if (!String.valueOf(manifest.get("schemaVersion")).equals(current)) {
            throw new OrderException("This backup is from database version " + manifest.get("schemaVersion")
                    + ", but this app is at version " + current + ". Restore it with the matching app version.");
        }

        // Safety copy of what's there now.
        Path safety = storage.root().resolve("backups").resolve("before-restore_" + LocalDateTime.now().format(STAMP) + ".zip");
        try {
            Files.createDirectories(safety.getParent());
            try (OutputStream out = Files.newOutputStream(safety)) {
                export(out);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // All data in one transaction: either everything is restored or nothing changes.
        List<String> order = tables();
        tx.executeWithoutResult(status -> {
            for (int i = order.size() - 1; i >= 0; i--) {
                jdbc.update("DELETE FROM " + order.get(i));
            }
            for (String table : order) {
                List<Map<String, Object>> rows = data.get(table);
                if (rows != null && !rows.isEmpty()) {
                    insertRows(table, rows);
                }
            }
        });

        // ID counters continue after the restored rows; number sequences continue too.
        jdbc.query("SELECT TABLE_NAME, COLUMN_NAME FROM USER_TAB_IDENTITY_COLS", rs -> {
            String t = rs.getString(1);
            String c = rs.getString(2);
            if (order.contains(t)) {
                jdbc.execute("ALTER TABLE " + t + " MODIFY " + c + " GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE)");
            }
        });
        Object seqs = manifest.get("sequences");
        if (seqs instanceof Map<?, ?> m) {
            for (String seq : SEQUENCES) {
                Object v = m.get(seq);
                if (v != null) {
                    long start = Math.max(1, Long.parseLong(v.toString()));
                    jdbc.execute("DROP SEQUENCE " + seq);
                    jdbc.execute("CREATE SEQUENCE " + seq + " START WITH " + start + " INCREMENT BY 1 NOCACHE");
                }
            }
        }

        // Files: replace invoices and logo with the backup's.
        int files = 0;
        try {
            for (String dir : List.of("invoices", "logo")) {
                Path live = storage.root().resolve(dir);
                if (Files.isDirectory(live)) {
                    try (Stream<Path> walk = Files.walk(live)) {
                        for (Path f : walk.sorted(Comparator.reverseOrder()).toList()) {
                            if (!f.equals(live)) {
                                Files.delete(f);
                            }
                        }
                    }
                }
                Path from = staged.resolve(dir);
                if (Files.isDirectory(from)) {
                    try (Stream<Path> walk = Files.walk(from)) {
                        for (Path f : walk.filter(Files::isRegularFile).toList()) {
                            Path target = live.resolve(from.relativize(f).toString());
                            Files.createDirectories(target.getParent());
                            Files.copy(f, target);
                            files++;
                        }
                    }
                }
                Files.createDirectories(live);
            }
            for (var type : com.fms.domain.OrderType.values()) {
                Files.createDirectories(storage.invoiceDir(type));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        data.forEach((t, rows) -> counts.put(t, rows.size()));
        audit.logAs(username, "RESTORE_BACKUP", "Backup", String.valueOf(manifest.get("created")),
                counts.getOrDefault("ORDER_LINE", 0) + " order lines, " + files + " files. Safety copy: " + safety.getFileName());
        return new Summary(String.valueOf(manifest.get("created")), current, counts, files);
    }

    private void insertRows(String table, List<Map<String, Object>> rows) {
        Map<String, String> types = columnTypes(table);
        List<String> cols = new ArrayList<>();
        for (String c : rows.get(0).keySet()) {
            if (types.containsKey(c)) {
                cols.add(c);
            }
        }
        String sql = "INSERT INTO " + table + " (" + String.join(", ", cols) + ") VALUES ("
                + String.join(", ", cols.stream().map(c -> "?").toList()) + ")";
        jdbc.batchUpdate(sql, rows, 500, (PreparedStatement ps, Map<String, Object> row) -> {
            for (int i = 0; i < cols.size(); i++) {
                String type = types.get(cols.get(i));
                Object v = row.get(cols.get(i));
                int idx = i + 1;
                if (v == null) {
                    ps.setNull(idx, type.startsWith("NUMBER") ? Types.NUMERIC
                            : (type.equals("DATE") || type.startsWith("TIMESTAMP")) ? Types.TIMESTAMP : Types.VARCHAR);
                } else if (type.startsWith("NUMBER") || type.equals("FLOAT")) {
                    ps.setBigDecimal(idx, new BigDecimal(v.toString()));
                } else if (type.equals("DATE") || type.startsWith("TIMESTAMP")) {
                    ps.setTimestamp(idx, Timestamp.valueOf(LocalDateTime.parse(v.toString())));
                } else {
                    ps.setString(idx, v.toString());
                }
            }
        });
    }

    /** For the page: is there anything to warn about? (size of the data in rows) */
    public long totalRows() {
        long n = 0;
        for (String t : tables()) {
            Long c = jdbc.queryForObject("SELECT COUNT(*) FROM " + t, Long.class);
            n += c == null ? 0 : c;
        }
        return n;
    }

    /** Builds a backup in memory (used by tests/checks; the page streams instead). */
    public byte[] exportBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        export(out);
        return out.toByteArray();
    }
}
