package com.fms.repo;

import com.fms.domain.Product;
import com.fms.domain.ProductClass;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByName(String name);
    List<Product> findAllByOrderByNameAsc();
    List<Product> findByProductClassOrderByNameAsc(ProductClass productClass);
    boolean existsByName(String name);
    Optional<Product> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
    boolean existsByNameAndIdNot(String name, Long id);

    @Query("select distinct p.unit from Product p order by p.unit")
    List<String> findDistinctUnits();
}
