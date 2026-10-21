package com.fms.service;

import com.fms.domain.*;
import com.fms.repo.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stock count (inventarizimi): takes a snapshot of what IMS expects for every
 * product, lets you enter what was actually counted (and print a count
 * sheet), then creates all the stock corrections in one go.
 */
@Service
public class StockCountService {

    private final StockCountRepository counts;
    private final StockCountLineRepository countLines;
    private final ProductRepository products;
    private final PricingService pricing;
    private final StockService stock;
    private final ShelfService shelves;
    private final PeriodLock lock;
    private final AuditService audit;

    public StockCountService(StockCountRepository counts, StockCountLineRepository countLines, ProductRepository products,
                             PricingService pricing, StockService stock, ShelfService shelves, PeriodLock lock, AuditService audit) {
        this.counts = counts; this.countLines = countLines; this.products = products; this.pricing = pricing;
        this.stock = stock; this.shelves = shelves; this.lock = lock; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<StockCount> all() { return counts.findAllByOrderByCreatedAtDesc(); }

    public StockCount get(String code) {
        return counts.findByCode(code).orElseThrow(() -> new NotFoundException("Stock count not found"));
    }

    @Transactional(readOnly = true)
    public List<StockCountLine> lines(Long id) { return countLines.findByCountIdOrderByProductNameAsc(id); }

    @Transactional
    public StockCount create(String note, AppUser user) {
        return create(note, null, user);
    }

    /** @param locationId the shop or warehouse counted (Locations module); null = the main / only one */
    @Transactional
    public StockCount create(String note, Long locationId, AppUser user) {
        lock.check(LocalDate.now());
        StockCount c = new StockCount();
        c.setCode(String.format("SC-%05d", counts.nextCode().longValue()));
        c.setCountedOn(LocalDate.now());
        c.setNote(note == null || note.isBlank() ? null : note.trim());
        c.setCreatedBy(user.getUsername());
        c.setLocationId(locationId);
        c = counts.save(c);
        for (Product p : products.findAllByOrderByNameAsc()) {
            StockCountLine l = new StockCountLine();
            l.setCountId(c.getId());
            l.setProductName(p.getName());
            l.setUnit(p.getUnit());
            l.setExpected(pricing.availableFor(p.getName(), locationId));
            countLines.save(l);
        }
        audit.log("CREATE", "Stock count", c.getCode(), null);
        return c;
    }

    /** Saves the counted quantities (empty = not counted yet). */
    @Transactional
    public void save(String code, Map<Long, Long> counted) {
        StockCount c = get(code);
        if (!c.isOpen()) throw new OrderException("This stock count is closed.");
        for (StockCountLine l : lines(c.getId())) {
            if (counted.containsKey(l.getId())) {
                Long v = counted.get(l.getId());
                l.setCounted(v == null || v < 0 ? null : v);
                countLines.save(l);
            }
        }
    }

    /** Creates a stock correction for every product whose count differs. Returns how many. */
    @Transactional
    public int apply(String code, AppUser user) {
        StockCount c = get(code);
        if (!c.isOpen()) throw new OrderException("This stock count is closed.");
        lock.check(LocalDate.now());
        List<StockCountLine> ls = lines(c.getId());
        // stock may have moved since the snapshot: correct against today's stock
        List<String> problems = new ArrayList<>();
        Map<StockCountLine, Long> diffs = new LinkedHashMap<>();
        for (StockCountLine l : ls) {
            if (l.getCounted() == null) continue;
            long diff = l.getCounted() - pricing.availableFor(l.getProductName(), c.getLocationId());
            if (diff == 0) continue;
            if (diff < 0 && shelves.enabled()) {
                Product p = products.findByName(l.getProductName()).orElse(null);
                if (p != null && shelves.notOnShelf(p) < -diff) problems.add(l.getProductName());
            }
            diffs.put(l, diff);
        }
        if (!problems.isEmpty()) {
            throw new OrderException("These products are on shelves, so remove the missing quantity from the right shelf in Stock adjustments: "
                    + String.join(", ", problems) + ".");
        }
        for (var e : diffs.entrySet()) {
            long d = e.getValue();
            StockLocation.run(c.getLocationId(), () -> stock.add(e.getKey().getProductName(), d > 0 ? StockAdjustment.Reason.COUNT_MORE : StockAdjustment.Reason.COUNT_LESS,
                    (int) Math.abs(d), c.getCode(), null, user));
        }
        c.setStatus("APPLIED");
        c.setAppliedAt(LocalDateTime.now());
        counts.save(c);
        audit.log("APPLY", "Stock count", c.getCode(), diffs.size() + " corrections");
        return diffs.size();
    }

    @Transactional
    public void cancel(String code) {
        StockCount c = get(code);
        if (!c.isOpen()) throw new OrderException("This stock count is closed.");
        c.setStatus("CANCELLED");
        counts.save(c);
        audit.log("CANCEL", "Stock count", c.getCode(), null);
    }
}
