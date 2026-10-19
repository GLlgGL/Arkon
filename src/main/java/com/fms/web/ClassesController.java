package com.fms.web;

import com.fms.domain.ClientClassDiscount;
import com.fms.domain.NotFoundException;
import com.fms.domain.Product;
import com.fms.domain.ProductClass;
import com.fms.repo.ClientClassDiscountRepository;
import com.fms.repo.ClientRepository;
import com.fms.repo.ProductClassRepository;
import com.fms.repo.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * ProductClasses page: product classes and the discount each client gets per class.
 *
 * The original stored a class's products as a comma-separated text and added
 * or dropped a CClass column (ALTER TABLE, sp_rename) every time a class was
 * created, renamed or deleted. Here a product points to its class and each
 * client discount is a row.
 */
@Controller
public class ClassesController {

    private final ProductClassRepository classes;
    private final ProductRepository products;
    private final ClientRepository clients;
    private final ClientClassDiscountRepository discounts;

    public ClassesController(ProductClassRepository classes, ProductRepository products,
                             ClientRepository clients, ClientClassDiscountRepository discounts) {
        this.classes = classes;
        this.products = products;
        this.clients = clients;
        this.discounts = discounts;
    }

    @GetMapping("/classes")
    public String page(@RequestParam(required = false) Long edit, Model model) {
        List<ProductClass> all = classes.findAllByOrderByNameAsc();
        model.addAttribute("classes", all);
        model.addAttribute("products", products.findAllByOrderByNameAsc());
        model.addAttribute("clients", clients.findAllByOrderByNameAsc());
        model.addAttribute("discounts", discounts.findAllSorted());
        model.addAttribute("editing", edit == null ? null : classes.findById(edit).orElse(null));
        return "classes";
    }

    @PostMapping("/classes/save")
    @Transactional
    public String saveClass(@RequestParam(required = false) Long id,
                            @RequestParam String name,
                            @RequestParam(required = false) Set<Long> productIds,
                            RedirectAttributes redirect) {
        String trimmed = name == null ? "" : name.trim();
        if (!StringUtils.hasText(trimmed)) {
            redirect.addFlashAttribute("error", "Enter a class name.");
            return "redirect:/classes";
        }
        boolean duplicate = id == null ? classes.existsByName(trimmed) : classes.existsByNameAndIdNot(trimmed, id);
        if (duplicate) {
            redirect.addFlashAttribute("error", "A class called " + trimmed + " already exists.");
            return "redirect:/classes" + (id == null ? "" : "?edit=" + id);
        }
        ProductClass pc = id == null ? new ProductClass()
                : classes.findById(id).orElseThrow(() -> new NotFoundException("Class not found"));
        pc.setName(trimmed);
        classes.save(pc);

        // A product belongs to at most one class; ticking it here moves it into this class.
        Set<Long> chosen = productIds == null ? Set.of() : productIds;
        for (Product p : products.findAll()) {
            boolean inThis = p.getProductClass() != null && p.getProductClass().getId().equals(pc.getId());
            if (chosen.contains(p.getId()) && !inThis) {
                p.setProductClass(pc);
            } else if (!chosen.contains(p.getId()) && inThis) {
                p.setProductClass(null);
            }
        }
        redirect.addFlashAttribute("message", "Class " + trimmed + " saved.");
        return "redirect:/classes";
    }

    @PostMapping("/classes/{id:\\d+}/delete")
    @Transactional
    public String deleteClass(@PathVariable Long id, RedirectAttributes redirect) {
        ProductClass pc = classes.findById(id).orElseThrow(() -> new NotFoundException("Class not found"));
        products.findByProductClassOrderByNameAsc(pc).forEach(p -> p.setProductClass(null));
        classes.delete(pc);
        redirect.addFlashAttribute("message", "Class deleted. Its client discounts were removed too.");
        return "redirect:/classes";
    }

    /** Adds or updates one client's discount for one class. */
    @PostMapping("/classes/discounts")
    @Transactional
    public String saveDiscount(@RequestParam Long clientId, @RequestParam Long classId,
                               @RequestParam BigDecimal percent, RedirectAttributes redirect) {
        if (percent.signum() < 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0) {
            redirect.addFlashAttribute("error", "The discount must be between 0 and 100 percent.");
            return "redirect:/classes";
        }
        var client = clients.findById(clientId).orElseThrow(() -> new NotFoundException("Client not found"));
        var pc = classes.findById(classId).orElseThrow(() -> new NotFoundException("Class not found"));
        ClientClassDiscount d = discounts.findByClientIdAndProductClassId(clientId, classId).orElseGet(ClientClassDiscount::new);
        d.setClient(client);
        d.setProductClass(pc);
        d.setDiscountPercent(percent);
        discounts.save(d);
        redirect.addFlashAttribute("message", client.getName() + " now gets " + percent.stripTrailingZeros().toPlainString()
                + "% off " + pc.getName() + ".");
        return "redirect:/classes";
    }

    @PostMapping("/classes/discounts/{id:\\d+}/delete")
    public String deleteDiscount(@PathVariable Long id, RedirectAttributes redirect) {
        discounts.deleteById(id);
        redirect.addFlashAttribute("message", "Discount removed.");
        return "redirect:/classes";
    }
}
