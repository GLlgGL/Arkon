package com.fms.web;

import com.fms.domain.AppUser;
import com.fms.domain.NotFoundException;
import com.fms.domain.OrderInfo;
import com.fms.domain.OrderType;
import com.fms.domain.Payment;
import com.fms.service.OrderException;
import com.fms.service.OrderService;
import com.fms.service.OrderView;
import com.fms.service.PaymentService;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Actions on a saved order besides adding and removing lines:
 * payments, due date, offer status, offer to sale, and emailing the invoice.
 */
@Controller
public class OrderExtrasController {

    private final OrderService orders;
    private final PaymentService payments;
    private final CurrentUser currentUser;
    private final com.fms.service.InvoiceService invoices;
    private final com.fms.service.StorageService storage;
    private final com.fms.service.MailService mail;
    private final com.fms.service.AuditService audit;
    private final org.springframework.context.MessageSource messages;

    public OrderExtrasController(OrderService orders, PaymentService payments, CurrentUser currentUser,
                                 com.fms.service.InvoiceService invoices, com.fms.service.StorageService storage,
                                 com.fms.service.MailService mail, com.fms.service.AuditService audit,
                                 org.springframework.context.MessageSource messages) {
        this.orders = orders;
        this.payments = payments;
        this.currentUser = currentUser;
        this.invoices = invoices;
        this.storage = storage;
        this.mail = mail;
        this.audit = audit;
        this.messages = messages;
    }

    private OrderView ownOrder(OrderType t, String code, AppUser user) {
        OrderView order = orders.load(t, code);
        if (order.isEmpty() || !orders.canEdit(user, order)) {
            throw new NotFoundException("Order not found");
        }
        return order;
    }

    private static String back(OrderType t, String code) {
        return "redirect:/" + t.getPath() + "/" + code;
    }

    // ------------------------------------------------------------ payments

    @PostMapping("/{type:purchases|sales}/{code:\\d+}/payments")
    public String addPayment(@PathVariable String type, @PathVariable String code,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidOn,
                             @RequestParam(required = false) BigDecimal amount,
                             @RequestParam(defaultValue = "CASH") Payment.Method method,
                             @RequestParam(required = false) String note,
                             @RequestParam(defaultValue = "PAYMENT") Payment.Kind kind,
                             @RequestParam(required = false) Long accountId,
                             Principal principal, RedirectAttributes redirect) {
        OrderType t = OrderType.fromPath(type);
        AppUser user = currentUser.of(principal);
        try {
            payments.add(ownOrder(t, code, user), paidOn, amount, method, note, kind, accountId, user);
            redirect.addFlashAttribute("message", kind == Payment.Kind.REFUND ? "Refund recorded." : "Payment recorded.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return back(t, code) + "#payments";
    }

    @PostMapping("/{type:purchases|sales}/{code:\\d+}/payments/{id}/delete")
    public String deletePayment(@PathVariable String type, @PathVariable String code, @PathVariable Long id,
                                Principal principal, RedirectAttributes redirect) {
        OrderType t = OrderType.fromPath(type);
        try {
            payments.delete(t, code, id, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Payment deleted.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return back(t, code) + "#payments";
    }

    @PostMapping("/{type:purchases|sales}/{code:\\d+}/due-date")
    public String dueDate(@PathVariable String type, @PathVariable String code,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                          Principal principal, RedirectAttributes redirect) {
        OrderType t = OrderType.fromPath(type);
        try {
            orders.updateDueDate(t, code, dueDate, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Due date saved.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return back(t, code) + "#payments";
    }

    // --------------------------------------------------------------- offers

    @PostMapping("/offers/{code:\\d+}/status")
    public String offerStatus(@PathVariable String code, @RequestParam OrderInfo.OfferStatus status,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validUntil,
                              Principal principal, RedirectAttributes redirect) {
        try {
            orders.updateOffer(code, status, validUntil, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Offer updated.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return back(OrderType.OFFER, code);
    }

    @PostMapping("/offers/{code:\\d+}/convert")
    public String convert(@PathVariable String code, Principal principal, RedirectAttributes redirect) {
        try {
            String sale = orders.convertOfferToSale(code, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Offer " + code + " is now sale " + sale + ", with the offered prices.");
            return "redirect:/sales/" + sale;
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return back(OrderType.OFFER, code);
        }
    }


    // ---------------------------------------------------------------- email

    /** Creates the current invoice PDF and emails it (sales and offers). */
    @PostMapping("/{type:sales|offers}/{code:\\d+}/email")
    public String email(@PathVariable String type, @PathVariable String code,
                        @RequestParam String to, @RequestParam(required = false) String message,
                        Principal principal, RedirectAttributes redirect) {
        OrderType t = OrderType.fromPath(type);
        AppUser user = currentUser.of(principal);
        try {
            OrderView order = ownOrder(t, code, user);
            String file = invoices.generate(t, code);
            var locale = org.springframework.context.i18n.LocaleContextHolder.getLocale();
            String typeName = messages.getMessage("type." + t.name() + ".label", null, t.getLabel(), locale);
            String company = order.provider().name();
            String subject = messages.getMessage("email.subject", new Object[] {typeName, code, company},
                    t.getLabel() + " invoice " + code + " from " + company, locale);
            String body = message == null || message.isBlank()
                    ? messages.getMessage("email.body", new Object[] {typeName, code, company},
                        "Hello,\n\nPlease find attached invoice " + code + ".\n\nKind regards,\n" + company, locale)
                    : message.trim();
            mail.sendInvoice(to, subject, body + "\n", storage.resolveInvoice(t, file));
            audit.log("EMAIL_INVOICE", t.getLabel(), code, "Sent to " + to.trim());
            redirect.addFlashAttribute("message", "Invoice emailed to " + to.trim() + ".");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return back(t, code);
    }

    // -------------------------------------------------------------- returns

    /**
     * A return on a sale (client brought goods back) or purchase (goods sent
     * back to the merchant): ret_<lineId> = quantity, plus reason and note.
     * Also creates the credit note / return note PDF.
     */
    @PostMapping("/{type:sales|purchases}/{code:\\d+}/return")
    public String recordReturn(@PathVariable String type, @PathVariable String code,
                               @RequestParam(required = false) Long shelfId,
                               @RequestParam(required = false) com.fms.domain.ReturnReason reason,
                               @RequestParam(required = false) String note,
                               jakarta.servlet.http.HttpServletRequest request, Principal principal,
                               RedirectAttributes redirect) {
        OrderType t = OrderType.fromPath(type);
        java.util.Map<Long, Integer> q = new java.util.LinkedHashMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (k.startsWith("ret_") && v.length > 0 && !v[0].isBlank()) {
                try {
                    q.put(Long.valueOf(k.substring(4)), Integer.parseInt(v[0].trim()));
                } catch (NumberFormatException ignored) {
                    // not a number: skip
                }
            }
        });
        try {
            int no = orders.recordReturn(t, code, q, shelfId, reason, note, currentUser.of(principal));
            String file = invoices.creditNote(t, code, no);
            redirect.addFlashAttribute("message", "Return recorded: stock and amounts updated.");
            redirect.addFlashAttribute("creditNoteFile", file);
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return back(t, code) + "#returns";
    }

    /** List of returns: Sales -> Returns, Purchases -> Returns. */
    @org.springframework.web.bind.annotation.GetMapping("/{type:sales|purchases}/returns")
    public String returns(@PathVariable String type, @RequestParam(required = false) String q,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          Principal principal, org.springframework.ui.Model model) {
        OrderType t = OrderType.fromPath(type);
        var list = orders.returns(t, currentUser.of(principal), q, from, to);
        java.util.Map<String, String> files = new java.util.HashMap<>();
        list.forEach(r -> files.put(r.number(), invoices.creditNoteFile(t, r.orderCode(), r.returnNo())));
        model.addAttribute("type", t);
        model.addAttribute("returns", list);
        model.addAttribute("files", files);
        model.addAttribute("total", list.stream().map(OrderService.ReturnView::valueWithVat).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("q", q);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "orders/returns";
    }
}
