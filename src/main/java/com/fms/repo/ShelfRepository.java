package com.fms.repo;

import com.fms.domain.Shelf;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShelfRepository extends JpaRepository<Shelf, Long> {
    List<Shelf> findAllByOrderByCodeAsc();
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}
