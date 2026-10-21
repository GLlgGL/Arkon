package com.fms.repo;

import com.fms.domain.MoneyMovement;
import com.fms.domain.OrderType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MoneyMovementRepository extends JpaRepository<MoneyMovement, Long> {

    /** Net movement of an account (in minus out), optionally only before a date. */
    @Query("select coalesce(sum(case when m.direction = 'IN' then m.amount else -m.amount end), 0)"
            + " from MoneyMovement m where m.account.id = :account and m.movedOn < :before")
    BigDecimal netBefore(@Param("account") Long account, @Param("before") LocalDate before);

    /** Like netBefore, in the account's own currency. */
    @Query("select coalesce(sum(case when m.direction = 'IN' then coalesce(m.fxAmount, m.amount) else -coalesce(m.fxAmount, m.amount) end), 0)"
            + " from MoneyMovement m where m.account.id = :account and m.movedOn < :before")
    BigDecimal netAccountBefore(@Param("account") Long account, @Param("before") LocalDate before);

    @Query("select m from MoneyMovement m where (:account is null or m.account.id = :account)"
            + " and m.movedOn >= :from and m.movedOn <= :to order by m.movedOn, m.id")
    List<MoneyMovement> between(@Param("account") Long account, @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<MoneyMovement> findByPaymentId(Long paymentId);

    List<MoneyMovement> findByTransferId(Long transferId);

    boolean existsByAccountId(Long accountId);

    List<MoneyMovement> findTop12ByOrderByMovedOnDescIdDesc();

    @Modifying
    @Query("delete from MoneyMovement m where m.paymentId is not null and m.orderType = :type and m.orderCode = :code")
    int deleteOrder(@Param("type") OrderType type, @Param("code") String code);
}
