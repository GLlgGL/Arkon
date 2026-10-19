package com.fms.web;

import com.fms.repo.ProductRepository;
import com.fms.service.OrderException;
import com.fms.service.ShelfService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Shelves page: create shelves, see what's on them, place and move stock. Administrator-only. */
@Controller
public class ShelfController {

    private final ShelfService shelves;
    private final ProductRepository products;

    public ShelfController(ShelfService shelves, ProductRepository products) {
        this.shelves = shelves;
        this.products = products;
    }

    @GetMapping("/shelves")
    public String page(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("views", shelves.overview());
        model.addAttribute("unplaced", shelves.unplaced());
        model.addAttribute("shelves", shelves.all());
        model.addAttribute("products", products.findAllByOrderByNameAsc());
        model.addAttribute("editing", edit == null ? null : shelves.all().stream().filter(s -> s.getId().equals(edit)).findFirst().orElse(null));
        return "shelves";
    }

    @PostMapping("/shelves/save")
    public String save(@RequestParam(required = false) Long id, @RequestParam String code,
                       @RequestParam(required = false) String description, RedirectAttributes redirect) {
        try {
            var s = shelves.save(id, code, description);
            redirect.addFlashAttribute("message", "Shelf " + s.getCode() + " saved.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shelves";
    }

    @PostMapping("/shelves/{id:\\d+}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        shelves.delete(id);
        redirect.addFlashAttribute("message", "Shelf deleted. Its stock is now not on a shelf.");
        return "redirect:/shelves";
    }

    /** Place stock on a shelf, move it between shelves, or take it off a shelf. Empty from/to = not on a shelf. */
    @PostMapping("/shelves/move")
    public String move(@RequestParam String product, @RequestParam(required = false) Long from,
                       @RequestParam(required = false) Long to, @RequestParam(defaultValue = "0") int quantity,
                       RedirectAttributes redirect) {
        try {
            shelves.move(product, from, to, quantity);
            redirect.addFlashAttribute("message", "Stock moved.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shelves";
    }
}
