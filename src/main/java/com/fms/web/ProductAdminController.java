package com.fms.web;

import com.fms.domain.NotFoundException;
import com.fms.domain.Product;
import com.fms.repo.ProductClassRepository;
import com.fms.repo.ProductRepository;
import com.fms.service.PricingService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** ProductRegistration (not among the uploaded files; rebuilt from the Products columns). */
@Controller
public class ProductAdminController {

    private final ProductRepository products;
    private final ProductClassRepository classes;
    private final PricingService pricing;
    private final com.fms.service.AuditService audit;
    private final com.fms.service.ShelfService shelves;

    public ProductAdminController(ProductRepository products, ProductClassRepository classes, PricingService pricing,
                                  com.fms.service.AuditService audit, com.fms.service.ShelfService shelves) {
        this.shelves = shelves;
        this.products = products;
        this.classes = classes;
        this.pricing = pricing;
        this.audit = audit;
    }

    @GetMapping("/admin/products")
    public String list(Model model) {
        var all = products.findAllByOrderByNameAsc();
        Map<Long, Long> stock = new HashMap<>();
        all.forEach(p -> stock.put(p.getId(), pricing.available(p.getName())));
        model.addAttribute("items", all);
        model.addAttribute("stock", stock);
        model.addAttribute("shelfSummary", shelves.summaryByProduct());
        return "admin/products";
    }

    @GetMapping("/admin/products/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ProductForm());
        model.addAttribute("itemId", null);
        model.addAttribute("classes", classes.findAllByOrderByNameAsc());
        return "admin/product-form";
    }

    @GetMapping("/admin/products/{id:\\d+}")
    public String editForm(@PathVariable Long id, Model model) {
        Product p = products.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
        model.addAttribute("form", ProductForm.of(p));
        model.addAttribute("itemId", id);
        model.addAttribute("classes", classes.findAllByOrderByNameAsc());
        return "admin/product-form";
    }

    @PostMapping({"/admin/products/new", "/admin/products/{id:\\d+}"})
    public String save(@PathVariable(required = false) Long id,
                       @Valid @ModelAttribute("form") ProductForm form, BindingResult errors,
                       Model model, RedirectAttributes redirect) {
        boolean duplicate = id == null ? products.existsByName(form.getName())
                : products.existsByNameAndIdNot(form.getName(), id);
        if (duplicate) {
            errors.rejectValue("name", "duplicate", "A product with this name already exists.");
        }
        if (form.getCode() != null && (id == null ? products.existsByCode(form.getCode())
                : products.existsByCodeAndIdNot(form.getCode(), id))) {
            errors.rejectValue("code", "duplicate", "Another product already uses this code.");
        }
        if (errors.hasErrors()) {
            model.addAttribute("itemId", id);
            model.addAttribute("classes", classes.findAllByOrderByNameAsc());
            return "admin/product-form";
        }
        Product p = id == null ? new Product()
                : products.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
        p.setName(form.getName());
        p.setUnit(form.getUnit());
        p.setBuyPrice(form.getBuyPrice());
        p.setSalePrice(form.getSalePrice());
        p.setQuantityLimit(form.getQuantityLimit());
        p.setProductClass(form.getClassId() == null ? null : classes.findById(form.getClassId()).orElse(null));
        p.setCode(form.getCode());
        products.save(p);
        audit.log(id == null ? "CREATE" : "UPDATE", "Product", p.getName(),
                "buy " + p.getBuyPrice() + ", sale " + p.getSalePrice() + ", limit " + p.getQuantityLimit()
                + (p.getCode() == null ? "" : ", code " + p.getCode()));
        redirect.addFlashAttribute("message", "Product saved.");
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/products/{id:\\d+}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        products.findById(id).ifPresent(p -> audit.log("DELETE", "Product", p.getName(), null));
        products.deleteById(id);
        redirect.addFlashAttribute("message", "Product deleted. Existing orders keep the name they were saved with.");
        return "redirect:/admin/products";
    }
}
