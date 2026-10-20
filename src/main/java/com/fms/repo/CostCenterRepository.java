package com.fms.repo;

import com.fms.domain.CostCenter;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {
    List<CostCenter> findAllByOrderByCodeAsc();
    List<CostCenter> findByActiveTrueOrderByCodeAsc();
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}
