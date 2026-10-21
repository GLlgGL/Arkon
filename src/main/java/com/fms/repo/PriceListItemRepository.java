package com.fms.repo;

import com.fms.domain.PriceListItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceListItemRepository extends JpaRepository<PriceListItem, Long> {
    java.util.List<PriceListItem> findByListId(Long listId);
    java.util.Optional<PriceListItem> findByListIdAndProductId(Long listId, Long productId);
}
