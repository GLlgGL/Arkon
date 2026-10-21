package com.fms.service;

import com.fms.domain.AppUser;
import com.fms.domain.BomLine;
import com.fms.domain.NotFoundException;
import com.fms.domain.Product;
import com.fms.domain.ProductionLine;
import com.fms.domain.ProductionOrder;
import com.fms.domain.StockAdjustment;
import com.fms.repo.BomLineRepository;
import com.fms.repo.ProductRepository;
import com.fms.repo.ProductionLineRepository;
import com.fms.repo.ProductionOrderRepository;
import com.fms.repo.StockAdjustmentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production (prodhim): bills of materials and production orders. Completing an
 * order uses up the components and adds the finished product, as stock
 * adjustments ("Used in production" / "Produced"), so stock is right
 * everywhere. The production cost per unit feeds the product's average cost.
 */
@Service
public class ProductionService {

    @org.springframework.beans.factory.annotation.Autowired
    private PeriodLock periodLock;

    public record BomView(BomLine line, Product component) {
    }

    private final BomLineRepository bom;
    private final ProductionOrderRepository orders;
    private final ProductionLineRepository prodLines;
    private final ProductRepository products;
    private final StockAdjustmentRepository adjustments;
    private final PricingService pricing;
    private final StockService stock;
    private final AuditService audit;

    public ProductionService(BomLineRepository bom, ProductionOrderRepository orders, ProductionLineRepository prodLines,
                             ProductRepository products, StockAdjustmentRepository adjustments, PricingService pricing,
                             StockService stock, AuditService audit) {
        this.bom = bom;
        this.orders = orders;
        this.prodLines = prodLines;
        this.products = products;
        this.adjustments = adjustments;
        this.pricing = pricing;
        this.stock = stock;
        this.audit = audit;
    }

    // -------------------------------------------------------------------- BOM

    @Transactional(readOnly = true)
    public List<Product> productsWithBom() {
        List<Long> ids = bom.productsWithBom();
        return products.findAllByOrderByNameAsc().stream().filter(p -> ids.contains(p.getId())).toList();
    }

    @Transactional(readOnly = true)
    public List<BomView> bom(Long productId) {
        List<BomView> out = new ArrayList<>();
        for (BomLine l : bom.findByProductIdOrderByIdAsc(productId)) {
            products.findById(l.getComponentId()).ifPresent(c -> out.add(new BomView(l, c)));
        }
        return out;
    }

    @Transactional
    public void addComponent(Long productId, Long componentId, int qty) {
        if (productId == null || componentId == null || productId.equals(componentId) || qty < 1) {
            throw new OrderException("Choose a component (not the product itself) and a quantity.");
        }
        for (BomLine l : bom.findByProductIdOrderByIdAsc(productId)) {
            if (l.getComponentId().equals(componentId)) {
                l.setQuantity(qty);
                bom.save(l);
                return;
            }
        }
        BomLine l = new BomLine();
        l.setProductId(productId);
        l.setComponentId(componentId);
        l.setQuantity(qty);
        bom.save(l);
        audit.log("UPDATE", "Bill of materials", String.valueOf(productId), "component " + componentId + " x " + qty);
    }

    @Transactional
    public void removeComponent(Long lineId) {
        bom.deleteById(lineId);
    }

    /** Cost to make one unit, at today's average costs. */
    public BigDecimal unitCost(Long productId) {
        BigDecimal c = BigDecimal.ZERO;
        for (BomView v : bom(productId)) {
            c = c.add(pricing.averageCost(v.component()).multiply(BigDecimal.valueOf(v.line().getQuantity())));
        }
        return Money.round(c);
    }

    // ----------------------------------------------------------------- orders

    @Transactional(readOnly = true)
    public List<ProductionOrder> all() {
        return orders.findAllByOrderByCreatedAtDesc();
    }

    public ProductionOrder get(String code) {
        return orders.findByCode(code).orElseThrow(() -> new NotFoundException("Production order not found"));
    }

    @Transactional(readOnly = true)
    public List<ProductionLine> lines(Long orderId) {
        return prodLines.findByOrderIdOrderByIdAsc(orderId);
    }

    @Transactional
    public ProductionOrder create(Long productId, int qty, LocalDate planned, String note, AppUser user) {
        Product p = products.findById(productId == null ? -1L : productId).orElseThrow(() -> new OrderException("Choose a product."));
        List<BomView> parts = bom(p.getId());
        if (parts.isEmpty()) {
            throw new OrderException(p.getName() + " has no bill of materials yet. Add its components first.");
        }
        if (qty < 1) {
            throw new OrderException("Quantity must be at least 1.");
        }
        ProductionOrder o = new ProductionOrder();
        o.setCode(String.format("PR-%05d", orders.nextCode().longValue()));
        o.setProductName(p.getName());
        o.setQuantity(qty);
        o.setPlannedOn(planned == null ? LocalDate.now() : planned);
        o.setNote(note == null || note.isBlank() ? null : note.trim());
        o.setCreatedBy(user.getUsername());
        o = orders.save(o);
        for (BomView v : parts) {
            ProductionLine l = new ProductionLine();
            l.setOrderId(o.getId());
            l.setComponentName(v.component().getName());
            l.setUnit(v.component().getUnit());
            l.setQuantity(v.line().getQuantity() * qty);
            l.setUnitCost(pricing.averageCost(v.component()));
            prodLines.save(l);
        }
        audit.log("CREATE", "Production order", o.getCode(), qty + " x " + p.getName());
        return o;
    }

    /** Makes the goods: components out of stock, finished product into stock. */
    @Transactional
    public void complete(String code, AppUser user) {
        ProductionOrder o = get(code);
        if (!o.isDraft()) {
            throw new OrderException("This production order is already " + o.getStatus().toLowerCase() + ".");
        }
        List<ProductionLine> ls = lines(o.getId());
        List<String> shortages = new ArrayList<>();
        for (ProductionLine l : ls) {
            long available = pricing.available(l.getComponentName());
            if (l.getQuantity() > available) {
                shortages.add(l.getComponentName() + " (needs " + l.getQuantity() + ", in stock " + Math.max(0, available) + ")");
            }
        }
        if (!shortages.isEmpty()) {
            throw new OrderException("Not enough components: " + String.join(", ", shortages) + ".");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ProductionLine l : ls) {
            Product c = products.findByName(l.getComponentName()).orElse(null);
            BigDecimal unit = c == null ? Money.nz(l.getUnitCost()) : pricing.averageCost(c);
            l.setUnitCost(unit);
            prodLines.save(l);
            total = total.add(unit.multiply(BigDecimal.valueOf(l.getQuantity())));
            stock.add(l.getComponentName(), StockAdjustment.Reason.PRODUCTION_OUT, l.getQuantity(), o.getCode(), null, user);
        }
        stock.add(o.getProductName(), StockAdjustment.Reason.PRODUCTION_IN, o.getQuantity(), o.getCode(), null, user);
        o.setTotalCost(Money.round(total));
        o.setUnitCost(total.divide(BigDecimal.valueOf(o.getQuantity()), 2, RoundingMode.HALF_UP));
        o.setStatus("DONE");
        o.setDoneAt(LocalDateTime.now());
        orders.save(o);
        audit.log("COMPLETE", "Production order", o.getCode(), o.getQuantity() + " x " + o.getProductName()
                + ", cost " + Money.format(o.getTotalCost()));
    }

    /** Undoes a completed order (components back, finished goods out), if the goods are still in stock. */
    @Transactional
    public void reverse(String code) {
        ProductionOrder o = get(code);
        if (!o.isDone()) {
            throw new OrderException("Only a completed production order can be reversed.");
        }
        if (o.getDoneAt() != null) periodLock.check(o.getDoneAt().toLocalDate());
        if (pricing.available(o.getProductName()) < o.getQuantity()) {
            throw new OrderException("The produced goods are no longer all in stock, so this can't be reversed.");
        }
        for (StockAdjustment a : adjustments.findAll()) {
            if (o.getCode().equals(a.getRefNo()) && !a.getReason().isManual()) {
                adjustments.delete(a);
            }
        }
        o.setStatus("DRAFT");
        o.setDoneAt(null);
        orders.save(o);
        audit.log("REVERSE", "Production order", o.getCode(), null);
    }

    @Transactional
    public void cancel(String code) {
        ProductionOrder o = get(code);
        if (!o.isDraft()) {
            throw new OrderException("Only a draft can be cancelled.");
        }
        o.setStatus("CANCELLED");
        orders.save(o);
        audit.log("CANCEL", "Production order", o.getCode(), null);
    }
}
