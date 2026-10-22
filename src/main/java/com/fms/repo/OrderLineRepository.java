package com.fms.repo;

import com.fms.domain.OrderLine;
import com.fms.domain.OrderType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * All SQL against Purchases / Sales / Offers now lives here, parameterised.
 * The original concatenated user input into SQL strings in many places.
 *
 * Convention: a null {@code createdBy} means "all users" (administrators);
 * regular users always pass their own username, matching the original's
 * "Where Usern = Session['User']" filters.
 */
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

    @Query(value = "SELECT ORDER_CODE_SEQ.NEXTVAL FROM DUAL", nativeQuery = true)
    Number nextOrderCode();

    List<OrderLine> findByOrderTypeAndOrderCodeOrderByCreatedAtAscIdAsc(OrderType type, String orderCode);

    Optional<OrderLine> findByIdAndOrderType(Long id, OrderType type);

    boolean existsByOrderTypeAndOrderCode(OrderType type, String orderCode);

    boolean existsByOrderCode(String orderCode);

    @Modifying
    @Query("delete from OrderLine l where l.orderType = :type and l.orderCode = :code")
    int deleteOrder(@Param("type") OrderType type, @Param("code") String code);

    @Modifying
    @Query("update OrderLine l set l.lastUpdated = :now where l.orderType = :type and l.orderCode = :code")
    int touchOrder(@Param("type") OrderType type, @Param("code") String code, @Param("now") LocalDateTime now);

    /** Total quantity of a product ever bought (PURCHASE) or sold (SALE). */
    @Query("select coalesce(sum(l.quantity), 0) from OrderLine l "
            + "where l.orderType = :type and l.productName = :product")
    Number sumQuantity(@Param("type") OrderType type, @Param("product") String product);

    /** Quantity at one location; lines without a location count for the main location. */
    @Query("select coalesce(sum(l.quantity), 0) from OrderLine l where l.orderType = :type and l.productName = :product"
            + " and (l.locationId = :loc or (:main = true and l.locationId is null))")
    Number sumQuantityAt(@Param("type") OrderType type, @Param("product") String product, @Param("loc") Long loc, @Param("main") boolean main);

    /** Purchases: total value without VAT for a product, for its average cost. */
    @Query("select coalesce(sum(l.valueNoVat), 0) from OrderLine l "
            + "where l.orderType = :type and l.productName = :product")
    Number sumValueNoVat(@Param("type") OrderType type, @Param("product") String product);

    /** Debts: one row per order [orderCode, buyerName, providerName, firstDate, totalWithVat, createdBy]. */
    @Query("select l.orderCode, max(l.buyerName), max(l.providerName), min(l.createdAt), sum(l.valueWithVat), max(l.createdBy)"
            + " from OrderLine l where l.orderType = :type group by l.orderCode")
    List<Object[]> orderTotals(@Param("type") OrderType type);

    /** Return lines of a type, newest first. */
    @Query("select l from OrderLine l where l.orderType = :type and l.returnOf is not null order by l.createdAt desc, l.id")
    List<OrderLine> findReturnLines(@Param("type") OrderType type);

    // ---------------------------------------------------------------- lists

    String SUMMARY_WHERE = " from OrderLine l where l.orderType = :type"
            + " and (:all = true or l.createdBy in :owners or l.orderCode in :codes)"
            + " and (:by is null or l.createdBy = :by)"
            + " and l.createdAt >= :from and l.createdAt < :to"
            + " and l.orderCode in (select x.orderCode from OrderLine x where x.orderType = :type and ("
            + "   lower(x.orderCode) like :q or lower(x.productName) like :q"
            + "   or lower(x.buyerName) like :q or lower(x.providerName) like :q))";

    @Query(value = "select new com.fms.repo.OrderSummary(l.orderCode, min(l.createdAt), max(l.providerName),"
            + " max(l.buyerName), count(l), sum(l.quantity), sum(l.valueNoVat), sum(l.vatValue),"
            + " sum(l.valueWithVat), max(l.createdBy), max(l.lastUpdated))"
            + SUMMARY_WHERE
            + " group by l.orderCode order by min(l.createdAt) desc",
            countQuery = "select count(distinct l.orderCode)" + SUMMARY_WHERE)
    Page<OrderSummary> findSummaries(@Param("type") OrderType type,
                                     @Param("all") boolean all, @Param("owners") java.util.Collection<String> owners,
                                     @Param("codes") java.util.Collection<String> codes, @Param("by") String by,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to,
                                     @Param("q") String likePattern,
                                     Pageable pageable);

    /** Totals of everything the list shows for the same filters (all pages, not just the current one). */
    @Query("select new com.fms.repo.Totals(count(distinct l.orderCode), sum(l.quantity),"
            + " sum(l.valueNoVat), sum(l.vatValue), sum(l.valueWithVat))"
            + SUMMARY_WHERE)
    Totals summaryTotals(@Param("type") OrderType type,
                         @Param("all") boolean all, @Param("owners") java.util.Collection<String> owners,
                                     @Param("codes") java.util.Collection<String> codes, @Param("by") String by,
                         @Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to,
                         @Param("q") String likePattern);

    // -------------------------------------------------------------- reports

    @Query("select l from OrderLine l where l.orderType = :type"
            + " and (:createdBy is null or l.createdBy = :createdBy)"
            + " and l.createdAt >= :from and l.createdAt < :to"
            + " order by l.orderCode, l.createdAt")
    List<OrderLine> findLines(@Param("type") OrderType type,
                              @Param("createdBy") String createdBy,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);

    @Query("select new com.fms.repo.Totals(count(distinct l.orderCode), sum(l.quantity),"
            + " sum(l.valueNoVat), sum(l.vatValue), sum(l.valueWithVat))"
            + " from OrderLine l where l.orderType = :type"
            + " and (:createdBy is null or l.createdBy = :createdBy)"
            + " and l.createdAt >= :from and l.createdAt < :to")
    Totals totals(@Param("type") OrderType type,
                  @Param("createdBy") String createdBy,
                  @Param("from") LocalDateTime from,
                  @Param("to") LocalDateTime to);

    /** Product report: lines whose product name contains the search text. */
    @Query("select l from OrderLine l where l.orderType = :type"
            + " and (:createdBy is null or l.createdBy = :createdBy)"
            + " and lower(l.productName) like :q"
            + " order by l.createdAt desc")
    List<OrderLine> searchLines(@Param("type") OrderType type,
                                @Param("createdBy") String createdBy,
                                @Param("q") String likePattern);

    /** Warehouse report: quantity per product moved in a period. */
    @Query("select new com.fms.repo.NameCount(l.productName, sum(l.quantity))"
            + " from OrderLine l where l.orderType = :type"
            + " and l.createdAt >= :from and l.createdAt < :to"
            + " group by l.productName")
    List<NameCount> quantityByProduct(@Param("type") OrderType type,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    @Query("select min(l.createdAt) from OrderLine l")
    LocalDateTime firstActivity();
    /** Lines of a product that carry a batch (batch-tracked products). */
    @Query("select l from OrderLine l where l.productName = :p and l.batchNo is not null")
    List<OrderLine> batchLines(@Param("p") String product);

    @Query("select distinct l.productName from OrderLine l where l.batchNo is not null")
    List<String> productsWithBatches();
    /** Who created each order of a type: [orderCode, createdBy]. */
    @Query("select l.orderCode, max(l.createdBy) from OrderLine l where l.orderType = :type group by l.orderCode")
    List<Object[]> creators(@Param("type") OrderType type);

    /** Hands a user's records over to another user. */
    @org.springframework.data.jpa.repository.Modifying
    @Query("update OrderLine l set l.createdBy = :to where l.createdBy = :from")
    int moveCreator(@Param("from") String from, @Param("to") String to);
    /** Sale (or other) lines created in a period, returns included (negative). */
    @Query("select l from OrderLine l where l.orderType = :type and l.createdAt >= :from and l.createdAt < :to")
    List<OrderLine> linesBetween(@Param("type") OrderType type, @Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);
}
