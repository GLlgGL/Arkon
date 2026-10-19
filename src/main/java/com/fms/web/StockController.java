package com.fms.web;

import com.fms.domain.StockAdjustment;
import com.fms.repo.ProductRepository;
import com.fms.service.OrderException;
import com.fms.service.StockService;
import java.security.Principal;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Returns, damage, losses and stock-count corrections. Administrator-only. */
@Controller
public class StockController {

    private final StockService stock;
    private final ProductRepository products;
    private final CurrentUser currentUser;
    private final com.fms.service.ShelfService shelves;

    public StockController(StockService stock, ProductRepository products, CurrentUser currentUser,
                           com.fms.service.ShelfService shelves) {
        this.shelves = shelves;
        this.stock = stock;
        this.products = products;
        this.currentUser = currentUser;
    }

    @GetMapping("/stock")
    public String page(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("page", stock.search(q, PageRequest.of(Math.max(page, 0), 50)));
        model.addAttribute("products", products.findAllByOrderByNameAsc());
        model.addAttribute("reasons", StockAdjustment.Reason.values());
        model.addAttribute("shelves", shelves.all());
        return "stock";
    }

    @PostMapping("/stock")
    public String add(@RequestParam String product, @RequestParam StockAdjustment.Reason reason,
                      @RequestParam(defaultValue = "0") int quantity, @RequestParam(required = false) String refNo,
                      @RequestParam(required = false) String note, @RequestParam(required = false) Long shelfId,
                      Principal principal, RedirectAttributes redirect) {
        try {
            var a = stock.add(product, reason, quantity, refNo, note, shelfId, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Saved: " + (a.getQuantity() > 0 ? "+" : "") + a.getQuantity()
                    + " " + a.getProductName() + " (" + reason.getLabel() + ").");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock";
    }

    @PostMapping("/stock/{id:\\d+}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            stock.delete(id);
            redirect.addFlashAttribute("message", "Adjustment undone.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock";
    }
}
