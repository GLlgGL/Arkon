package com.fms.web;

import com.fms.service.*;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Production (prodhim): bills of materials and production orders. Administrator-only; optional module. */
@Controller
public class ProductionController {

    private final ProductionService production;
    private final com.fms.repo.ProductRepository products;
    private final CurrentUser currentUser;

    public ProductionController(ProductionService production, com.fms.repo.ProductRepository products, CurrentUser currentUser) {
        this.production = production;
        this.products = products;
        this.currentUser = currentUser;
    }

    @GetMapping("/production")
    public String orders(Model model) {
        model.addAttribute("orders", production.all());
        model.addAttribute("bomProducts", production.productsWithBom());
        model.addAttribute("today", LocalDate.now());
        return "production/orders";
    }

    @PostMapping("/production")
    public String create(@RequestParam(required = false) Long productId, @RequestParam(defaultValue = "1") int quantity,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate plannedOn, @RequestParam(required = false) String note, Principal principal, RedirectAttributes redirect) {
        try {
            var o = production.create(productId, quantity, plannedOn, note, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Production order created.");
            return "redirect:/production/" + o.getCode();
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/production";
        }
    }

    @GetMapping("/production/{code:PR-\\d+}")
    public String order(@PathVariable String code, Model model) {
        var o = production.get(code);
        model.addAttribute("o", o);
        model.addAttribute("lines", production.lines(o.getId()));
        return "production/order";
    }

    @PostMapping("/production/{code:PR-\\d+}/complete")
    public String complete(@PathVariable String code, Principal principal, RedirectAttributes redirect) {
        try {
            production.complete(code, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Production completed: stock updated.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/production/" + code + "";
    }

    @PostMapping("/production/{code:PR-\\d+}/reverse")
    public String reverse(@PathVariable String code, RedirectAttributes redirect) {
        try {
            production.reverse(code);
            redirect.addFlashAttribute("message", "Production reversed.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/production/" + code + "";
    }

    @PostMapping("/production/{code:PR-\\d+}/cancel")
    public String cancel(@PathVariable String code, RedirectAttributes redirect) {
        try {
            production.cancel(code);
            redirect.addFlashAttribute("message", "Production order cancelled.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/production/" + code + "";
    }

    @GetMapping("/production/bom")
    public String bom(@RequestParam(required = false) Long product, Model model) {
        model.addAttribute("products", products.findAllByOrderByNameAsc());
        model.addAttribute("product", product == null ? null : products.findById(product).orElse(null));
        if (product != null) {
            model.addAttribute("bom", production.bom(product));
            model.addAttribute("unitCost", production.unitCost(product));
        }
        return "production/bom";
    }

    @PostMapping("/production/bom")
    public String addComponent(@RequestParam Long product, @RequestParam(required = false) Long component,
                               @RequestParam(defaultValue = "1") int quantity, RedirectAttributes redirect) {
        try {
            production.addComponent(product, component, quantity);
            redirect.addFlashAttribute("message", "Bill of materials saved.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/production/bom?product=" + product + "";
    }

    @PostMapping("/production/bom/{id:\\d+}/delete")
    public String removeComponent(@PathVariable Long id, @RequestParam Long product, RedirectAttributes redirect) {
        production.removeComponent(id);
        redirect.addFlashAttribute("message", "Component removed.");
        return "redirect:/production/bom?product=" + product;
    }
}
