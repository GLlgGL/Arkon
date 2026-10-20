package com.fms.repo;

import com.fms.domain.TaxBracket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxBracketRepository extends JpaRepository<TaxBracket, Long> {
    java.util.List<TaxBracket> findAllByOrderByFromAmountAsc();
}
