package com.fms.repo;

import com.fms.domain.AccEntry;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccEntryRepository extends JpaRepository<AccEntry, Long> {

    @Query("select e from AccEntry e where e.entryDate >= :from and e.entryDate <= :to order by e.entryDate, e.id")
    List<AccEntry> between(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select e from AccEntry e where e.source not in ('MANUAL', 'CLOSING') and e.entryDate >= :from and e.entryDate <= :to")
    List<AccEntry> automaticBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT ACC_ENTRY_SEQ.NEXTVAL FROM DUAL", nativeQuery = true)
    Number nextNo();

    /** Per account: [accountId, sum debit, sum credit] for entries in a date range. */
    @Query("select l.account.id, coalesce(sum(l.debit), 0), coalesce(sum(l.credit), 0) from AccLine l"
            + " where l.entry.entryDate >= :from and l.entry.entryDate <= :to group by l.account.id")
    List<Object[]> totalsByAccount(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Like totalsByAccount, without year-end closing entries (for the income statement). */
    @Query("select l.account.id, coalesce(sum(l.debit), 0), coalesce(sum(l.credit), 0) from AccLine l"
            + " where l.entry.source <> 'CLOSING' and l.entry.entryDate >= :from and l.entry.entryDate <= :to group by l.account.id")
    List<Object[]> totalsByAccountNoClosing(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select e from AccEntry e where e.source = 'CLOSING' and e.sourceRef = :year")
    List<AccEntry> closingOf(@Param("year") String year);

    @Query("select l from com.fms.domain.AccLine l where l.account.id = :account and l.entry.entryDate >= :from"
            + " and l.entry.entryDate <= :to order by l.entry.entryDate, l.entry.id, l.id")
    List<com.fms.domain.AccLine> ledger(@Param("account") Long account, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
