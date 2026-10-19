package com.fms.repo;

import com.fms.domain.ProductClass;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductClassRepository extends JpaRepository<ProductClass, Long> {
    List<ProductClass> findAllByOrderByNameAsc();
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
