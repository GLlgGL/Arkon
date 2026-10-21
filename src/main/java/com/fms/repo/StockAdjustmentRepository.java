package com.fms.repo;

import com.fms.domain.StockAdjustment;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    @Query("select coalesce(sum(a.quantity), 0) from StockAdjustment a where a.productName = :product")
    Number sumQuantity(@Param("product") String product);

    @Query("select coalesce(sum(a.quantity), 0) from StockAdjustment a where a.productName = :product"
            + " and (a.locationId = :loc or (:main = true and a.locationId is null))")
    Number sumQuantityAt(@Param("product") String product, @Param("loc") Long loc, @Param("main") boolean main);

    /** Warehouse report: net adjustment per product in a period. */
    @Query("select new com.fms.repo.NameCount(a.productName, sum(a.quantity)) from StockAdjustment a"
            + " where a.createdAt >= :from and a.createdAt < :to group by a.productName")
    List<NameCount> quantityByProduct(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select a from StockAdjustment a where lower(a.productName) like :q or lower(coalesce(a.refNo, '')) like :q"
            + " or lower(coalesce(a.note, '')) like :q order by a.createdAt desc")
    Page<StockAdjustment> search(@Param("q") String likePattern, Pageable pageable);
    java.util.List<com.fms.domain.StockAdjustment> findByRefNoOrderByIdAsc(String refNo);
}
