package com.fms.repo;

import com.fms.domain.PriceList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceListRepository extends JpaRepository<PriceList, Long> {
    java.util.List<PriceList> findAllByOrderByNameAsc();
    java.util.List<PriceList> findByActiveTrueOrderByNameAsc();
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
