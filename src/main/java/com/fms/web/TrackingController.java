package com.fms.web;

import com.fms.service.LocationService;
import com.fms.service.TrackingService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** Expiry report, serial number lookup, and the batches a sale can choose from. Tracking module. */
@Controller
public class TrackingController {

    private final TrackingService tracking;
    private final LocationService locations;
    private final CurrentUser currentUser;

    public TrackingController(TrackingService tracking, LocationService locations, CurrentUser currentUser) {
        this.tracking = tracking;
        this.locations = locations;
        this.currentUser = currentUser;
    }

    @GetMapping("/tracking/expiry")
    public String expiry(@RequestParam(defaultValue = "30") int days, @RequestParam(required = false) Long location, Model model) {
        model.addAttribute("rows", tracking.expiring(Math.max(0, Math.min(3650, days)), location));
        model.addAttribute("days", days);
        model.addAttribute("location", location);
        return "tracking/expiry";
    }

    @GetMapping("/tracking/serials")
    public String serials(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("results", tracking.search(q));
        model.addAttribute("q", q);
        return "tracking/serials";
    }

    /** Batches of a product with stock at the order's location (for the sale form). */
    @GetMapping("/api/batches")
    @ResponseBody
    public List<Map<String, Object>> batches(@RequestParam String product, @RequestParam(required = false) Long location, Principal principal) {
        if (!tracking.enabled()) return List.of();
        Long loc = location != null ? location : currentUser.of(principal).getLocationId();
        return tracking.batches(product, loc).stream().map(b -> Map.<String, Object>of(
                "batch", b.batchNo(), "expiry", b.expiry() == null ? "" : b.expiry().toString(),
                "quantity", b.quantity(), "expired", b.isExpired())).toList();
    }
}
