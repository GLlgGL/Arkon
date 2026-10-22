package com.fms.web;

import com.fms.domain.Product;
import com.fms.repo.ProductRepository;
import com.fms.service.ConfigService;
import com.fms.service.LabelService;
import com.fms.service.Money;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Barcode labels for products: choose products and how many, then print. */
@Controller
public class LabelController {

    /** One label to print. */
    public record Label(String name, String code, String svg, BigDecimal price) { }

    private final ProductRepository products;
    private final LabelService labels;
    private final ConfigService config;

    public LabelController(ProductRepository products, LabelService labels, ConfigService config) {
        this.products = products; this.labels = labels; this.config = config;
    }

    @GetMapping("/labels")
    public String page(@RequestParam(required = false) String q, Model model) {
        String s = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        model.addAttribute("products", products.findAllByOrderByNameAsc().stream()
                .filter(p -> s.isEmpty() || p.getName().toLowerCase(Locale.ROOT).contains(s) || (p.getCode() != null && p.getCode().toLowerCase(Locale.ROOT).contains(s)))
                .toList());
        model.addAttribute("q", q);
        model.addAttribute("sizes", List.of("50x30", "40x25", "58x40", "a4-3x8"));
        return "labels/choose";
    }

    /** The printable labels: product ids with their counts, the size, and the price with or without VAT. */
    @GetMapping("/labels/print")
    public String print(@RequestParam Map<String, String> params, @RequestParam(defaultValue = "50x30") String size,
                        @RequestParam(defaultValue = "vat") String price, Model model) {
        BigDecimal vat = BigDecimal.ONE.add(config.get().getVatPercent().movePointLeft(2));
        List<Label> out = new ArrayList<>();
        for (var e : params.entrySet()) {
            if (!e.getKey().startsWith("n_")) continue;
            int n;
            try { n = Integer.parseInt(e.getValue().trim()); } catch (NumberFormatException ex) { continue; }
            if (n <= 0) continue;
            Product p;
            try { p = products.findById(Long.valueOf(e.getKey().substring(2))).orElse(null); } catch (NumberFormatException ex) { p = null; }
            if (p == null) continue;
            BigDecimal pr = "none".equals(price) ? null : "novat".equals(price) ? p.getSalePrice() : Money.round(p.getSalePrice().multiply(vat));
            Label l = new Label(p.getName(), p.getCode(), labels.svg(p.getCode()), pr);
            for (int i = 0; i < Math.min(n, 500); i++) out.add(l);
        }
        model.addAttribute("labels", out);
        model.addAttribute("size", LabelService.size(size));
        return "labels/print";
    }
}
