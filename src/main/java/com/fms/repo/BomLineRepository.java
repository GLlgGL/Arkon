package com.fms.repo;

import com.fms.domain.BomLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BomLineRepository extends JpaRepository<BomLine, Long> {
    java.util.List<BomLine> findByProductIdOrderByIdAsc(Long productId);
    @org.springframework.data.jpa.repository.Query("select distinct b.productId from BomLine b")
    java.util.List<Long> productsWithBom();
}
