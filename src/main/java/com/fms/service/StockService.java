package com.fms.service;

import com.fms.domain.AppUser;
import com.fms.domain.NotFoundException;
import com.fms.domain.Product;
import com.fms.domain.StockAdjustment;
import com.fms.repo.ProductRepository;
import com.fms.repo.StockAdjustmentRepository;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stock adjustments: returns from clients, returns to merchants, damage, loss
 * and corrections after a stock count. They change stock without a purchase
 * or sale. Removing more than is in stock is refused.
 */
@Service
public class StockService {

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private TrackingService tracking;

    @org.springframework.beans.factory.annotation.Autowired
    private PeriodLock periodLock;

    private final StockAdjustmentRepository repo;
    private final ProductRepository products;
    private final PricingService pricing;
    private final AuditService audit;
    private final ShelfService shelves;

    public StockService(StockAdjustmentRepository repo, ProductRepository products,
                        PricingService pricing, AuditService audit, ShelfService shelves) {
        this.shelves = shelves;
        this.repo = repo;
        this.products = products;
        this.pricing = pricing;
        this.audit = audit;
    }

    @Transactional
    public StockAdjustment add(String productName, StockAdjustment.Reason reason, int quantity,
                               String refNo, String note, AppUser user) {
        return add(productName, reason, quantity, refNo, note, null, user);
    }

    /** @param shelfId shelf the units come off (removals) or go onto (additions); null = not on a shelf */
    @Transactional
    public StockAdjustment add(String productName, StockAdjustment.Reason reason, int quantity,
                               String refNo, String note, Long shelfId, AppUser user) {
        Product product = products.findByName(productName == null ? "" : productName.trim())
                .orElseThrow(() -> new OrderException("Choose a product."));
        if (reason == null) {
            throw new OrderException("Choose a reason.");
        }
        if (quantity < 1) {
            throw new OrderException("Quantity must be at least 1.");
        }
        int signed = quantity * reason.getSign();
        if (signed < 0) {
            long available = pricing.available(product.getName());
            if (quantity > available) {
                throw new OrderException("Only " + Math.max(0, available) + " " + product.getUnit() + " of "
                        + product.getName() + " are in stock, so " + quantity + " can't be removed.");
            }
        }
        ShelfService.Taken taken = signed < 0 ? shelves.take(product, shelfId, quantity) : ShelfService.Taken.NONE;
        StockAdjustment a = new StockAdjustment();
        a.setLocationId(StockLocation.get());          // Locations module: where the change happens
        a.setProductName(product.getName());
        a.setQuantity(signed);
        a.setReason(reason);
        a.setRefNo(refNo == null || refNo.isBlank() ? null : refNo.trim());
        a.setNote(note == null || note.isBlank() ? null : note.trim());
        a.setCreatedBy(user.getUsername());
        a.setCreatedAt(LocalDateTime.now());
        if (tracking.enabled()) {                    // Tracking module
            if (product.isBatchTracked()) {
                if (signed < 0) {
                    var b = tracking.pickBatch(product.getName(), quantity, StockLocation.get(), TrackingService.hintBatch());
                    a.setBatchNo(b.batchNo());
                    a.setExpiryDate(b.expiry());
                } else {
                    a.setBatchNo(TrackingService.hintBatch() != null ? TrackingService.hintBatch() : "–");
                    a.setExpiryDate(TrackingService.hintExpiry());
                }
            }
            if (product.isSerialTracked() && signed < 0 && reason != StockAdjustment.Reason.TRANSFER_OUT) {
                tracking.removed(product.getName(), quantity, StockLocation.get());
            }
        }
        if (signed > 0) {
            com.fms.domain.Shelf put = shelves.put(product, shelfId, quantity);
            if (put != null) {
                taken = new ShelfService.Taken(put.getId(), put.getCode());
            }
        }
        a.setShelfId(taken.shelfId());
        a.setShelfCode(taken.code());
        StockAdjustment saved = repo.save(a);
        audit.log("STOCK_ADJUSTMENT", "Product", product.getName(),
                (signed > 0 ? "+" : "") + signed + " " + product.getUnit() + ": " + reason.getLabel()
                + (a.getRefNo() == null ? "" : " (ref " + a.getRefNo() + ")"));
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        StockAdjustment a = repo.findById(id).orElseThrow(() -> new NotFoundException("Adjustment not found"));
        periodLock.check(a.getCreatedAt().toLocalDate());
        if (a.getQuantity() > 0) {
            // Undoing an addition removes stock again: make sure it's still there.
            long available = pricing.availableFor(a.getProductName(), a.getLocationId());
            if (a.getQuantity() > available) {
                throw new OrderException("Can't undo: only " + Math.max(0, available) + " of " + a.getProductName()
                        + " are left in stock, and this adjustment added " + a.getQuantity() + ".");
            }
        }
        if (a.getShelfId() != null) {
            products.findByName(a.getProductName()).ifPresent(p -> {
                if (a.getQuantity() > 0) {
                    shelves.takeUpTo(p, a.getShelfId(), a.getQuantity());
                } else {
                    shelves.put(p, a.getShelfId(), -a.getQuantity());
                }
            });
        }
        repo.delete(a);
        audit.log("DELETE_ADJUSTMENT", "Product", a.getProductName(),
                "Undid " + (a.getQuantity() > 0 ? "+" : "") + a.getQuantity() + ": " + a.getReason().getLabel());
    }

    @Transactional(readOnly = true)
    public Page<StockAdjustment> search(String text, Pageable pageable) {
        return repo.search("%" + (text == null ? "" : text.trim().toLowerCase()) + "%", pageable);
    }
}
