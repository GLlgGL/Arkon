package com.fms.web;

import com.fms.domain.OrderType;
import com.fms.domain.PurchaseOrder;
import com.fms.service.AuditService;
import com.fms.service.InvoiceService;
import com.fms.service.MailService;
import com.fms.service.OrderException;
import com.fms.service.PartyService;
import com.fms.service.PurchaseOrderService;
import com.fms.service.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Purchase orders: list, view/edit, PDF, email, receive, cancel. Administrator-only. */
@Controller
public class PurchaseOrderController {

    private final PurchaseOrderService pos;
    private final PartyService parties;
    private final InvoiceService invoices;
    private final MailService mail;
    private final StorageService storage;
    private final AuditService audit;
    private final CurrentUser currentUser;

    public PurchaseOrderController(PurchaseOrderService pos, PartyService parties, InvoiceService invoices,
                                   MailService mail, StorageService storage, AuditService audit, CurrentUser currentUser) {
        this.pos = pos;
        this.parties = parties;
        this.invoices = invoices;
        this.mail = mail;
        this.storage = storage;
        this.audit = audit;
        this.currentUser = currentUser;
    }

    @GetMapping("/purchase-orders")
    public String list(Model model) {
        model.addAttribute("orders", pos.all());
        return "purchase-orders/list";
    }

    /** From the warehouse "For supply" list: qty_<product> fields for the ticked products. */
    @PostMapping("/purchase-orders/from-supply")
    public String fromSupply(@RequestParam String merchant, @RequestParam String company,
                             @RequestParam(name = "pick", required = false) java.util.List<String> picked,
                             HttpServletRequest request, Principal principal, RedirectAttributes redirect) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        if (picked != null) {
            for (String name : picked) {
                quantities.put(name, parse(request.getParameter("qty_" + name)));
            }
        }
        try {
            PurchaseOrder po = pos.create(merchant, company, quantities, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Purchase order " + po.getCode() + " created.");
            return "redirect:/purchase-orders/" + po.getCode();
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/reports/warehouse?mode=supply";
        }
    }

    @GetMapping("/purchase-orders/{code}")
    public String view(@PathVariable String code, Model model) {
        PurchaseOrder po = pos.get(code);
        model.addAttribute("po", po);
        model.addAttribute("merchantInfo", parties.provider(OrderType.PURCHASE, po.getMerchantName()));
        model.addAttribute("mailReady", mail.isReady());
        return "purchase-orders/view";
    }

    @PostMapping("/purchase-orders/{code}")
    public String update(@PathVariable String code, HttpServletRequest request, @RequestParam(required = false) String note,
                         RedirectAttributes redirect) {
        Map<Long, Integer> q = new LinkedHashMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (k.startsWith("line_") && v.length > 0) {
                q.put(Long.valueOf(k.substring(5)), parse(v[0]));
            }
        });
        try {
            pos.update(code, q, note);
            redirect.addFlashAttribute("message", "Purchase order saved.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/purchase-orders/" + code;
    }

    @PostMapping("/purchase-orders/{code}/receive")
    public String receive(@PathVariable String code, Principal principal, RedirectAttributes redirect) {
        try {
            String purchase = pos.receive(code, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Goods received: purchase " + purchase + " created and stock updated.");
            return "redirect:/purchases/" + purchase;
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/purchase-orders/" + code;
        }
    }

    @PostMapping("/purchase-orders/{code}/cancel")
    public String cancel(@PathVariable String code, RedirectAttributes redirect) {
        try {
            pos.cancel(code);
            redirect.addFlashAttribute("message", "Purchase order cancelled.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/purchase-orders/" + code;
    }

    @GetMapping("/purchase-orders/{code}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String code) {
        PurchaseOrder po = pos.get(code);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        invoices.purchaseOrder(po, parties.buyer(OrderType.PURCHASE, po.getCompanyName()),
                parties.provider(OrderType.PURCHASE, po.getMerchantName()), out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + code + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }

    @PostMapping("/purchase-orders/{code}/email")
    public String email(@PathVariable String code, @RequestParam String to, @RequestParam(required = false) String message,
                        RedirectAttributes redirect) {
        PurchaseOrder po = pos.get(code);
        try {
            Path dir = storage.invoiceDir(OrderType.PURCHASE);
            Path file = dir.resolve(StorageService.safeFileName(po.getMerchantName() + "_" + code + "_PurchaseOrder.pdf"));
            try (var out = Files.newOutputStream(file)) {
                invoices.purchaseOrder(po, parties.buyer(OrderType.PURCHASE, po.getCompanyName()),
                        parties.provider(OrderType.PURCHASE, po.getMerchantName()), out);
            }
            String body = message == null || message.isBlank()
                    ? "Hello,\n\nPlease find attached purchase order " + code + ".\n\nKind regards,\n" + po.getCompanyName()
                    : message.trim();
            mail.sendInvoice(to, "Purchase order " + code + " from " + po.getCompanyName(), body + "\n", file);
            audit.log("EMAIL_PO", "Purchase order", code, "Sent to " + to.trim());
            redirect.addFlashAttribute("message", "Purchase order emailed to " + to.trim() + ".");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return "redirect:/purchase-orders/" + code;
    }

    private static int parse(String v) {
        try {
            return v == null || v.isBlank() ? 0 : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
