package com.fms.repo;

import com.fms.domain.FxReval;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FxRevalRepository extends JpaRepository<FxReval, Long> {
    List<FxReval> findByRevalDateOrderByIdAsc(LocalDate date);
    List<FxReval> findAllByOrderByRevalDateDescIdAsc();
    boolean existsByRevalDate(LocalDate date);
    /** Earlier adjustments of an item that were not reversed (they're still in its book value). */
    List<FxReval> findByItemKeyAndRevalDateBeforeAndReversalIdIsNull(String itemKey, LocalDate date);
}
