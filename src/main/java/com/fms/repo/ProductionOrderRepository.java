package com.fms.repo;

import com.fms.domain.ProductionOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {
    java.util.List<ProductionOrder> findAllByOrderByCreatedAtDesc();
    java.util.Optional<ProductionOrder> findByCode(String code);
    @org.springframework.data.jpa.repository.Query(value = "SELECT PRODUCTION_SEQ.NEXTVAL FROM DUAL", nativeQuery = true)
    Number nextCode();
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(o.quantity), 0), coalesce(sum(o.totalCost), 0) from ProductionOrder o where o.status = 'DONE' and o.productName = :product")
    java.util.List<Object[]> producedTotals(@org.springframework.data.repository.query.Param("product") String product);
}
