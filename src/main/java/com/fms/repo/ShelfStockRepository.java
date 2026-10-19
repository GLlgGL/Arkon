package com.fms.repo;

import com.fms.domain.ShelfStock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShelfStockRepository extends JpaRepository<ShelfStock, Long> {

    Optional<ShelfStock> findByShelfIdAndProductId(Long shelfId, Long productId);

    @Query("select s from ShelfStock s where s.product.id = :productId and s.quantity > 0 order by s.shelf.code")
    List<ShelfStock> placements(@Param("productId") Long productId);

    @Query("select s from ShelfStock s where s.shelf.id = :shelfId and s.quantity > 0 order by s.product.name")
    List<ShelfStock> contents(@Param("shelfId") Long shelfId);

    @Query("select coalesce(sum(s.quantity), 0) from ShelfStock s where s.product.id = :productId")
    Number totalOnShelves(@Param("productId") Long productId);

    @Query("select s from ShelfStock s where s.quantity > 0 order by s.product.name, s.shelf.code")
    List<ShelfStock> allPlacements();
}
