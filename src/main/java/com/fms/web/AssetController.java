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

/** Fixed assets (asete). Administrator-only; optional module. */
@Controller
public class AssetController {

    private final AssetService assets;
    private final CostCenterService centers;
    private final ConfigService config;

    public AssetController(AssetService assets, CostCenterService centers, ConfigService config) {
        this.assets = assets;
        this.centers = centers;
        this.config = config;
    }

    @GetMapping("/assets")
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate at, @RequestParam(required = false) Integer year, @RequestParam(required = false) Long edit, Model model) {
        LocalDate d = at == null ? LocalDate.now() : at;
        int y = year == null ? d.getYear() : year;
        var reg = assets.register(d, LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31));
        model.addAttribute("register", reg);
        model.addAttribute("at", d);
        model.addAttribute("year", y);
        model.addAttribute("editing", edit == null ? null : assets.get(edit));
        model.addAttribute("totCost", reg.stream().map(r -> r.asset().getCost()).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("totAcc", reg.stream().map(AssetService.AssetView::accumulated).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("totBook", reg.stream().map(AssetService.AssetView::bookValue).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("totYear", reg.stream().map(AssetService.AssetView::periodDepreciation).reduce(BigDecimal.ZERO, BigDecimal::add));
        if (config.get().isModCostCenters()) {
            model.addAttribute("costCenters", centers.active());
        }
        model.addAttribute("today", LocalDate.now());
        return "assets/list";
    }

    @GetMapping("/assets/{id:\\d+}")
    public String schedule(@PathVariable Long id, Model model) {
        var a = assets.get(id);
        model.addAttribute("asset", a);
        model.addAttribute("schedule", assets.schedule(a));
        model.addAttribute("today", LocalDate.now());
        return "assets/schedule";
    }

    @PostMapping("/assets")
    public String save(@RequestParam(required = false) Long id, @RequestParam String code, @RequestParam String name,
                       @RequestParam(required = false) String category, @RequestParam(required = false) String location,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate, @RequestParam(required = false) BigDecimal cost, @RequestParam(required = false) BigDecimal salvage,
                       @RequestParam(required = false) Integer lifeMonths, @RequestParam(defaultValue = "STRAIGHT") String method,
                       @RequestParam(required = false) BigDecimal rate, @RequestParam(required = false) Long costCenterId,
                       @RequestParam(required = false) String note, RedirectAttributes redirect) {
        try {
            assets.save(id, code, name, category, location, purchaseDate, cost, salvage, lifeMonths, method, rate, costCenterId, note);
            redirect.addFlashAttribute("message", "Asset saved.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/assets";
    }

    @PostMapping("/assets/{id:\\d+}/dispose")
    public String dispose(@PathVariable Long id, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam(required = false) BigDecimal value, RedirectAttributes redirect) {
        try {
            assets.dispose(id, date, value);
            redirect.addFlashAttribute("message", "Asset disposed.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/assets/" + id + "";
    }

    @PostMapping("/assets/{id:\\d+}/undispose")
    public String undispose(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            assets.undispose(id);
            redirect.addFlashAttribute("message", "Asset back in use.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/assets/" + id + "";
    }

    @PostMapping("/assets/{id:\\d+}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            assets.delete(id);
            redirect.addFlashAttribute("message", "Asset deleted.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/assets";
    }
}
