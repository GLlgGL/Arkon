package com.fms.repo;

import com.fms.domain.StockCountLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockCountLineRepository extends JpaRepository<StockCountLine, Long> {
    java.util.List<StockCountLine> findByCountIdOrderByProductNameAsc(Long countId);
}
