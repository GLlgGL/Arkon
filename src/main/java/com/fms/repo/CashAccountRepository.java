package com.fms.repo;

import com.fms.domain.CashAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashAccountRepository extends JpaRepository<CashAccount, Long> {
    List<CashAccount> findAllByOrderByKindAscNameAsc();
    List<CashAccount> findByActiveTrueOrderByKindAscNameAsc();
    Optional<CashAccount> findFirstByKindAndDefaultAccountTrueAndActiveTrue(CashAccount.Kind kind);
    Optional<CashAccount> findFirstByKindAndActiveTrueOrderByIdAsc(CashAccount.Kind kind);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
