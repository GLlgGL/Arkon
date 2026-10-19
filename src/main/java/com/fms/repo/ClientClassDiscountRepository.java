package com.fms.repo;

import com.fms.domain.ClientClassDiscount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClientClassDiscountRepository extends JpaRepository<ClientClassDiscount, Long> {

    Optional<ClientClassDiscount> findByClientIdAndProductClassId(Long clientId, Long productClassId);

    @Query("select d from ClientClassDiscount d order by d.client.name, d.productClass.name")
    List<ClientClassDiscount> findAllSorted();
}
