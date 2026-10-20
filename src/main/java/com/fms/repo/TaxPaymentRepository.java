package com.fms.repo;

import com.fms.domain.TaxPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxPaymentRepository extends JpaRepository<TaxPayment, Long> {
    java.util.List<TaxPayment> findAllByOrderByPaidOnDesc();
}
