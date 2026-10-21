package com.fms.web;

import com.fms.domain.OrderType;
import com.fms.domain.Payment;
import com.fms.repo.ProductRepository;
import com.fms.service.*;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Quick sale (POS). Optional module; needs full access to Sales to sell. */
@Controller
public class PosController {

    private final PosService pos;
    private final ProductRepository products;
    private final PricingService pricing;
    private final PartyService parties;
    private final OrderService orders;
    private final PaymentService payments;
    private final CashService cash;
    private final ConfigService config;
    private final AuditService audit;
    private final CurrentUser currentUser;

    public PosController(PosService pos, ProductRepository products, PricingService pricing, PartyService parties, OrderService orders,
                         PaymentService payments, CashService cash, ConfigService config, AuditService audit, CurrentUser currentUser) {
        this.pos = pos; this.products = products; this.pricing = pricing; this.parties = parties; this.orders = orders;
        this.payments = payments; this.cash = cash; this.config = config; this.audit = audit; this.currentUser = currentUser;
    }

    @GetMapping("/pos")
    public String page(Model model, Principal principal) {
        Long currentUserLoc = currentUser.of(principal).getLocationId();
        List<Map<String, Object>> list = new ArrayList<>();
        for (var p : products.findAllByOrderByNameAsc()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", p.getName());
            m.put("code", p.getCode() == null ? "" : p.getCode());
            m.put("unit", p.getUnit());
            m.put("price", p.getSalePrice());
            m.put("stock", pricing.availableFor(p.getName(), currentUserLoc));
            list.add(m);
        }
        model.addAttribute("products", list);
        model.addAttribute("clients", parties.buyerNames(OrderType.SALE));
        model.addAttribute("defaultClient", pos.defaultClient());
        if (config.get().isModCash()) model.addAttribute("accounts", cash.activeAccounts());
        return "pos/pos";
    }

    /** The cart, as form fields p_0 / q_0, p_1 / q_1 …; answers JSON. */
    @PostMapping("/pos/checkout")
    @ResponseBody
    public Map<String, Object> checkout(@RequestParam Map<String, String> form, Principal principal) {
        List<PosService.Item> cart = new ArrayList<>();
        for (int i = 0; form.containsKey("p_" + i); i++) {
            int q;
            try { q = Integer.parseInt(form.getOrDefault("q_" + i, "0")); } catch (NumberFormatException e) { q = 0; }
            cart.add(new PosService.Item(form.get("p_" + i), q));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            Payment.Method method = "CARD".equals(form.get("method")) ? Payment.Method.CARD : Payment.Method.CASH;
            BigDecimal received = form.get("received") == null || form.get("received").isBlank() ? null : new BigDecimal(form.get("received").trim());
            Long account = form.get("accountId") == null || form.get("accountId").isBlank() ? null : Long.valueOf(form.get("accountId"));
            var r = pos.checkout(cart, form.get("client"), method, received, account, currentUser.of(principal));
            out.put("ok", true);
            out.put("code", r.code());
            out.put("change", r.change());
        } catch (OrderException | NumberFormatException e) {
            out.put("ok", false);
            out.put("error", e instanceof OrderException ? e.getMessage() : "Check the amounts.");
        }
        return out;
    }

    /** A receipt to print (receipt printer or normal paper). */
    @GetMapping("/pos/receipt/{code:\\d+}")
    public String receipt(@PathVariable String code, @RequestParam(required = false) BigDecimal received, Model model) {
        var o = orders.load(OrderType.SALE, code);
        if (o.isEmpty()) throw new com.fms.domain.NotFoundException("Sale not found");
        model.addAttribute("o", o);
        model.addAttribute("pays", payments.list(OrderType.SALE, code));
        model.addAttribute("received", received);
        model.addAttribute("change", received == null ? null : received.subtract(o.valueWithVat()));
        return "pos/receipt";
    }

    @PostMapping("/settings/pos")
    public String settings(@RequestParam(required = false) String company, @RequestParam(required = false) String client,
                           RedirectAttributes redirect) {
        var c = config.get();
        c.setPosCompany(company);
        c.setPosClient(client);
        config.save(c);
        audit.log("UPDATE", "Settings", "Quick sale", "company " + company + ", client " + client);
        redirect.addFlashAttribute("message", "Quick sale settings saved.");
        return "redirect:/settings";
    }
}
