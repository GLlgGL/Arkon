package com.fms.service;

import com.fms.domain.AppConfig;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Automatic backups to a folder of your choice (another disk, a USB drive, a network share
 * or a cloud-synced folder): every N hours or once a day, the full backup (data + invoices +
 * logo) or the data only; one file replaced each time, or dated files kept with retention.
 * The file is the same as Administration → Backup → Export, so it restores the same way.
 */
@Service
public class AutoBackupService {

    private static final Logger log = LoggerFactory.getLogger(AutoBackupService.class);
    public static final String PREFIX = "Arkon-backup_";
    public static final String LATEST = "Arkon-backup-latest.zip";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    public record Result(boolean ok, String message, Path file, int removed) { }

    private final BackupService backup;
    private final ConfigService config;
    private final AuditService audit;

    public AutoBackupService(BackupService backup, ConfigService config, AuditService audit) {
        this.backup = backup;
        this.config = config;
        this.audit = audit;
    }

    /** Checked every minute: is a backup due? */
    @Scheduled(fixedDelay = 60_000, initialDelay = 90_000)
    public void tick() {
        AppConfig c = config.get();
        if (c.isBkAuto() && due(c, LocalDateTime.now())) run("automatic");
    }

    /** When the next backup will run (null = automatic backups off). */
    public LocalDateTime next(AppConfig c) {
        if (!c.isBkAuto()) return null;
        LocalDateTime now = LocalDateTime.now(), last = c.getBkLastRun();
        if (c.getBkEveryHours() > 0) return last == null ? now : last.plusHours(c.getBkEveryHours());
        LocalDateTime slot = now.toLocalDate().atTime(time(c));
        if (last == null || last.isBefore(slot)) return slot.isAfter(now) ? slot : now;     // today's slot, or due now
        return slot.plusDays(1);
    }

    static boolean dueAt(boolean everyHoursMode, int hours, LocalTime at, LocalDateTime last, LocalDateTime now) {
        if (everyHoursMode) return last == null || !now.isBefore(last.plusHours(hours));
        LocalDateTime slot = now.toLocalDate().atTime(at);
        return !now.isBefore(slot) && (last == null || last.isBefore(slot));
    }

    private boolean due(AppConfig c, LocalDateTime now) {
        return dueAt(c.getBkEveryHours() > 0, c.getBkEveryHours(), time(c), c.getBkLastRun(), now);
    }

    private static LocalTime time(AppConfig c) {
        try { return LocalTime.parse(c.getBkAt() == null ? "02:00" : c.getBkAt()); } catch (Exception e) { return LocalTime.of(2, 0); }
    }

    /** Makes a backup now; records the result (shown on the Backup page). */
    public synchronized Result run(String trigger) {
        AppConfig c = config.get();
        Result r;
        try {
            if (c.getBkDir() == null || c.getBkDir().isBlank()) throw new IOException("No backup folder is set.");
            Path dir = Paths.get(c.getBkDir().trim());
            Files.createDirectories(dir);
            if (!Files.isWritable(dir)) throw new IOException("The backup folder can't be written to: " + dir);
            Path target = dir.resolve(c.isBkReplace() ? LATEST : PREFIX + LocalDateTime.now().format(STAMP) + ".zip");
            Path tmp = dir.resolve(target.getFileName() + ".part");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                backup.export(out, c.isBkFull());
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            int removed = c.isBkReplace() ? 0 : retention(dir, target, c.getBkKeepCount(), c.getBkKeepDays());
            long kb = Files.size(target) / 1024;
            r = new Result(true, target.getFileName() + " (" + kb + " KB)" + (removed > 0 ? ", " + removed + " old removed" : ""), target, removed);
        } catch (Exception e) {
            log.error("Automatic backup failed", e);
            r = new Result(false, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), null, 0);
        }
        AppConfig fresh = config.get();
        fresh.setBkLastRun(LocalDateTime.now());
        fresh.setBkLastOk(r.ok());
        fresh.setBkLastMsg(r.message() == null ? null : r.message().length() > 480 ? r.message().substring(0, 480) : r.message());
        config.save(fresh);
        audit.log("BACKUP", "Backup", trigger, (r.ok() ? "OK " : "FAILED ") + r.message());
        return r;
    }

    /**
     * Deletes old dated backups (only Arkon's own "Arkon-backup_….zip" files in that folder):
     * beyond the newest keepCount, and older than keepDays. The file just written is always kept.
     */
    static int retention(Path dir, Path keep, int keepCount, int keepDays) throws IOException {
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = new ArrayList<>(s.filter(p -> {
                String n = p.getFileName().toString();
                return n.startsWith(PREFIX) && n.endsWith(".zip") && Files.isRegularFile(p);
            }).toList());
        }
        files.sort(Comparator.comparing((Path p) -> modified(p)).reversed());
        FileTime limit = keepDays > 0 ? FileTime.from(java.time.Instant.now().minus(Duration.ofDays(keepDays))) : null;
        int removed = 0;
        for (int i = 0; i < files.size(); i++) {
            Path p = files.get(i);
            if (p.equals(keep)) continue;
            boolean tooMany = keepCount > 0 && i >= keepCount;
            boolean tooOld = limit != null && modified(p).compareTo(limit) < 0;
            if (tooMany || tooOld) {
                Files.deleteIfExists(p);
                removed++;
            }
        }
        return removed;
    }

    private static FileTime modified(Path p) {
        try { return Files.getLastModifiedTime(p); } catch (IOException e) { return FileTime.fromMillis(0); }
    }
}
