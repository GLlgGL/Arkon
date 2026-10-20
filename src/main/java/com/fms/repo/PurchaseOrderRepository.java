package com.fms.repo;

import com.fms.domain.PurchaseOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

    Optional<PurchaseOrder> findByCode(String code);

    @Query(value = "SELECT PO_CODE_SEQ.NEXTVAL FROM DUAL", nativeQuery = true)
    Number nextCode();
}
