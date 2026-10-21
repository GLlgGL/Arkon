package com.fms.repo;

import com.fms.domain.SerialNumber;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SerialNumberRepository extends JpaRepository<SerialNumber, Long> {
    Optional<SerialNumber> findByProductNameAndSerial(String productName, String serial);
    List<SerialNumber> findByProductNameAndStatusOrderByIdAsc(String productName, String status);
    List<SerialNumber> findBySaleLineIdOrderByIdDesc(Long saleLineId);
    List<SerialNumber> findByPurchaseLineIdOrderByIdAsc(Long purchaseLineId);
    @Query("select s from SerialNumber s where lower(s.serial) like lower(concat('%', :q, '%')) order by s.serial")
    List<SerialNumber> search(@Param("q") String q);
}
