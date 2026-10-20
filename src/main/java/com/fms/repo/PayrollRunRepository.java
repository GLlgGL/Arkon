package com.fms.repo;

import com.fms.domain.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    java.util.List<PayrollRun> findAllByOrderByPeriodDesc();
    java.util.Optional<PayrollRun> findByPeriod(String period);
    boolean existsByPeriod(String period);
}
