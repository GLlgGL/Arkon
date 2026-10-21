package com.fms.repo;

import com.fms.domain.StockCount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockCountRepository extends JpaRepository<StockCount, Long> {
    java.util.List<StockCount> findAllByOrderByCreatedAtDesc();
    java.util.Optional<StockCount> findByCode(String code);
    @org.springframework.data.jpa.repository.Query(value = "SELECT STOCK_COUNT_SEQ.NEXTVAL FROM DUAL", nativeQuery = true)
    Number nextCode();
}
