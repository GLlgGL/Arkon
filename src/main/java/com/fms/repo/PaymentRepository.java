package com.fms.repo;

import com.fms.domain.OrderType;
import com.fms.domain.Payment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderTypeAndOrderCodeOrderByPaidOnAscIdAsc(OrderType type, String code);

    /** Paid amount per order: [orderCode, sum(amount)]. */
    @Query("select p.orderCode, p.kind, sum(p.amount) from Payment p where p.orderType = :type group by p.orderCode, p.kind")
    List<Object[]> paidPerOrder(@Param("type") OrderType type);

    @Query("select p.orderCode, p.kind, sum(p.amount) from Payment p where p.orderType = :type and p.orderCode in :codes group by p.orderCode, p.kind")
    List<Object[]> paidPerOrder(@Param("type") OrderType type, @Param("codes") Collection<String> codes);

    @Modifying
    @Query("delete from Payment p where p.orderType = :type and p.orderCode = :code")
    int deleteOrder(@Param("type") OrderType type, @Param("code") String code);
    boolean existsByOrderTypeAndOrderCode(OrderType type, String code);
}
