package com.fms.service;

import com.fms.domain.*;
import com.fms.repo.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Batch / expiry and serial number tracking (Tracking module), per product.
 *
 * Batch stock is counted like all stock: purchases − sales ± adjustments,
 * per batch (and per location when Locations is on). Sales take the batch
 * that expires first (first-expired, first-out) unless one is chosen;
 * expired batches are never picked automatically.
 *
 * Serial numbers are one row per unit: received on purchase, sold on sale,
 * released again when a line is deleted, moved with transfers.
 */
@Service("tracking")
public class TrackingService {

    public record Batch(String batchNo, LocalDate expiry, long quantity) {
        public boolean isExpired() { return expiry != null && expiry.isBefore(LocalDate.now()); }
    }
    public record ExpiryRow(String product, String unit, Batch batch, Long locationId, long daysLeft) { }

    private final ProductRepository products;
    private final OrderLineRepository lines;
    private final StockAdjustmentRepository adjustments;
    private final SerialNumberRepository serials;
    private final LocationRepository locations;
    private final ConfigService config;

    public TrackingService(ProductRepository products, OrderLineRepository lines, StockAdjustmentRepository adjustments,
                           SerialNumberRepository serials, LocationRepository locations, ConfigService config) {
        this.products = products; this.lines = lines; this.adjustments = adjustments; this.serials = serials;
        this.locations = locations; this.config = config;
    }

    public boolean enabled() { return config.get().isModTracking(); }

    private static final ThreadLocal<Object[]> HINT = new ThreadLocal<>();

    /** Runs work with a given batch (and expiry) for the stock adjustments it makes. */
    public static <T> T withBatch(String batchNo, LocalDate expiry, java.util.function.Supplier<T> work) {
        Object[] before = HINT.get();
        HINT.set(new Object[] {batchNo, expiry});
        try { return work.get(); } finally { if (before == null) HINT.remove(); else HINT.set(before); }
    }

    static String hintBatch() { Object[] h = HINT.get(); return h == null ? null : (String) h[0]; }
    static LocalDate hintExpiry() { Object[] h = HINT.get(); return h == null ? null : (LocalDate) h[1]; }

    /** The product's tracking (NONE when the module is off). */
    public String trackingOf(String productName) {
        if (!enabled()) return "NONE";
        return products.findByName(productName).map(Product::getTracking).orElse("NONE");
    }

    // ------------------------------------------------------------ locations

    private boolean locationsOn() { return config.get().isModLocations(); }

    private Long main() { return locations.findFirstByMainTrue().map(Location::getId).orElse(null); }

    /** Same location? (null = the main one; always true without Locations) */
    private boolean sameLocation(Long a, Long b) {
        if (!locationsOn()) return true;
        Long m = main();
        return Objects.equals(a == null ? m : a, b == null ? m : b);
    }

    // ---------------------------------------------------------------- batches

    /** Batches of a product with stock at a location, the first to expire first. */
    @Transactional(readOnly = true)
    public List<Batch> batches(String productName, Long loc) {
        Map<String, long[]> qty = new LinkedHashMap<>();
        Map<String, LocalDate> exp = new HashMap<>();
        for (OrderLine l : lines.batchLines(productName)) {
            if (l.getOrderType() == OrderType.OFFER || !sameLocation(l.getLocationId(), loc)) continue;
            long sign = l.getOrderType() == OrderType.PURCHASE ? 1 : -1;
            qty.computeIfAbsent(l.getBatchNo(), k -> new long[1])[0] += sign * l.getQuantity();
            if (l.getExpiryDate() != null) exp.putIfAbsent(l.getBatchNo(), l.getExpiryDate());
        }
        for (StockAdjustment a : adjustments.batchAdjustments(productName)) {
            if (!sameLocation(a.getLocationId(), loc)) continue;
            qty.computeIfAbsent(a.getBatchNo(), k -> new long[1])[0] += a.getQuantity();
            if (a.getExpiryDate() != null) exp.putIfAbsent(a.getBatchNo(), a.getExpiryDate());
        }
        List<Batch> out = new ArrayList<>();
        qty.forEach((b, q) -> { if (q[0] > 0) out.add(new Batch(b, exp.get(b), q[0])); });
        out.sort(Comparator.comparing(Batch::expiry, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Batch::batchNo));
        return out;
    }

    /** The batch a sale or removal takes from: the chosen one, or the first to expire that has enough. */
    public Batch pickBatch(String productName, int qty, Long loc, String chosen) {
        List<Batch> list = batches(productName, loc);
        if (chosen != null && !chosen.isBlank()) {
            Batch b = list.stream().filter(x -> x.batchNo().equals(chosen.trim())).findFirst()
                    .orElseThrow(() -> new OrderException("Batch " + chosen + " of " + productName + " has no stock here."));
            if (b.isExpired()) throw new OrderException("Batch " + b.batchNo() + " of " + productName + " has expired.");
            if (b.quantity() < qty) throw new OrderException("Batch " + b.batchNo() + " has only " + b.quantity() + " of " + productName + ".");
            return b;
        }
        return list.stream().filter(b -> !b.isExpired() && b.quantity() >= qty).findFirst()
                .orElseThrow(() -> new OrderException("No single batch of " + productName + " has " + qty
                        + " that haven't expired. Split the quantity into lines per batch."));
    }

    /** Batches expiring within some days (or already expired), at a location or everywhere. */
    @Transactional(readOnly = true)
    public List<ExpiryRow> expiring(int days, Long loc) {
        List<ExpiryRow> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        List<Long> locs = new ArrayList<>();
        if (locationsOn() && loc == null) locations.findByActiveTrueOrderByMainDescNameAsc().forEach(l -> locs.add(l.getId()));
        else locs.add(loc);
        for (Product p : products.findAllByOrderByNameAsc()) {
            if (!p.isBatchTracked()) continue;
            for (Long l : locs) {
                for (Batch b : batches(p.getName(), l)) {
                    if (b.expiry() == null || b.expiry().isAfter(today.plusDays(days))) continue;
                    out.add(new ExpiryRow(p.getName(), p.getUnit(), b, l, java.time.temporal.ChronoUnit.DAYS.between(today, b.expiry())));
                }
            }
        }
        out.sort(Comparator.comparing(r -> r.batch().expiry()));
        return out;
    }

    // ---------------------------------------------------------- serial numbers

    /** Serial numbers typed or scanned: one per line, or separated by commas / spaces. */
    public static List<String> parse(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String s : text.split("[\\r\\n,;\\t ]+")) {
            String t = s.trim();
            if (!t.isEmpty() && !out.contains(t)) out.add(t);
        }
        return out;
    }

    /** Units received on a purchase line. */
    @Transactional
    public void receive(Product p, OrderLine line, String typed) {
        List<String> list = parse(typed);
        if (list.size() != line.getQuantity()) {
            throw new OrderException("Enter " + line.getQuantity() + " serial numbers for " + p.getName() + " (one per unit); you entered " + list.size() + ".");
        }
        for (String s : list) {
            if (serials.findByProductNameAndSerial(p.getName(), s).isPresent()) {
                throw new OrderException("Serial number " + s + " of " + p.getName() + " already exists.");
            }
            SerialNumber n = new SerialNumber();
            n.setProductName(p.getName());
            n.setSerial(s);
            n.setLocationId(line.getLocationId());
            n.setPurchaseLineId(line.getId());
            n.setPurchaseCode(line.getOrderCode());
            n.setPurchasedOn(line.getCreatedAt().toLocalDate());
            serials.save(n);
        }
    }

    /** Units sold on a sale line: the ones scanned, or the oldest in stock. */
    @Transactional
    public void sell(Product p, OrderLine line, String typed, Long loc) {
        List<String> list = parse(typed);
        List<SerialNumber> inStock = serials.findByProductNameAndStatusOrderByIdAsc(p.getName(), "IN_STOCK").stream()
                .filter(s -> sameLocation(s.getLocationId(), loc)).toList();
        List<SerialNumber> take = new ArrayList<>();
        if (list.isEmpty()) {
            if (inStock.size() < line.getQuantity()) {
                throw new OrderException("Only " + inStock.size() + " serial numbers of " + p.getName() + " are in stock here.");
            }
            take.addAll(inStock.subList(0, line.getQuantity()));
        } else {
            if (list.size() != line.getQuantity()) {
                throw new OrderException("Enter " + line.getQuantity() + " serial numbers for " + p.getName() + " (one per unit); you entered " + list.size() + ".");
            }
            for (String s : list) {
                take.add(inStock.stream().filter(x -> x.getSerial().equals(s)).findFirst()
                        .orElseThrow(() -> new OrderException("Serial number " + s + " of " + p.getName() + " isn't in stock here.")));
            }
        }
        for (SerialNumber s : take) {
            s.setStatus("SOLD");
            s.setSaleLineId(line.getId());
            s.setSaleCode(line.getOrderCode());
            s.setSoldOn(line.getCreatedAt().toLocalDate());
            serials.save(s);
        }
    }

    /** A line is deleted: its sold units go back to stock; its purchased units disappear (if none was sold). */
    @Transactional
    public void lineDeleted(OrderLine l) {
        if (!enabled() || l.isReturn()) return;
        if (l.getOrderType() == OrderType.SALE) {
            for (SerialNumber s : serials.findBySaleLineIdOrderByIdDesc(l.getId())) {
                s.setStatus("IN_STOCK");
                s.setSaleLineId(null);
                s.setSaleCode(null);
                s.setSoldOn(null);
                serials.save(s);
            }
        } else if (l.getOrderType() == OrderType.PURCHASE) {
            List<SerialNumber> mine = serials.findByPurchaseLineIdOrderByIdAsc(l.getId());
            if (mine.stream().anyMatch(s -> !"IN_STOCK".equals(s.getStatus()))) {
                throw new OrderException("Some serial numbers of " + l.getProductName() + " from this purchase were already sold, so the line can't be removed.");
            }
            serials.deleteAll(mine);
        }
    }

    /** Units returned: a client brings units back (they're in stock again) or units go back to the supplier. */
    @Transactional
    public void returned(OrderLine original, int qty) {
        if (!enabled()) return;
        if (original.getOrderType() == OrderType.SALE) {
            List<SerialNumber> sold = serials.findBySaleLineIdOrderByIdDesc(original.getId());
            for (int i = 0; i < Math.min(qty, sold.size()); i++) {
                SerialNumber s = sold.get(i);
                s.setStatus("IN_STOCK");
                serials.save(s);
            }
        } else if (original.getOrderType() == OrderType.PURCHASE) {
            List<SerialNumber> mine = serials.findByPurchaseLineIdOrderByIdAsc(original.getId()).stream()
                    .filter(s -> "IN_STOCK".equals(s.getStatus())).toList();
            for (int i = 0; i < Math.min(qty, mine.size()); i++) {
                mine.get(i).setStatus("RETURNED");
                serials.save(mine.get(i));
            }
        }
    }

    /** Units removed by a stock adjustment (damaged, lost…): the oldest in stock at that location. */
    @Transactional
    public void removed(String productName, int qty, Long loc) {
        List<SerialNumber> inStock = serials.findByProductNameAndStatusOrderByIdAsc(productName, "IN_STOCK").stream()
                .filter(s -> sameLocation(s.getLocationId(), loc)).toList();
        for (int i = 0; i < Math.min(qty, inStock.size()); i++) {
            inStock.get(i).setStatus("REMOVED");
            serials.save(inStock.get(i));
        }
    }

    /** Units moved between locations by a transfer. */
    @Transactional
    public void moved(String productName, int qty, Long from, Long to) {
        List<SerialNumber> inStock = serials.findByProductNameAndStatusOrderByIdAsc(productName, "IN_STOCK").stream()
                .filter(s -> sameLocation(s.getLocationId(), from)).toList();
        for (int i = 0; i < Math.min(qty, inStock.size()); i++) {
            inStock.get(i).setLocationId(to);
            serials.save(inStock.get(i));
        }
    }

    @Transactional(readOnly = true)
    public List<SerialNumber> search(String q) {
        return q == null || q.isBlank() ? List.of() : serials.search(q.trim());
    }

    /** Serials of a line, for the order page. */
    @Transactional(readOnly = true)
    public String serialsOf(OrderLine l) {
        if (!enabled()) return "";
        List<SerialNumber> list = l.getOrderType() == OrderType.PURCHASE ? serials.findByPurchaseLineIdOrderByIdAsc(l.getId())
                : serials.findBySaleLineIdOrderByIdDesc(l.getId());
        return String.join(", ", list.stream().map(SerialNumber::getSerial).toList());
    }
}
