package com.fms.repo;

import com.fms.domain.StockTransfer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    List<StockTransfer> findTop100ByOrderByCreatedAtDesc();
    Optional<StockTransfer> findByCode(String code);
    @Query(value = "SELECT TRANSFER_SEQ.NEXTVAL FROM DUAL", nativeQuery = true)
    Number nextCode();
}
