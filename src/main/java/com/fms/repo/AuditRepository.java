package com.fms.repo;

import com.fms.domain.AuditEntry;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditRepository extends JpaRepository<AuditEntry, Long> {

    @Query("select a from AuditEntry a where a.loggedAt >= :from and a.loggedAt < :to and ("
            + " lower(a.username) like :q or lower(a.action) like :q or lower(a.entity) like :q"
            + " or lower(coalesce(a.entityKey, '')) like :q or lower(coalesce(a.details, '')) like :q)"
            + " order by a.loggedAt desc, a.id desc")
    Page<AuditEntry> search(@Param("q") String likePattern, @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to, Pageable pageable);
}
