package com.fms.service;

import com.fms.domain.AppUser;
import com.fms.domain.NotFoundException;
import com.fms.domain.OrderLine;
import com.fms.domain.OrderType;
import com.fms.domain.Product;
import com.fms.domain.PurchaseOrder;
import com.fms.domain.PurchaseOrderLine;
import com.fms.repo.ProductRepository;
import com.fms.repo.PurchaseOrderRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purchase orders: made from the warehouse "For supply" list, sent to the
 * merchant (PDF / email), then received, which creates the real purchase and
 * only then adds the stock.
 */
@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository repo;
    private final ProductRepository products;
    private final OrderService orders;
    private final AuditService audit;

    public PurchaseOrderService(PurchaseOrderRepository repo, ProductRepository products, OrderService orders, AuditService audit) {
        this.repo = repo;
        this.products = products;
        this.orders = orders;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> all() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public PurchaseOrder get(String code) {
        return repo.findByCode(code).orElseThrow(() -> new NotFoundException("Purchase order not found"));
    }

    /** New purchase order for the chosen products and quantities, at each product's buy price. */
    @Transactional
    public PurchaseOrder create(String merchant, String company, Map<String, Integer> quantities, AppUser user) {
        if (merchant == null || merchant.isBlank() || company == null || company.isBlank()) {
            throw new OrderException("Choose the merchant and your company.");
        }
        PurchaseOrder po = new PurchaseOrder();
        po.setCode(String.format("PO-%05d", repo.nextCode().longValue()));
        po.setMerchantName(merchant);
        po.setCompanyName(company);
        po.setCreatedBy(user.getUsername());
        quantities.forEach((name, qty) -> {
            if (qty == null || qty < 1) {
                return;
            }
            Product p = products.findByName(name).orElse(null);
            if (p == null) {
                return;
            }
            PurchaseOrderLine l = new PurchaseOrderLine();
            l.setOrder(po);
            l.setProductName(p.getName());
            l.setUnit(p.getUnit());
            l.setQuantity(qty);
            l.setUnitPrice(Money.round(p.getBuyPrice()));
            po.getLines().add(l);
        });
        if (po.getLines().isEmpty()) {
            throw new OrderException("Tick at least one product and enter a quantity.");
        }
        PurchaseOrder saved = repo.save(po);
        audit.log("CREATE", "Purchase order", saved.getCode(), saved.getLines().size() + " products for " + merchant);
        return saved;
    }

    /** Change quantities (0 removes the line) and the note, while the order is open. */
    @Transactional
    public void update(String code, Map<Long, Integer> quantities, String note) {
        PurchaseOrder po = open(code);
        po.getLines().removeIf(l -> quantities.containsKey(l.getId()) && quantities.get(l.getId()) != null
                && quantities.get(l.getId()) < 1);
        po.getLines().forEach(l -> {
            Integer q = quantities.get(l.getId());
            if (q != null && q > 0) {
                l.setQuantity(q);
            }
        });
        if (po.getLines().isEmpty()) {
            throw new OrderException("A purchase order needs at least one product. Cancel it instead.");
        }
        po.setNote(note);
        repo.save(po);
        audit.log("UPDATE", "Purchase order", code, po.getLines().size() + " products");
    }

    /** Goods arrived: create the real purchase with these products. Returns the purchase number. */
    @Transactional
    public String receive(String code, AppUser user) {
        PurchaseOrder po = open(code);
        String purchase = null;
        for (PurchaseOrderLine l : po.getLines()) {
            AddLineForm f = new AddLineForm();
            f.setProvider(po.getMerchantName());
            f.setBuyer(po.getCompanyName());
            f.setProduct(l.getProductName());
            f.setQuantity(l.getQuantity());
            f.setUnitPrice(l.getUnitPrice());
            OrderLine saved = orders.addLine(OrderType.PURCHASE, purchase, f, user);
            purchase = saved.getOrderCode();
        }
        po.setStatus(PurchaseOrder.Status.RECEIVED);
        po.setPurchaseCode(purchase);
        repo.save(po);
        audit.log("RECEIVE", "Purchase order", code, "Became purchase " + purchase);
        return purchase;
    }

    @Transactional
    public void cancel(String code) {
        PurchaseOrder po = open(code);
        po.setStatus(PurchaseOrder.Status.CANCELLED);
        repo.save(po);
        audit.log("CANCEL", "Purchase order", code, null);
    }

    private PurchaseOrder open(String code) {
        PurchaseOrder po = get(code);
        if (!po.isOpen()) {
            throw new OrderException("This purchase order is already " + po.getStatus().name().toLowerCase() + ".");
        }
        return po;
    }
}
