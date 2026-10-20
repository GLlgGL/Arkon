package com.fms.repo;

import com.fms.domain.ProductionLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionLineRepository extends JpaRepository<ProductionLine, Long> {
    java.util.List<ProductionLine> findByOrderIdOrderByIdAsc(Long orderId);
}
