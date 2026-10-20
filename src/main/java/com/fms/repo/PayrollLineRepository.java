package com.fms.repo;

import com.fms.domain.PayrollLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollLineRepository extends JpaRepository<PayrollLine, Long> {
    java.util.List<PayrollLine> findByRunIdOrderByEmployeeNameAsc(Long runId);
}
