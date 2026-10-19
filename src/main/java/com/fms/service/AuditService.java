package com.fms.service;

import com.fms.domain.AuditEntry;
import com.fms.repo.AuditRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audit log: who created, changed or deleted what, and when. Entries are
 * written in the same transaction as the change, so a failed change leaves
 * no misleading entry behind.
 */
@Service
public class AuditService {

    private final AuditRepository repo;

    public AuditService(AuditRepository repo) {
        this.repo = repo;
    }

    /** Records an action by the logged-in user. */
    @Transactional
    public void log(String action, String entity, String key, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logAs(auth == null ? "system" : auth.getName(), action, entity, key, details);
    }

    @Transactional
    public void logAs(String username, String action, String entity, String key, String details) {
        AuditEntry e = new AuditEntry();
        e.setUsername(username == null ? "system" : username);
        e.setAction(action);
        e.setEntity(entity);
        e.setEntityKey(cut(key, 200));
        e.setDetails(cut(details, 1000));
        repo.save(e);
    }

    /** Deletes every entry, then records who cleared the log, so there is always a trace. */
    @Transactional
    public long clearAll() {
        long count = repo.count();
        repo.deleteAllInBatch();
        log("CLEAR_AUDIT_LOG", "Audit log", null, count + " entries deleted");
        return count;
    }

    @Transactional(readOnly = true)
    public Page<AuditEntry> search(String text, LocalDate from, LocalDate to, Pageable pageable) {
        String like = "%" + (text == null ? "" : text.trim().toLowerCase()) + "%";
        LocalDateTime start = from == null ? LocalDateTime.of(1900, 1, 1, 0, 0) : from.atStartOfDay();
        LocalDateTime end = to == null ? LocalDateTime.of(9999, 1, 1, 0, 0) : to.plusDays(1).atStartOfDay();
        return repo.search(like, start, end, pageable);
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
