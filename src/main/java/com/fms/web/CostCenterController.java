package com.fms.web;

import com.fms.service.CostCenterService;
import com.fms.service.OrderException;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Cost centres: the register and the result report. Administrator-only; optional module. */
@Controller
public class CostCenterController {

    private final CostCenterService centers;

    public CostCenterController(CostCenterService centers) {
        this.centers = centers;
    }

    @GetMapping("/cost-centers")
    public String list(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("centers", centers.all());
        model.addAttribute("editing", edit == null ? null : centers.get(edit));
        return "cost-centers/list";
    }

    @PostMapping("/cost-centers")
    public String save(@RequestParam(required = false) Long id, @RequestParam String code, @RequestParam String name,
                       @RequestParam(required = false) String description, @RequestParam(defaultValue = "false") boolean active,
                       RedirectAttributes redirect) {
        try {
            centers.save(id, code, name, description, active);
            redirect.addFlashAttribute("message", "Cost centre saved.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cost-centers";
    }

    @PostMapping("/cost-centers/{id:\\d+}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        centers.delete(id);
        redirect.addFlashAttribute("message", "Cost centre deleted.");
        return "redirect:/cost-centers";
    }

    @GetMapping("/cost-centers/report")
    public String report(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, Model model) {
        LocalDate f = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate t = to == null ? LocalDate.now() : to;
        if (t.isBefore(f)) {
            t = f;
        }
        model.addAttribute("report", centers.report(f, t));
        model.addAttribute("from", f);
        model.addAttribute("to", t);
        return "cost-centers/report";
    }
}
