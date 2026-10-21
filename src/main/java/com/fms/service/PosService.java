package com.fms.service;

import com.fms.domain.*;
import com.fms.repo.ClientRepository;
import com.fms.repo.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quick sale (POS): a whole cart becomes one normal sale, paid at once.
 * All or nothing: if any product can't be sold (stock, shelves), nothing is saved.
 */
@Service
public class PosService {

    public static final String WALK_IN = "Klient pakice";

    public record Item(String product, int quantity) { }
    public record Result(String code, BigDecimal total, BigDecimal received, BigDecimal change) { }

    private final OrderService orders;
    private final PaymentService payments;
    private final PartyService parties;
    private final ClientRepository clients;
    private final ProductRepository products;
    private final ShelfService shelves;
    private final ConfigService config;
    private final AuditService audit;

    public PosService(OrderService orders, PaymentService payments, PartyService parties, ClientRepository clients,
                      ProductRepository products, ShelfService shelves, ConfigService config, AuditService audit) {
        this.orders = orders; this.payments = payments; this.parties = parties; this.clients = clients;
        this.products = products; this.shelves = shelves; this.config = config; this.audit = audit;
    }

    /** The company selling: the one chosen in Settings, or the first company. */
    public String company() {
        String c = config.get().getPosCompany();
        List<String> all = parties.providerNames(OrderType.SALE);
        if (c != null && all.contains(c)) return c;
        if (all.isEmpty()) throw new OrderException("Add your company first (Administration → Companies).");
        return all.get(0);
    }

    /** The default client for walk-in sales. */
    public String defaultClient() {
        String c = config.get().getPosClient();
        return c == null ? WALK_IN : c;
    }

    @Transactional
    public Result checkout(List<Item> cart, String client, Payment.Method method, BigDecimal received, Long accountId, AppUser user) {
        // the same product twice becomes one line
        Map<String, Integer> lines = new LinkedHashMap<>();
        for (Item i : cart) {
            if (i.product() == null || i.product().isBlank() || i.quantity() < 1) continue;
            lines.merge(i.product().trim(), i.quantity(), Integer::sum);
        }
        if (lines.isEmpty()) throw new OrderException("The cart is empty.");
        String buyer = client == null || client.isBlank() ? defaultClient() : client.trim();
        if (clients.findByName(buyer).isEmpty()) {          // walk-in client, created the first time
            Client c = new Client();
            c.setName(buyer);
            clients.save(c);
        }
        String company = company();
        String code = null;
        for (var e : lines.entrySet()) {
            Product p = products.findByName(e.getKey()).orElseThrow(() -> new OrderException("Unknown product: " + e.getKey()));
            AddLineForm f = new AddLineForm();
            f.setProvider(company);
            f.setBuyer(buyer);
            f.setProduct(p.getName());
            f.setQuantity(e.getValue());
            f.setShelfId(shelves.chooseShelf(p, e.getValue()));
            OrderLine l = orders.addLine(OrderType.SALE, code, f, user);
            code = l.getOrderCode();
        }
        OrderView o = orders.load(OrderType.SALE, code);
        BigDecimal total = o.valueWithVat();
        Payment.Method m = method == null ? Payment.Method.CASH : method;
        BigDecimal got = received == null || m != Payment.Method.CASH ? total : received;
        if (got.compareTo(total) < 0) {
            throw new OrderException("The amount received is less than the total (" + Money.format(total) + ").");
        }
        payments.add(o, LocalDate.now(), total, m, "POS", Payment.Kind.PAYMENT, accountId, user);
        audit.log("POS_SALE", "Sale", code, Money.format(total) + " " + m + " (" + buyer + ")");
        return new Result(code, total, got, Money.round(got.subtract(total)));
    }
}
