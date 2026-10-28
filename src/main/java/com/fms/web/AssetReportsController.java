package com.fms.web;

import com.fms.service.AssetService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Assets section: acquisitions, disposals, depreciation by month, and the assets report. */
@Controller
public class AssetReportsController {

    private final AssetService assets;

    public AssetReportsController(AssetService assets) {
        this.assets = assets;
    }

    private static LocalDate from(LocalDate f) { return f == null ? LocalDate.now().withDayOfYear(1) : f; }
    private static LocalDate to(LocalDate t) { return t == null ? LocalDate.now() : t; }

    @GetMapping("/assets/acquisitions")
    public String acquisitions(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, Model model) {
        model.addAttribute("rows", assets.acquisitions(from(from), to(to)));
        model.addAttribute("from", from(from)); model.addAttribute("to", to(to));
        return "assets/acquisitions";
    }

    @GetMapping("/assets/disposals")
    public String disposals(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, Model model) {
        model.addAttribute("rows", assets.disposals(from(from), to(to)));
        model.addAttribute("from", from(from)); model.addAttribute("to", to(to));
        return "assets/disposals";
    }

    @GetMapping("/assets/depreciation")
    public String depreciation(@RequestParam(required = false) Integer year, Model model) {
        int y = year == null ? LocalDate.now().getYear() : year;
        model.addAttribute("rows", assets.depreciation(y));
        model.addAttribute("year", y);
        return "assets/depreciation";
    }

    @GetMapping("/assets/report")
    public String report(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, Model model) {
        model.addAttribute("rows", assets.movement(from(from), to(to)));
        model.addAttribute("from", from(from)); model.addAttribute("to", to(to));
        return "assets/report";
    }
}
