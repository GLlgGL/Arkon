package com.fms.web;

import com.fms.service.LocationService;
import com.fms.service.OrderException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Shops and warehouses, stock per location, transfers. Locations module; access through Stock. */
@Controller
public class LocationController {

    private final LocationService locations;
    private final com.fms.repo.ProductRepository products;
    private final CurrentUser currentUser;

    public LocationController(LocationService locations, com.fms.repo.ProductRepository products, CurrentUser currentUser) {
        this.locations = locations;
        this.products = products;
        this.currentUser = currentUser;
    }

    @GetMapping("/locations")
    public String list(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("locs", locations.all());
        model.addAttribute("editing", edit == null ? null : locations.get(edit));
        return "locations/list";
    }

    @PostMapping("/locations")
    public String save(@RequestParam(required = false) Long id, @RequestParam String code, @RequestParam String name,
                       @RequestParam(defaultValue = "SHOP") String kind, @RequestParam(required = false) String address,
                       @RequestParam(defaultValue = "false") boolean active, RedirectAttributes redirect) {
        try {
            locations.save(id, code, name, kind, address, active);
            redirect.addFlashAttribute("message", "Location saved.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/locations";
    }

    @GetMapping("/locations/stock")
    public String stock(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("locs", locations.active());
        model.addAttribute("rows", locations.stockTable(q));
        model.addAttribute("q", q);
        return "locations/stock";
    }

    @GetMapping("/locations/transfers")
    public String transfers(Model model) {
        model.addAttribute("transfers", locations.transfers());
        model.addAttribute("locs", locations.active());
        model.addAttribute("products", products.findAllByOrderByNameAsc());
        model.addAttribute("today", LocalDate.now());
        return "locations/transfers";
    }

    @PostMapping("/locations/transfers")
    public String transfer(@RequestParam(required = false) Long fromId, @RequestParam(required = false) Long toId,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(required = false) String note,
                           @RequestParam(name = "product", required = false) List<String> product,
                           @RequestParam(name = "qty", required = false) List<String> qty,
                           Principal principal, RedirectAttributes redirect) {
        List<LocationService.TransferLine> items = new ArrayList<>();
        if (product != null) {
            for (int i = 0; i < product.size(); i++) {
                int q = 0;
                try { q = qty == null || i >= qty.size() || qty.get(i).isBlank() ? 0 : Integer.parseInt(qty.get(i).trim()); } catch (NumberFormatException ignored) { }
                items.add(new LocationService.TransferLine(product.get(i), q));
            }
        }
        try {
            var t = locations.transfer(fromId, toId, date, note, items, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Transfer " + t.getCode() + " saved.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/locations/transfers";
    }

    @PostMapping("/locations/transfers/{code:TR-\\d+}/undo")
    public String undo(@PathVariable String code, RedirectAttributes redirect) {
        try {
            locations.undo(code);
            redirect.addFlashAttribute("message", "Transfer undone.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/locations/transfers";
    }
}
