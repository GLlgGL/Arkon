package com.fms.repo;

import com.fms.domain.ExchangeRate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findFirstByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(String currency, LocalDate date);
    Optional<ExchangeRate> findByRateDateAndCurrency(LocalDate date, String currency);
    List<ExchangeRate> findTop60ByOrderByRateDateDescCurrencyAsc();
}
