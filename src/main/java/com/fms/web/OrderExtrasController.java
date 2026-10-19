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
 * payments, due date, offer status and offer to sale.
 */
@Controller
public class OrderExtrasController {

    private final OrderService orders;
    private final PaymentService payments;
    private final CurrentUser currentUser;

    public OrderExtrasController(OrderService orders, PaymentService payments, CurrentUser currentUser) {
        this.orders = orders;
        this.payments = payments;
        this.currentUser = currentUser;
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
                             Principal principal, RedirectAttributes redirect) {
        OrderType t = OrderType.fromPath(type);
        AppUser user = currentUser.of(principal);
        try {
            payments.add(ownOrder(t, code, user), paidOn, amount, method, note, user);
            redirect.addFlashAttribute("message", "Payment recorded.");
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

}
