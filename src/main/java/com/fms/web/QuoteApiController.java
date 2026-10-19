package com.fms.web;

import com.fms.domain.OrderType;
import com.fms.service.PartyService;
import com.fms.service.PricingService;
import com.fms.service.Quote;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Live price preview for the order page. Replaces the AutoPostBack round trips
 * (product_SelectedIndexChanged, Amount_TextChanged, Buyer_SelectedIndexChanged)
 * that re-rendered the whole page through UpdatePanels.
 */
@RestController
public class QuoteApiController {

    private final PricingService pricing;
    private final PartyService parties;
    private final com.fms.service.ShelfService shelves;

    public QuoteApiController(PricingService pricing, PartyService parties, com.fms.service.ShelfService shelves) {
        this.pricing = pricing;
        this.parties = parties;
        this.shelves = shelves;
    }

    /** Where a product is: each shelf with its quantity, and how much isn't on a shelf. */
    @GetMapping("/api/shelves")
    public com.fms.service.ShelfService.ProductShelves productShelves(@RequestParam String product) {
        return shelves.forProduct(product);
    }

    @GetMapping("/api/quote")
    public Quote quote(@RequestParam String type,
                       @RequestParam String product,
                       @RequestParam(required = false) String buyer,
                       @RequestParam(required = false) BigDecimal discount,
                       @RequestParam(defaultValue = "1") int qty,
                       @RequestParam(required = false) BigDecimal price) {
        OrderType t = OrderType.fromPath(type);
        return pricing.quote(t, product, buyer, discount, qty, t == OrderType.PURCHASE ? price : null);
    }

    /** Contact details for the selected provider or buyer, shown next to the drop-down. */
    @GetMapping("/api/party")
    public Map<String, Object> party(@RequestParam String type, @RequestParam String role, @RequestParam String name) {
        OrderType t = OrderType.fromPath(type);
        var p = "provider".equals(role) ? parties.provider(t, name) : parties.buyer(t, name);
        return Map.of("name", p.name(), "tin", nz(p.tin()), "address", nz(p.address()),
                "email", nz(p.email()), "phone", nz(p.phone()), "registered", p.registered());
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
