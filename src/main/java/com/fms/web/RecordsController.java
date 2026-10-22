package com.fms.web;

import com.fms.domain.OrderType;
import com.fms.repo.AppUserRepository;
import com.fms.service.OrderException;
import com.fms.service.OrderService;
import com.fms.service.RecordService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Sharing records with other users (view only), and handing a user's records over to another. */
@Controller
public class RecordsController {

    private final RecordService records;
    private final OrderService orders;
    private final AppUserRepository users;
    private final CurrentUser currentUser;

    public RecordsController(RecordService records, OrderService orders, AppUserRepository users, CurrentUser currentUser) {
        this.records = records; this.orders = orders; this.users = users; this.currentUser = currentUser;
    }

    @GetMapping("/account/sharing")
    public String sharing(Principal principal, Model model) {
        var me = currentUser.of(principal);
        model.addAttribute("mine", records.sharedBy(me.getUsername()));
        model.addAttribute("withMe", records.sharedWith(me.getUsername()));
        model.addAttribute("others", users.findAll().stream().map(u -> u.getUsername()).filter(n -> !n.equals(me.getUsername())).sorted().toList());
        if (me.isAdmin()) model.addAttribute("everything", records.all());
        model.addAttribute("seesAll", me.isSeesAll());
        return "account/sharing";
    }

    /** Share all my records with a user. */
    @PostMapping("/account/sharing")
    public String shareAll(@RequestParam(required = false) String grantee, @RequestParam(required = false) String note,
                           Principal principal, RedirectAttributes redirect) {
        var me = currentUser.of(principal);
        try {
            records.share(me, me.getUsername(), grantee, null, null, note);
            redirect.addFlashAttribute("message", "Shared.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account/sharing";
    }

    /** Share one sale, purchase or offer (its creator or an administrator). */
    @PostMapping("/records/share")
    public String shareOne(@RequestParam String type, @RequestParam String code, @RequestParam(required = false) String grantee,
                           @RequestParam(required = false) String note, Principal principal, RedirectAttributes redirect) {
        var me = currentUser.of(principal);
        OrderType t = OrderType.fromPath(type);
        var o = orders.load(t, code);
        try {
            if (o.isEmpty()) throw new OrderException("Share not found.");
            records.share(me, o.createdBy(), grantee, t, code, note);
            redirect.addFlashAttribute("message", "Shared.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/" + t.getPath() + "/" + code;
    }

    @PostMapping("/records/share/{id:\\d+}/revoke")
    public String revoke(@PathVariable Long id, @RequestParam(required = false) String back, Principal principal, RedirectAttributes redirect) {
        try {
            records.revoke(currentUser.of(principal), id);
            redirect.addFlashAttribute("message", "Sharing ended.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + (back != null && back.startsWith("/") && !back.startsWith("//") ? back : "/account/sharing");
    }

    /** Administrators: hand all of a user's records to another user. */
    @PostMapping("/admin/users/{id:\\d+}/hand-over")
    public String handOver(@PathVariable Long id, @RequestParam(required = false) String to, Principal principal, RedirectAttributes redirect) {
        var from = users.findById(id).orElse(null);
        try {
            if (from == null) throw new OrderException("Choose another user to hand the records to.");
            int n = records.handOver(currentUser.of(principal), from.getUsername(), to);
            redirect.addFlashAttribute("message", "Records handed over: " + n + " lines.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }
}
