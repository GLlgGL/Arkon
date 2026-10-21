package com.fms.repo;

import com.fms.domain.BudgetLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetLineRepository extends JpaRepository<BudgetLine, Long> {
    java.util.List<BudgetLine> findByBudgetYear(Integer year);
}
