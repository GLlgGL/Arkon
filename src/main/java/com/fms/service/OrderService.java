package com.fms.service;

import com.fms.domain.AppUser;
import com.fms.domain.NotFoundException;
import com.fms.domain.OrderInfo;
import com.fms.domain.OrderLine;
import com.fms.domain.OrderType;
import com.fms.domain.Product;
import com.fms.repo.OrderInfoRepository;
import com.fms.repo.OrderLineRepository;
import com.fms.repo.PaymentRepository;
import com.fms.repo.ProductRepository;
import com.fms.repo.OrderSummary;
import com.fms.repo.Totals;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Creating and changing purchases, sales and offers. Replaces addproduct_Click,
 * Delete_Click and BindGrid in the New* and ListOf*Update pages.
 */
@Service
public class OrderService {

    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.repo.ClientRepository clients;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private CurrencyService currency;

    @org.springframework.beans.factory.annotation.Autowired
    private PeriodLock periodLock;

    private final OrderLineRepository lines;
    private final PricingService pricing;
    private final PartyService parties;
    private final OrderInfoRepository infos;
    private final PaymentRepository payments;
    private final ProductRepository products;
    private final ConfigService config;
    private final AuditService audit;
    private final ShelfService shelves;
    private final CashService cash;

    public OrderService(OrderLineRepository lines, PricingService pricing, PartyService parties,
                        OrderInfoRepository infos, PaymentRepository payments, ProductRepository products,
                        ConfigService config, AuditService audit, ShelfService shelves, CashService cash) {
        this.shelves = shelves;
        this.cash = cash;
        this.lines = lines;
        this.pricing = pricing;
        this.parties = parties;
        this.infos = infos;
        this.payments = payments;
        this.products = products;
        this.config = config;
        this.audit = audit;
    }

    /**
     * A fresh 8-digit order number: 00000001, 00000002, ... The order exists
     * once its first line is saved. Numbers already in use (for example codes
     * imported from the old system) are skipped. The sequence is shared by
     * purchases, sales and offers, so a number is never reused across types.
     */
    @Transactional
    public String newOrderCode() {
        while (true) {
            String code = String.format("%08d", lines.nextOrderCode().longValue());
            if (!lines.existsByOrderCode(code)) {
                return code;
            }
        }
    }

    /** A new order that has no number yet (the number is given when the first product is saved). */
    public OrderView newOrder(OrderType type) {
        return new OrderView(type, null, List.of(), null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public OrderView load(OrderType type, String code) {
        List<OrderLine> list = lines.findByOrderTypeAndOrderCodeOrderByCreatedAtAscIdAsc(type, code);
        PartyInfo provider = null;
        PartyInfo buyer = null;
        if (!list.isEmpty()) {
            provider = parties.provider(type, list.get(0).getProviderName());
            buyer = parties.buyer(type, list.get(0).getBuyerName());
        }
        BigDecimal noVat = list.stream().map(OrderLine::getValueNoVat).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vat = list.stream().map(OrderLine::getVatValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal withVat = list.stream().map(OrderLine::getValueWithVat).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderView(type, code, list, provider, buyer, noVat, vat, withVat);
    }

    /**
     * Administrators can change any order. Other users can build a new order
     * and change orders they created. (The original blocked regular users from
     * the ListOf*Update pages entirely but let them edit while creating.)
     */
    public boolean canEdit(AppUser user, OrderView order) {
        return user.isAdmin() || order.isEmpty() || user.getUsername().equals(order.createdBy());
    }

    /**
     * Changes an order's exchange rate while nothing has been paid. Purchases
     * were typed in the foreign currency, so their base amounts follow the rate;
     * sales are priced in the base currency, so only the foreign amounts change.
     */
    @Transactional
    public void updateFxRate(OrderType type, String code, BigDecimal rate, AppUser user) {
        OrderView o = load(type, code);
        if (o.isEmpty() || !canEdit(user, o)) throw new OrderException("You can only change orders you created.");
        lockOrder(type, code);
        if (rate == null || rate.signum() <= 0) throw new OrderException("Enter a rate greater than 0.");
        if (payments.existsByOrderTypeAndOrderCode(type, code)) {
            throw new OrderException("This order already has payments, so its exchange rate can't change.");
        }
        OrderInfo i = infoForUpdate(type, code);
        if (!i.isForeign()) throw new OrderException("This order is in the base currency.");
        BigDecimal old = i.getFxRate();
        if (type == OrderType.PURCHASE) {
            for (OrderLine l : o.lines()) {
                l.setUnitPrice(Money.round(l.getUnitPrice().multiply(rate).divide(old, 6, java.math.RoundingMode.HALF_UP)));
                l.setValueNoVat(Money.round(l.getValueNoVat().multiply(rate).divide(old, 6, java.math.RoundingMode.HALF_UP)));
                l.setVatValue(Money.round(l.getVatValue().multiply(rate).divide(old, 6, java.math.RoundingMode.HALF_UP)));
                l.setValueWithVat(l.getValueNoVat().add(l.getVatValue()));
                lines.save(l);
            }
        }
        i.setFxRate(rate);
        infos.save(i);
        audit.log("UPDATE", type.getLabel(), code, "exchange rate " + old + " -> " + rate + " " + i.getCurrency());
    }

    /**
     * VAT of one sale or offer (when the client has no VAT of its own).
     * rate null = back to the normal rate. Recalculates every line.
     */
    @Transactional
    public void updateVat(OrderType type, String code, BigDecimal rate, String note, AppUser user) {
        if (type == OrderType.PURCHASE) throw new OrderException("VAT can be changed on sales and offers.");
        OrderView o = load(type, code);
        if (o.isEmpty() || !canEdit(user, o)) throw new OrderException("You can only change orders you created.");
        lockOrder(type, code);
        if (payments.existsByOrderTypeAndOrderCode(type, code)) {
            throw new OrderException("This order already has payments, so its VAT can't change.");
        }
        if (rate != null && (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new OrderException("Enter a VAT rate between 0 and 100.");
        }
        String buyer = o.lines().get(0).getBuyerName();
        if (clients.findByName(buyer).map(c -> c.getVatRate() != null).orElse(false)) {
            throw new OrderException("This client has its own VAT rate. Change it on the client's page.");
        }
        BigDecimal pct = rate != null ? rate : config.get().getVatPercent();
        for (OrderLine l : o.lines()) {
            l.setVatValue(Money.round(l.getValueNoVat().multiply(pct).divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP)));
            l.setValueWithVat(l.getValueNoVat().add(l.getVatValue()));
            lines.save(l);
        }
        OrderInfo i = infoForUpdate(type, code);
        i.setVatRate(rate);
        i.setVatNote(rate == null ? null : note);
        infos.save(i);
        audit.log("UPDATE", type.getLabel(), code, "VAT " + pct + "%" + (note == null || note.isBlank() ? "" : " – " + note));
    }

    /** An order started in a locked period can't be changed. */
    private void lockOrder(OrderType type, String code) {
        if (code == null || periodLock.lockedUntil() == null) return;
        OrderView o = load(type, code);
        if (!o.isEmpty()) {
            o.lines().stream().map(l -> l.getCreatedAt().toLocalDate()).min(java.util.Comparator.naturalOrder()).ifPresent(periodLock::check);
        }
    }

    /** May see the order (accountants see all, read-only). */
    public boolean canView(AppUser user, OrderView order) {
        return canEdit(user, order) || user.isSeesAll();
    }

    /**
     * Adds a product to an order. Pass {@code code = null} for a new order: it
     * gets the next number only after every check has passed, so abandoned or
     * rejected new orders don't leave gaps in the numbering.
     */
    @Transactional
    public OrderLine addLine(OrderType type, String code, AddLineForm form, AppUser user) {
        lockOrder(type, code);
        OrderView order = code == null ? newOrder(type) : load(type, code);
        if (!canEdit(user, order)) {
            throw new OrderException("You can only change orders you created.");
        }
        if (!StringUtils.hasText(form.getProvider()) || !StringUtils.hasText(form.getBuyer())) {
            throw new OrderException("Choose the " + type.getProviderLabel().toLowerCase()
                    + " and the " + type.getBuyerLabel().toLowerCase() + " first.");
        }
        if (!StringUtils.hasText(form.getProduct())) {
            throw new OrderException("Choose a product.");
        }
        if (form.getQuantity() < 1) {
            throw new OrderException("Quantity must be at least 1.");
        }

        // Once an order has lines, its provider and buyer are fixed (the
        // original disabled both drop-downs on the update pages).
        String providerName = order.isEmpty() ? form.getProvider() : order.lines().get(0).getProviderName();
        String buyerName = order.isEmpty() ? form.getBuyer() : order.lines().get(0).getBuyerName();

        if (order.isEmpty() && !parties.isValidProvider(type, providerName)) {
            throw new OrderException("Unknown " + type.getProviderLabel().toLowerCase() + ": " + providerName);
        }
        boolean buyerIsClient = parties.isValidBuyer(type, buyerName);
        if (order.isEmpty() && !buyerIsClient && type != OrderType.OFFER) {
            throw new OrderException("Unknown " + type.getBuyerLabel().toLowerCase() + ": " + buyerName);
        }

        // Currency: a new order takes the chosen currency at today's rate; later lines use the order's.
        String fxCode = null;
        BigDecimal fxRate = null;
        if (currency != null && currency.enabled()) {
            if (order.isEmpty()) {
                String c = form.getCurrency();
                if (c != null && com.fms.service.CurrencyService.FOREIGN.contains(c)) {
                    fxCode = c;
                    fxRate = currency.requireRate(c, LocalDate.now());
                }
            } else {
                var fx = currency.ofOrder(type, code);
                if (fx.foreign()) { fxCode = fx.code(); fxRate = fx.rate(); }
            }
        }
        BigDecimal paid = form.getUnitPrice();
        if (fxRate != null && paid != null) {
            paid = Money.round(paid.multiply(fxRate));      // price paid typed in the order's currency
        }
        Quote q = pricing.quote(type, form.getProduct(), buyerName, form.getManualDiscount(), form.getQuantity(),
                type == OrderType.PURCHASE ? paid : null);
        if (type.isOutgoing() && q.exceedsStock()) {
            long inStock = Math.max(0, q.available());
            throw new OrderException(inStock == 0
                    ? q.productName() + " is out of stock. Record a purchase first."
                    : "Not enough stock: you entered " + q.requestedQuantity() + " " + q.unit()
                      + " of " + q.productName() + ", but only " + inStock + " " + q.unit()
                      + " are in stock. Lower the quantity.");
        }

        // Shelves: a sale takes from the chosen shelf (or from stock that isn't on a shelf).
        Product product = products.findByName(q.productName()).orElseThrow(() -> new OrderException("Choose a product."));
        ShelfService.Taken taken = ShelfService.Taken.NONE;
        if (type == OrderType.SALE) {
            taken = shelves.take(product, form.getShelfId(), q.quantity());
        }

        if (code == null) {
            code = newOrderCode();
        }

        OrderLine line = new OrderLine();
        line.setOrderType(type);
        line.setOrderCode(code);
        line.setCreatedAt(LocalDateTime.now());
        line.setProductName(q.productName());
        line.setProviderName(providerName);
        line.setBuyerName(buyerName);
        line.setUnit(q.unit());
        line.setQuantity(q.quantity());
        line.setUnitPrice(q.unitPrice());
        line.setDiscountPercent(q.discountPercent());
        line.setValueNoVat(q.valueNoVat());
        line.setVatValue(q.vatValue());
        line.setValueWithVat(q.valueWithVat());
        line.setCreatedBy(user.getUsername());
        if (type == OrderType.SALE) {
            // Cost of the goods sold, at today's average purchase cost (for real profit figures).
            products.findByName(q.productName()).ifPresent(p -> line.setUnitCost(pricing.averageCost(p)));
        }
        if (type == OrderType.OFFER) {
            line.setExistingClient(buyerIsClient ? "Y" : "N");
            if (!buyerIsClient && order.isEmpty()) {
                parties.saveProspect(buyerName, form.getProspectTin(), form.getProspectAddress(),
                        form.getProspectEmail(), form.getProspectPhone());
            }
        }
        if (type == OrderType.PURCHASE && form.getShelfId() != null) {
            com.fms.domain.Shelf put = shelves.put(product, form.getShelfId(), q.quantity());
            if (put != null) {
                taken = new ShelfService.Taken(put.getId(), put.getCode());
            }
        }
        line.setShelfId(taken.shelfId());
        line.setShelfCode(taken.code());
        if (type != OrderType.PURCHASE && !order.isEmpty()) {        // the order's own VAT rate, if changed on the sale
            // every line of an order uses the order's rate (its own, or the normal one), so one invoice never mixes rates
            OrderInfo vi = infos.findByOrderTypeAndOrderCode(type, code).orElse(null);
            BigDecimal pct = vi != null && vi.getVatRate() != null ? vi.getVatRate() : config.get().getVatPercent();
            line.setVatValue(Money.round(line.getValueNoVat().multiply(pct).divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP)));
            line.setValueWithVat(line.getValueNoVat().add(line.getVatValue()));
        }
        OrderLine saved = lines.save(line);
        if (order.isEmpty()) {
            createInfo(type, code);
        } else {
            lines.touchOrder(type, code, LocalDateTime.now());
        }
        if (type != OrderType.PURCHASE && order.isEmpty()) {        // remember the client's VAT on the order
            clients.findByName(buyerName).filter(c -> c.getVatRate() != null).ifPresent(c -> {
                OrderInfo vi = infoForUpdate(type, saved.getOrderCode());
                vi.setVatRate(c.getVatRate());
                vi.setVatNote(c.getVatNote());
                infos.save(vi);
            });
        }
        if (fxCode != null && order.isEmpty()) {
            OrderInfo fi = infoForUpdate(type, saved.getOrderCode());
            fi.setCurrency(fxCode);
            fi.setFxRate(fxRate);
            infos.save(fi);
        }
        audit.log("ADD_LINE", type.getLabel(), code, q.quantity() + " " + q.unit() + " " + q.productName()
                + " at " + Money.format(q.unitPrice()) + " (" + buyerName + ")"
                + (taken.code() == null ? "" : ", shelf " + taken.code()));
        return saved;
    }

    @Transactional
    public void deleteLine(OrderType type, String code, Long lineId, AppUser user) {
        lockOrder(type, code);
        OrderView order = load(type, code);
        if (!canEdit(user, order)) {
            throw new OrderException("You can only change orders you created.");
        }
        OrderLine line = lines.findByIdAndOrderType(lineId, type)
                .filter(l -> l.getOrderCode().equals(code))
                .orElseThrow(() -> new NotFoundException("Line not found"));
        if (order.lines().stream().anyMatch(l -> lineId.equals(l.getReturnOf()))) {
            throw new OrderException("Part of this line was returned. Remove its return lines first.");
        }
        undoShelf(line);
        lines.delete(line);
        lines.touchOrder(type, code, LocalDateTime.now());
        audit.log("REMOVE_LINE", type.getLabel(), code, line.getQuantity() + " " + line.getUnit() + " " + line.getProductName());
    }

    /** Deleting a whole order is for administrators only, as in the original lists. */
    @Transactional
    public void deleteOrder(OrderType type, String code, AppUser user) {
        lockOrder(type, code);
        if (!user.isAdmin()) {
            throw new OrderException("Only administrators can delete orders.");
        }
        OrderView order = load(type, code);
        order.lines().forEach(this::undoShelf);
        lines.deleteOrder(type, code);
        cash.deleteOrder(type, code);
        payments.deleteOrder(type, code);
        infos.deleteOrder(type, code);
        audit.log("DELETE_ORDER", type.getLabel(), code, order.isEmpty() ? null
                : order.lines().size() + " lines, " + Money.format(order.valueWithVat()) + " with VAT, "
                  + order.lines().get(0).getBuyerName());
    }

    /** Removing a sale line puts it back on its shelf; removing a purchase line takes it off. */
    private void undoShelf(OrderLine l) {
        if (l.getShelfId() == null) {
            return;
        }
        products.findByName(l.getProductName()).ifPresent(p -> {
            if (l.getOrderType() == OrderType.SALE && l.getQuantity() < 0) {
                shelves.takeUpTo(p, l.getShelfId(), -l.getQuantity());   // undoing a return
            } else if (l.getOrderType() == OrderType.SALE) {
                shelves.put(p, l.getShelfId(), l.getQuantity());
            } else if (l.getOrderType() == OrderType.PURCHASE && l.getQuantity() < 0) {
                shelves.put(p, l.getShelfId(), -l.getQuantity());         // undoing a return to the merchant
            } else if (l.getOrderType() == OrderType.PURCHASE) {
                shelves.takeUpTo(p, l.getShelfId(), l.getQuantity());
            }
        });
    }

    // ------------------------------------------------------------- returns

    /** How many units of each sale line can still be returned (sold minus already returned). */
    public java.util.Map<Long, Integer> returnable(OrderView sale) {
        java.util.Map<Long, Integer> left = new LinkedHashMap<>();
        for (OrderLine l : sale.lines()) {
            if (!l.isReturn()) {
                left.put(l.getId(), l.getQuantity());
            }
        }
        for (OrderLine r : sale.lines()) {
            if (r.isReturn()) {
                left.computeIfPresent(r.getReturnOf(), (id, q) -> q + r.getQuantity());   // return lines are negative
            }
        }
        return left;
    }

    /**
     * Records a return on a sale (goods a client brought back) or on a
     * purchase (goods sent back to the merchant). Each returned item becomes
     * a minus line on the same order, at the same price and discount, so
     * stock and every total (reports, statistics, profit, debts) follow.
     *
     * Sales: stock goes back up; goods go back on the shelf they came from
     * (or the shelf given). Purchases: stock goes down (only what's still in
     * stock can be returned); goods come off the shelf they were put on (or
     * the shelf given). Returns the return number within the order.
     */
    @Transactional
    public int recordReturn(OrderType type, String code, java.util.Map<Long, Integer> quantities, Long shelfId,
                            com.fms.domain.ReturnReason reason, String note, AppUser user) {
        if (type != OrderType.SALE && type != OrderType.PURCHASE) {
            throw new OrderException("Returns are for sales and purchases.");
        }
        OrderView order = load(type, code);
        if (order.isEmpty()) {
            throw new NotFoundException("Order not found");
        }
        if (!canEdit(user, order)) {
            throw new OrderException("You can only change orders you created.");
        }
        if (reason == null) {
            throw new OrderException("Choose the reason for the return.");
        }
        java.util.Map<Long, Integer> left = returnable(order);
        int returnNo = order.lines().stream().map(OrderLine::getReturnNo).filter(java.util.Objects::nonNull)
                .max(Integer::compare).orElse(0) + 1;
        LocalDateTime now = LocalDateTime.now();
        List<String> done = new ArrayList<>();
        java.util.Map<String, Integer> outOfStock = new LinkedHashMap<>();
        for (OrderLine l : order.lines()) {
            Integer q = quantities.get(l.getId());
            if (l.isReturn() || q == null || q <= 0) {
                continue;
            }
            int max = left.getOrDefault(l.getId(), 0);
            if (q > max) {
                throw new OrderException("Only " + max + " " + l.getUnit() + " of " + l.getProductName() + " can still be returned.");
            }
            if (type == OrderType.PURCHASE) {
                outOfStock.merge(l.getProductName(), q, Integer::sum);
            }
        }
        // Sending back to a merchant: the goods must still be in stock.
        for (var e : outOfStock.entrySet()) {
            long available = pricing.available(e.getKey());
            if (e.getValue() > available) {
                throw new OrderException("Only " + Math.max(0, available) + " of " + e.getKey()
                        + " are in stock, so " + e.getValue() + " can't be returned to the merchant.");
            }
        }
        for (OrderLine l : order.lines()) {
            Integer q = quantities.get(l.getId());
            if (l.isReturn() || q == null || q <= 0) {
                continue;
            }
            BigDecimal share = BigDecimal.valueOf(q).divide(BigDecimal.valueOf(l.getQuantity()), 10, java.math.RoundingMode.HALF_UP);
            BigDecimal noVat = Money.round(l.getValueNoVat().multiply(share)).negate();
            BigDecimal vat = Money.round(l.getVatValue().multiply(share)).negate();
            OrderLine r = new OrderLine();
            r.setOrderType(type);
            r.setOrderCode(code);
            r.setCreatedAt(now);
            r.setProductName(l.getProductName());
            r.setProviderName(l.getProviderName());
            r.setBuyerName(l.getBuyerName());
            r.setExistingClient(l.getExistingClient());
            r.setUnit(l.getUnit());
            r.setQuantity(-q);
            r.setUnitPrice(l.getUnitPrice());
            r.setDiscountPercent(l.getDiscountPercent());
            r.setValueNoVat(noVat);
            r.setVatValue(vat);
            r.setValueWithVat(noVat.add(vat));
            r.setUnitCost(l.getUnitCost());
            r.setReturnOf(l.getId());
            r.setReturnNo(returnNo);
            r.setReturnReason(reason);
            r.setReturnNote(note);
            r.setCreatedBy(user.getUsername());
            Long target = shelfId != null ? shelfId : l.getShelfId();
            Product p = products.findByName(l.getProductName()).orElse(null);
            if (p != null && shelves.enabled()) {
                if (type == OrderType.SALE && target != null) {
                    com.fms.domain.Shelf put = shelves.put(p, target, q);
                    if (put != null) {
                        r.setShelfId(put.getId());
                        r.setShelfCode(put.getCode());
                    }
                } else if (type == OrderType.PURCHASE) {
                    ShelfService.Taken taken = shelves.take(p, target, q);
                    r.setShelfId(taken.shelfId());
                    r.setShelfCode(taken.code());
                }
            }
            lines.save(r);
            done.add(q + " " + l.getUnit() + " " + l.getProductName());
        }
        if (done.isEmpty()) {
            throw new OrderException("Enter how many of each item came back.");
        }
        lines.touchOrder(type, code, now);
        audit.log("RETURN", type.getLabel(), code, "Return " + returnNo + " (" + reason + "): " + String.join(", ", done));
        return returnNo;
    }

    /** One return on an order: what the returns list shows. */
    public record ReturnView(OrderType type, String orderCode, int returnNo, LocalDateTime date, String party,
                             List<OrderLine> lines, BigDecimal valueWithVat, int units,
                             com.fms.domain.ReturnReason reason, String note, String createdBy) {
        public String number() { return orderCode + "-" + returnNo; }
    }

    /** All returns of a type, newest first (users see their own). */
    @Transactional(readOnly = true)
    public List<ReturnView> returns(OrderType type, AppUser user, String search, java.time.LocalDate from, java.time.LocalDate to) {
        String q = search == null ? "" : search.trim().toLowerCase();
        java.util.Map<String, List<OrderLine>> groups = new LinkedHashMap<>();
        for (OrderLine l : lines.findReturnLines(type)) {
            if (!user.isSeesAll() && !user.getUsername().equals(l.getCreatedBy())) continue;
            if (from != null && l.getCreatedAt().toLocalDate().isBefore(from)) continue;
            if (to != null && l.getCreatedAt().toLocalDate().isAfter(to)) continue;
            groups.computeIfAbsent(l.getOrderCode() + "#" + l.getReturnNo(), k -> new ArrayList<>()).add(l);
        }
        List<ReturnView> out = new ArrayList<>();
        for (List<OrderLine> g : groups.values()) {
            OrderLine f = g.get(0);
            String party = type == OrderType.SALE ? f.getBuyerName() : f.getProviderName();
            if (!q.isEmpty() && !(f.getOrderCode().contains(q) || party.toLowerCase().contains(q)
                    || g.stream().anyMatch(l -> l.getProductName().toLowerCase().contains(q)))) {
                continue;
            }
            out.add(new ReturnView(type, f.getOrderCode(), f.getReturnNo(), f.getCreatedAt(), party, g,
                    g.stream().map(OrderLine::getValueWithVat).reduce(BigDecimal.ZERO, BigDecimal::add).negate(),
                    g.stream().mapToInt(l -> -l.getQuantity()).sum(), f.getReturnReason(), f.getReturnNote(), f.getCreatedBy()));
        }
        return out;
    }

    // ------------------------------------------------------------ order info

    /** Creates the order's info row: payment due date, or offer validity. */
    private OrderInfo createInfo(OrderType type, String code) {
        OrderInfo info = new OrderInfo();
        info.setOrderType(type);
        info.setOrderCode(code);
        var c = config.get();
        if (type == OrderType.OFFER) {
            info.setOfferStatus(OrderInfo.OfferStatus.OPEN);
            info.setValidUntil(LocalDate.now().plusDays(c.getOfferValidDays()));
        } else {
            info.setDueDate(LocalDate.now().plusDays(c.getPaymentTermsDays()));
        }
        return infos.save(info);
    }

    /**
     * The order's info. Orders saved before this feature (or imported) get
     * defaults based on their date, without writing anything.
     */
    @Transactional(readOnly = true)
    public OrderInfo info(OrderType type, String code, LocalDate orderDate) {
        return infos.findByOrderTypeAndOrderCode(type, code).orElseGet(() -> {
            OrderInfo i = new OrderInfo();
            i.setOrderType(type);
            i.setOrderCode(code);
            var c = config.get();
            LocalDate d = orderDate == null ? LocalDate.now() : orderDate;
            if (type == OrderType.OFFER) {
                i.setOfferStatus(OrderInfo.OfferStatus.OPEN);
                i.setValidUntil(d.plusDays(c.getOfferValidDays()));
            } else {
                i.setDueDate(d.plusDays(c.getPaymentTermsDays()));
            }
            return i;
        });
    }

    private OrderInfo infoForUpdate(OrderType type, String code) {
        OrderView order = load(type, code);
        if (order.isEmpty()) {
            throw new NotFoundException("Order not found");
        }
        return infos.findByOrderTypeAndOrderCode(type, code).orElseGet(() -> {
            OrderInfo i = info(type, code, order.lines().get(0).getCreatedAt().toLocalDate());
            return infos.save(i);
        });
    }

    @Transactional
    public void updateDueDate(OrderType type, String code, LocalDate dueDate, AppUser user) {
        if (!canEdit(user, load(type, code))) {
            throw new OrderException("You can only change orders you created.");
        }
        OrderInfo i = infoForUpdate(type, code);
        i.setDueDate(dueDate);
        infos.save(i);
        audit.log("SET_DUE_DATE", type.getLabel(), code, String.valueOf(dueDate));
    }

    /** Cost centre of a sale or purchase (null removes it). */
    @Transactional
    public void updateCostCenter(OrderType type, String code, Long costCenterId, AppUser user) {
        if (!canEdit(user, load(type, code))) {
            throw new OrderException("You can only change orders you created.");
        }
        OrderInfo i = infoForUpdate(type, code);
        i.setCostCenterId(costCenterId);
        infos.save(i);
        audit.log("SET_COST_CENTER", type.getLabel(), code, String.valueOf(costCenterId));
    }

    @Transactional
    public void updateOffer(String code, OrderInfo.OfferStatus status, LocalDate validUntil, AppUser user) {
        if (!canEdit(user, load(OrderType.OFFER, code))) {
            throw new OrderException("You can only change offers you created.");
        }
        OrderInfo i = infoForUpdate(OrderType.OFFER, code);
        if (i.getConvertedTo() != null) {
            throw new OrderException("This offer was already turned into sale " + i.getConvertedTo() + ".");
        }
        i.setOfferStatus(status);
        i.setValidUntil(validUntil);
        infos.save(i);
        audit.log("SET_OFFER_STATUS", "Offer", code, status + ", valid until " + validUntil);
    }

    // ------------------------------------------------------- offer to sale

    /**
     * Turns an offer into a new sale with the same lines and the prices that
     * were offered. Every product must be in stock in full; otherwise nothing
     * is created. An offer to a non-client registers them as a client first
     * (administrators only). Returns the new sale number.
     */
    @Transactional
    public String convertOfferToSale(String offerCode, AppUser user) {
        OrderView offer = load(OrderType.OFFER, offerCode);
        if (offer.isEmpty()) {
            throw new NotFoundException("Offer not found");
        }
        if (!canEdit(user, offer)) {
            throw new OrderException("You can only convert offers you created.");
        }
        OrderInfo info = infoForUpdate(OrderType.OFFER, offerCode);
        if (info.getConvertedTo() != null) {
            throw new OrderException("This offer was already turned into sale " + info.getConvertedTo() + ".");
        }
        String buyer = offer.lines().get(0).getBuyerName();
        if (!parties.isClient(buyer)) {
            if (!user.isAdmin()) {
                throw new OrderException(buyer + " isn't a registered client yet. Ask an administrator to convert this offer;"
                        + " it registers them as a client automatically.");
            }
            parties.registerClientFromProspect(buyer);
        }

        // Check stock for every product before creating anything.
        Map<String, Integer> needed = new LinkedHashMap<>();
        for (OrderLine l : offer.lines()) {
            needed.merge(l.getProductName(), l.getQuantity(), Integer::sum);
        }
        List<String> shortages = new ArrayList<>();
        needed.forEach((product, qty) -> {
            long available = pricing.available(product);
            if (qty > available) {
                shortages.add(product + " (needs " + qty + ", in stock " + Math.max(0, available) + ")");
            }
        });
        if (!shortages.isEmpty()) {
            throw new OrderException("Not enough stock to turn this offer into a sale: " + String.join(", ", shortages) + ".");
        }

        String saleCode = newOrderCode();
        LocalDateTime now = LocalDateTime.now();
        for (OrderLine o : offer.lines()) {
            OrderLine l = new OrderLine();
            l.setOrderType(OrderType.SALE);
            l.setOrderCode(saleCode);
            l.setCreatedAt(now);
            l.setProductName(o.getProductName());
            l.setProviderName(o.getProviderName());
            l.setBuyerName(o.getBuyerName());
            l.setUnit(o.getUnit());
            l.setQuantity(o.getQuantity());
            l.setUnitPrice(o.getUnitPrice());
            l.setDiscountPercent(o.getDiscountPercent());
            l.setValueNoVat(o.getValueNoVat());
            l.setVatValue(o.getVatValue());
            l.setValueWithVat(o.getValueWithVat());
            l.setCreatedBy(user.getUsername());
            Product p = products.findByName(o.getProductName()).orElse(null);
            if (p != null) {
                l.setUnitCost(pricing.averageCost(p));
                ShelfService.Taken t = shelves.takeAutomatically(p, o.getQuantity());
                l.setShelfId(t.shelfId());
                l.setShelfCode(t.code());
            }
            lines.save(l);
        }
        createInfo(OrderType.SALE, saleCode);

        info.setOfferStatus(OrderInfo.OfferStatus.ACCEPTED);
        info.setConvertedTo(saleCode);
        if (info.getVatRate() != null) {              // the sale keeps the offer's VAT
            OrderInfo vi = infoForUpdate(OrderType.SALE, saleCode);
            vi.setVatRate(info.getVatRate());
            vi.setVatNote(info.getVatNote());
            infos.save(vi);
        }
        if (info.isForeign()) {                       // the sale keeps the offer's currency and rate
            OrderInfo si = infoForUpdate(OrderType.SALE, saleCode);
            si.setCurrency(info.getCurrency());
            si.setFxRate(info.getFxRate());
            infos.save(si);
        }
        infos.save(info);
        audit.log("CONVERT_OFFER", "Offer", offerCode, "Became sale " + saleCode + " for " + buyer
                + ", " + Money.format(offer.valueWithVat()) + " with VAT");
        return saleCode;
    }

    /**
     * Orders for the list pages. Regular users only see their own, as in the
     * original's "Where Usern = Session['User']".
     */
    @Transactional(readOnly = true)
    public Page<OrderSummary> list(OrderType type, AppUser user, String search,
                                   LocalDate from, LocalDate to, Pageable pageable) {
        return lines.findSummaries(type, scope(user), start(from), end(to), like(search), pageable);
    }

    /** Footer totals for the list: every order matching the same filters, across all pages. */
    @Transactional(readOnly = true)
    public Totals listTotals(OrderType type, AppUser user, String search, LocalDate from, LocalDate to) {
        Totals t = lines.summaryTotals(type, scope(user), start(from), end(to), like(search));
        return t == null ? Totals.empty() : t.orZero();
    }

    private static String scope(AppUser user) {
        return user.isSeesAll() ? null : user.getUsername();
    }

    private static String like(String search) {
        return (StringUtils.hasText(search) ? search.trim().toLowerCase() : "") + "%";
    }

    private static LocalDateTime start(LocalDate from) {
        return from == null ? LocalDateTime.of(1900, 1, 1, 0, 0) : from.atStartOfDay();
    }

    private static LocalDateTime end(LocalDate to) {
        return to == null ? LocalDateTime.of(9999, 1, 1, 0, 0) : to.plusDays(1).atStartOfDay();
    }
}
