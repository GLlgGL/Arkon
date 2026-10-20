package com.fms.service;

import com.fms.domain.NotFoundException;
import com.fms.domain.Product;
import com.fms.domain.Shelf;
import com.fms.domain.ShelfStock;
import com.fms.repo.ProductRepository;
import com.fms.repo.ShelfRepository;
import com.fms.repo.ShelfStockRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shelves: where a product's stock physically is (e.g. A1: 2, A2: 3).
 *
 * Total stock still comes from purchases, sales and adjustments. Shelves say
 * where that stock is; whatever isn't on a shelf counts as "not on a shelf":
 *     not on a shelf = total stock - units on all shelves
 * Every change keeps shelves within the total: you can only take from a shelf
 * what's on it, and only take "not on a shelf" stock that isn't on a shelf.
 */
@Service
public class ShelfService {

    /** One choice in the shelf drop-down: a shelf and how much of the product is on it. */
    public record Option(Long shelfId, String code, int quantity) {
    }

    /** Where a product's stock is. */
    public record ProductShelves(String product, String unit, long total, long notOnShelf, List<Option> shelves) {
    }

    /** Where units were taken from: one shelf (id + code), several shelves (code only, e.g. "A1, A2"), or none. */
    public record Taken(Long shelfId, String code) {
        static final Taken NONE = new Taken(null, null);
    }

    /** One shelf with what's on it, for the Shelves page. */
    public record ShelfView(Shelf shelf, List<ShelfStock> contents, long units) {
    }

    /** A product with stock that isn't on any shelf yet. */
    public record Unplaced(Product product, long quantity) {
    }

    private final ShelfRepository shelves;
    private final ShelfStockRepository stock;
    private final ProductRepository products;
    private final PricingService pricing;
    private final AuditService audit;
    private final ConfigService config;

    public ShelfService(ShelfRepository shelves, ShelfStockRepository stock, ProductRepository products,
                        PricingService pricing, AuditService audit, ConfigService config) {
        this.config = config;
        this.shelves = shelves;
        this.stock = stock;
        this.products = products;
        this.pricing = pricing;
        this.audit = audit;
    }

    // ------------------------------------------------------------- reading

    @Transactional(readOnly = true)
    public List<Shelf> all() {
        return shelves.findAllByOrderByCodeAsc();
    }

    public long onShelves(Product p) {
        return stock.totalOnShelves(p.getId()).longValue();
    }

    public long notOnShelf(Product p) {
        return Math.max(0, pricing.available(p.getName()) - onShelves(p));
    }

    /** Shelves switched on in Settings. */
    public boolean enabled() {
        return config.get().isUseShelves();
    }

    @Transactional(readOnly = true)
    public ProductShelves forProduct(String productName) {
        Product p = products.findByName(productName).orElseThrow(() -> new NotFoundException("Product not found"));
        List<Option> opts = stock.placements(p.getId()).stream()
                .map(s -> new Option(s.getShelf().getId(), s.getShelf().getCode(), s.getQuantity())).toList();
        long total = pricing.available(p.getName());
        long onShelves = opts.stream().mapToLong(Option::quantity).sum();
        return new ProductShelves(p.getName(), p.getUnit(), total, total - onShelves, opts);
    }

    @Transactional(readOnly = true)
    public List<ShelfView> overview() {
        List<ShelfView> out = new ArrayList<>();
        for (Shelf s : shelves.findAllByOrderByCodeAsc()) {
            List<ShelfStock> c = stock.contents(s.getId());
            out.add(new ShelfView(s, c, c.stream().mapToLong(ShelfStock::getQuantity).sum()));
        }
        return out;
    }

    /** Products that have stock not yet placed on any shelf. */
    @Transactional(readOnly = true)
    public List<Unplaced> unplaced() {
        List<Unplaced> out = new ArrayList<>();
        for (Product p : products.findAllByOrderByNameAsc()) {
            long n = notOnShelf(p);
            if (n > 0) {
                out.add(new Unplaced(p, n));
            }
        }
        return out;
    }

    /** "A1: 2, A2: 3" per product id, for the products list. */
    @Transactional(readOnly = true)
    public Map<Long, String> summaryByProduct() {
        Map<Long, String> m = new LinkedHashMap<>();
        for (ShelfStock s : stock.allPlacements()) {
            m.merge(s.getProduct().getId(), s.getShelf().getCode() + ": " + s.getQuantity(), (a, b) -> a + ", " + b);
        }
        return m;
    }

    // ------------------------------------------------- taking and putting

    /**
     * Takes units for a sale or a removal. Call before the line is saved.
     *
     * - Shelves switched off: nothing happens and nothing is limited.
     * - A shelf chosen: taken from that shelf (refused if it holds less).
     * - "Not on a shelf": taken from stock that isn't on any shelf; if that
     *   isn't enough, you're asked to choose the shelf.
     */
    @Transactional
    public Taken take(Product p, Long shelfId, int qty) {
        if (!enabled()) {
            return Taken.NONE;
        }
        if (shelfId == null) {
            long free = notOnShelf(p);
            if (qty > free) {
                List<String> where = stock.placements(p.getId()).stream()
                        .map(s -> s.getShelf().getCode() + " (" + s.getQuantity() + ")").toList();
                throw new OrderException("Choose the shelf to take " + p.getName() + " from: only " + free
                        + " " + p.getUnit() + " are not on a shelf. On shelves: " + String.join(", ", where) + ".");
            }
            return Taken.NONE;
        }
        ShelfStock s = stock.findByShelfIdAndProductId(shelfId, p.getId())
                .orElseThrow(() -> new OrderException(p.getName() + " isn't on that shelf."));
        if (qty > s.getQuantity()) {
            throw new OrderException("Shelf " + s.getShelf().getCode() + " has only " + s.getQuantity() + " "
                    + p.getUnit() + " of " + p.getName() + ".");
        }
        s.setQuantity(s.getQuantity() - qty);
        stock.save(s);
        return new Taken(s.getShelf().getId(), s.getShelf().getCode());
    }

    /**
     * For an offer turned into a sale: stock not on a shelf if there's enough,
     * otherwise the single shelf with the most of it that can cover it.
     */
    @Transactional
    public Taken takeAutomatically(Product p, int qty) {
        if (!enabled() || notOnShelf(p) >= qty) {
            return Taken.NONE;
        }
        ShelfStock best = stock.placements(p.getId()).stream()
                .filter(s -> s.getQuantity() >= qty)
                .max(Comparator.comparingInt(ShelfStock::getQuantity))
                .orElseThrow(() -> new OrderException(p.getName() + " is spread over several shelves, so the shelves can't be"
                        + " chosen automatically. Create the sale by hand and pick the shelves."));
        best.setQuantity(best.getQuantity() - qty);
        stock.save(best);
        return new Taken(best.getShelf().getId(), best.getShelf().getCode());
    }

    /** Puts units on a shelf (purchase put away, return, undo of a sale). Unknown shelf: they stay "not on a shelf". */
    @Transactional
    public Shelf put(Product p, Long shelfId, int qty) {
        if (shelfId == null || qty <= 0 || !enabled()) {
            return null;
        }
        Shelf shelf = shelves.findById(shelfId).orElse(null);
        if (shelf == null) {
            return null;
        }
        ShelfStock s = stock.findByShelfIdAndProductId(shelfId, p.getId()).orElseGet(() -> {
            ShelfStock n = new ShelfStock();
            n.setShelf(shelf);
            n.setProduct(p);
            return n;
        });
        s.setQuantity(s.getQuantity() + qty);
        stock.save(s);
        return shelf;
    }

    /** Takes up to qty off a shelf (undoing a purchase that was put on it). */
    @Transactional
    public void takeUpTo(Product p, Long shelfId, int qty) {
        if (shelfId == null) {
            return;
        }
        stock.findByShelfIdAndProductId(shelfId, p.getId()).ifPresent(s -> {
            s.setQuantity(Math.max(0, s.getQuantity() - qty));
            stock.save(s);
        });
    }

    /** Moves units between shelves, or between a shelf and "not on a shelf" (null). */
    @Transactional
    public void move(String productName, Long fromShelfId, Long toShelfId, int qty) {
        Product p = products.findByName(productName == null ? "" : productName)
                .orElseThrow(() -> new OrderException("Choose a product."));
        if (qty < 1) {
            throw new OrderException("Quantity must be at least 1.");
        }
        if (java.util.Objects.equals(fromShelfId, toShelfId)) {
            throw new OrderException("Choose a different place to move to.");
        }
        if (fromShelfId == null && qty > notOnShelf(p)) {
            throw new OrderException("Only " + notOnShelf(p) + " " + p.getUnit() + " of " + p.getName() + " are not on a shelf.");
        }
        Taken from = take(p, fromShelfId, qty);
        Shelf to = put(p, toShelfId, qty);
        audit.log("MOVE_STOCK", "Product", p.getName(), qty + " " + p.getUnit() + " from "
                + (from.code() == null ? "not on a shelf" : from.code()) + " to " + (to == null ? "not on a shelf" : to.getCode()));
    }

    // ------------------------------------------------------- shelves CRUD

    @Transactional
    public Shelf save(Long id, String code, String description) {
        String c = code == null ? "" : code.trim().toUpperCase();
        if (c.isEmpty()) {
            throw new OrderException("Enter a shelf code, e.g. A1.");
        }
        if (id == null ? shelves.existsByCode(c) : shelves.existsByCodeAndIdNot(c, id)) {
            throw new OrderException("Shelf " + c + " already exists.");
        }
        Shelf s = id == null ? new Shelf() : shelves.findById(id).orElseThrow(() -> new NotFoundException("Shelf not found"));
        s.setCode(c);
        s.setDescription(description);
        Shelf saved = shelves.save(s);
        audit.log(id == null ? "CREATE" : "UPDATE", "Shelf", saved.getCode(), saved.getDescription());
        return saved;
    }

    /** Deleting a shelf keeps its stock: those units become "not on a shelf". */
    @Transactional
    public void delete(Long id) {
        Shelf s = shelves.findById(id).orElseThrow(() -> new NotFoundException("Shelf not found"));
        long units = stock.contents(id).stream().mapToLong(ShelfStock::getQuantity).sum();
        shelves.delete(s);
        audit.log("DELETE", "Shelf", s.getCode(), units + " units moved to not on a shelf");
    }
}
