package com.fms.service;

import com.fms.domain.*;
import com.fms.repo.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shops and warehouses (Locations module): the register, stock per location,
 * and transfers between locations. A transfer is two stock adjustments
 * (out at one location, in at the other) sharing the transfer's code, so the
 * company's total stock and accounting don't change.
 */
@Service("locations")
public class LocationService {

    /** One product's stock at each location (same order as locations()). */
    public record StockRow(Product product, List<Long> perLocation, long total) { }
    public record TransferView(StockTransfer transfer, String from, String to, List<StockAdjustment> lines) { }
    public record TransferLine(String product, int quantity) { }

    private final LocationRepository repo;
    private final StockTransferRepository transfers;
    private final StockAdjustmentRepository adjustments;
    private final OrderLineRepository lines;
    private final ProductRepository products;
    private final PricingService pricing;
    private final StockService stock;
    private final ConfigService config;
    private final PeriodLock lock;
    private final AuditService audit;

    public LocationService(LocationRepository repo, StockTransferRepository transfers, StockAdjustmentRepository adjustments,
                           OrderLineRepository lines, ProductRepository products, PricingService pricing, StockService stock,
                           ConfigService config, PeriodLock lock, AuditService audit) {
        this.repo = repo; this.transfers = transfers; this.adjustments = adjustments; this.lines = lines; this.products = products;
        this.pricing = pricing; this.stock = stock; this.config = config; this.lock = lock; this.audit = audit;
    }

    public boolean enabled() { return config.get().isModLocations(); }

    @Transactional(readOnly = true)
    public List<Location> all() { return repo.findAllByOrderByMainDescNameAsc(); }

    @Transactional(readOnly = true)
    public List<Location> active() { return repo.findByActiveTrueOrderByMainDescNameAsc(); }

    public Location get(Long id) { return repo.findById(id).orElseThrow(() -> new NotFoundException("Location not found")); }

    /** Name of a location id (null = the main location), for pages. */
    @Transactional(readOnly = true)
    public String name(Long id) {
        if (id == null) return repo.findFirstByMainTrue().map(Location::getName).orElse("");
        return repo.findById(id).map(Location::getName).orElse("");
    }

    /** The location a user works at: their own, or the main one. */
    public Long of(AppUser u) {
        if (u != null && u.getLocationId() != null) return u.getLocationId();
        return repo.findFirstByMainTrue().map(Location::getId).orElse(null);
    }

    @Transactional
    public Location save(Long id, String code, String name, String kind, String address, boolean active) {
        if (code == null || code.isBlank() || name == null || name.isBlank()) throw new OrderException("Enter a code and a name.");
        String c = code.trim().toUpperCase();
        if (id == null ? repo.existsByCode(c) : repo.existsByCodeAndIdNot(c, id)) {
            throw new OrderException("A location with this code already exists.");
        }
        Location l = id == null ? new Location() : get(id);
        l.setCode(c);
        l.setName(name);
        l.setKind(kind);
        l.setAddress(address);
        l.setActive(active || l.isMain());       // the main location stays active
        Location saved = repo.save(l);
        audit.log(id == null ? "CREATE" : "UPDATE", "Location", saved.getLabel(), saved.getKind());
        return saved;
    }

    // ------------------------------------------------------------------- stock

    @Transactional(readOnly = true)
    public List<StockRow> stockTable(String search) {
        List<Location> locs = active();
        String q = search == null ? "" : search.trim().toLowerCase();
        List<StockRow> out = new ArrayList<>();
        for (Product p : products.findAllByOrderByNameAsc()) {
            if (!q.isEmpty() && !p.getName().toLowerCase().contains(q) && (p.getCode() == null || !p.getCode().toLowerCase().contains(q))) continue;
            List<Long> per = new ArrayList<>();
            long total = 0;
            for (Location l : locs) {
                long a = pricing.availableAt(p.getName(), l.getId());
                per.add(a);
                total += a;
            }
            out.add(new StockRow(p, per, total));
        }
        return out;
    }

    // --------------------------------------------------------------- transfers

    @Transactional(readOnly = true)
    public List<TransferView> transfers() {
        List<TransferView> out = new ArrayList<>();
        for (StockTransfer t : transfers.findTop100ByOrderByCreatedAtDesc()) {
            out.add(new TransferView(t, name(t.getFromLocation()), name(t.getToLocation()),
                    adjustments.findByRefNoOrderByIdAsc(t.getCode()).stream()
                            .filter(a -> a.getReason() == StockAdjustment.Reason.TRANSFER_IN).toList()));
        }
        return out;
    }

    @Transactional
    public StockTransfer transfer(Long from, Long to, LocalDate date, String note, List<TransferLine> items, AppUser user) {
        if (from == null || to == null || from.equals(to)) throw new OrderException("Choose two different locations.");
        get(from);
        get(to);
        LocalDate d = date == null ? LocalDate.now() : date;
        lock.check(d);
        Map<String, Integer> want = new LinkedHashMap<>();
        for (TransferLine i : items) {
            if (i.product() != null && !i.product().isBlank() && i.quantity() > 0) want.merge(i.product().trim(), i.quantity(), Integer::sum);
        }
        if (want.isEmpty()) throw new OrderException("Add at least one product and quantity.");
        List<String> short_ = new ArrayList<>();
        for (var e : want.entrySet()) {
            if (products.findByName(e.getKey()).isEmpty()) throw new OrderException("Unknown product: " + e.getKey());
            long have = pricing.availableAt(e.getKey(), from);
            if (e.getValue() > have) short_.add(e.getKey() + " (" + Math.max(0, have) + ")");
        }
        if (!short_.isEmpty()) throw new OrderException("Not enough stock at the sending location: " + String.join(", ", short_) + ".");
        StockTransfer t = new StockTransfer();
        t.setCode(String.format("TR-%05d", transfers.nextCode().longValue()));
        t.setFromLocation(from);
        t.setToLocation(to);
        t.setMovedOn(d);
        t.setNote(note);
        t.setCreatedBy(user.getUsername());
        t = transfers.save(t);
        String code = t.getCode();
        for (var e : want.entrySet()) {
            StockLocation.run(from, () -> stock.add(e.getKey(), StockAdjustment.Reason.TRANSFER_OUT, e.getValue(), code, note, user));
            StockLocation.run(to, () -> stock.add(e.getKey(), StockAdjustment.Reason.TRANSFER_IN, e.getValue(), code, note, user));
        }
        audit.log("TRANSFER", "Stock", code, name(from) + " -> " + name(to) + ": " + want);
        return t;
    }

    /** Undoes a transfer while the goods are still at the receiving location. */
    @Transactional
    public void undo(String code) {
        StockTransfer t = transfers.findByCode(code).orElseThrow(() -> new NotFoundException("Transfer not found"));
        lock.check(t.getMovedOn());
        List<StockAdjustment> moves = adjustments.findByRefNoOrderByIdAsc(code);
        for (StockAdjustment a : moves) {
            if (a.getReason() == StockAdjustment.Reason.TRANSFER_IN && pricing.availableAt(a.getProductName(), t.getToLocation()) < a.getQuantity()) {
                throw new OrderException("Some of the goods have already been sold or moved at " + name(t.getToLocation()) + ", so this transfer can't be undone.");
            }
        }
        adjustments.deleteAll(moves.stream().filter(a -> !a.getReason().isManual()).toList());
        transfers.delete(t);
        audit.log("UNDO_TRANSFER", "Stock", code, null);
    }
}
