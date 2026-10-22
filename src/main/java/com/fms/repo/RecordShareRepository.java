package com.fms.repo;

import com.fms.domain.OrderType;
import com.fms.domain.RecordShare;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordShareRepository extends JpaRepository<RecordShare, Long> {
    List<RecordShare> findByGranteeOrderByCreatedAtDesc(String grantee);
    List<RecordShare> findByOwnerOrderByCreatedAtDesc(String owner);
    List<RecordShare> findAllByOrderByCreatedAtDesc();
    List<RecordShare> findByOrderTypeAndOrderCode(OrderType type, String code);
    boolean existsByOwnerAndGranteeAndOrderTypeAndOrderCode(String owner, String grantee, OrderType type, String code);
    boolean existsByOwnerAndGranteeAndOrderCodeIsNull(String owner, String grantee);

    @Modifying
    @Query("update RecordShare s set s.owner = :to where s.owner = :from")
    int moveOwner(@Param("from") String from, @Param("to") String to);
}
