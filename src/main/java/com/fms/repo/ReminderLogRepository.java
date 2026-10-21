package com.fms.repo;

import com.fms.domain.ReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {
    java.util.List<ReminderLog> findByOrderCodeOrderBySentAtDesc(String orderCode);
    java.util.List<ReminderLog> findTop50ByOrderBySentAtDesc();
}
