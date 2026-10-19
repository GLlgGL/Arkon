package com.fms.repo;

import com.fms.domain.OrderInfo;
import com.fms.domain.OrderType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderInfoRepository extends JpaRepository<OrderInfo, Long> {

    Optional<OrderInfo> findByOrderTypeAndOrderCode(OrderType type, String code);

    List<OrderInfo> findByOrderType(OrderType type);

    List<OrderInfo> findByOrderTypeAndOrderCodeIn(OrderType type, Collection<String> codes);

    @Modifying
    @Query("delete from OrderInfo i where i.orderType = :type and i.orderCode = :code")
    int deleteOrder(@Param("type") OrderType type, @Param("code") String code);
}
