package com.fms.repo;

import com.fms.domain.AccAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccAccountRepository extends JpaRepository<AccAccount, Long> {
    List<AccAccount> findAllByOrderByCodeAsc();
    Optional<AccAccount> findBySystemKey(String key);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}
