package com.fms.web;

import com.fms.domain.AppUser;
import com.fms.domain.NotFoundException;
import com.fms.domain.OrderType;
import com.fms.repo.ProductRepository;
import com.fms.service.AddLineForm;
import com.fms.service.ExcelService;
import com.fms.service.InvoiceService;
import com.fms.service.OrderException;
import com.fms.service.OrderService;
import com.fms.service.OrderView;
import com.fms.service.PartyService;
import com.fms.service.Period;
import com.fms.service.ReportService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Purchases, sales and offers. One controller and two templates replace
 *   ListOfPruchases / AddNewPruchase / ListOfPurchaseUpdate,
 *   ListOfSales / NewSale / ListofSalesUpdate,
 *   ListOfOfferings / NewOffer / ListOfOfferingsUpdate.
 *
 * URLs: /purchases, /sales, /offers                list
 *       /{type}/new                                start a new order
 *       /{type}/{code}                             view / edit an order
 */
@Controller
public class OrderController {

    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.service.AccountingService accountingService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.repo.OrderInfoRepository orderInfoRepo;
    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.repo.ClientRepository clientRepo;

    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.service.CurrencyService fxService;

    private static final int[] PAGE_SIZES = {10, 25, 50, 100};

    private final OrderService orders;
    private final PartyService parties;
    private final ProductRepository products;
    private final InvoiceService invoices;
    private final ReportService reports;
    private final ExcelService excel;
    private final CurrentUser currentUser;

    private final com.fms.service.PaymentService payments;
    private final com.fms.repo.OrderInfoRepository infos;
    private final com.fms.service.AuditService audit;
    private final com.fms.service.ShelfService shelfService;
    private final com.fms.service.MailService mailService;
    private final com.fms.service.CashService cashService;
    private final com.fms.service.CostCenterService costCenterService;
    private final com.fms.service.ConfigService configService;
    private final com.fms.service.PickListPdf pickListPdf = new com.fms.service.PickListPdf();

    public OrderController(OrderService orders, PartyService parties, ProductRepository products,
                           InvoiceService invoices, ReportService reports, ExcelService excel,
                           CurrentUser currentUser, com.fms.service.PaymentService payments,
                           com.fms.repo.OrderInfoRepository infos, com.fms.service.AuditService audit,
                           com.fms.service.ShelfService shelfService, com.fms.service.MailService mailService,
                           com.fms.service.CashService cashService, com.fms.service.CostCenterService costCenterService,
                           com.fms.service.ConfigService configService) {
        this.cashService = cashService;
        this.costCenterService = costCenterService;
        this.configService = configService;
        this.shelfService = shelfService;
        this.mailService = mailService;
        this.payments = payments;
        this.infos = infos;
        this.audit = audit;
        this.orders = orders;
        this.parties = parties;
        this.products = products;
        this.invoices = invoices;
        this.reports = reports;
        this.excel = excel;
        this.currentUser = currentUser;
    }

    @GetMapping("/{type:purchases|sales|offers}")
    public String list(@PathVariable String type,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "25") int size,
                       Principal principal, Model model) {
        OrderType t = OrderType.fromPath(type);
        AppUser user = currentUser.of(principal);
        int pageSize = clampPageSize(size);
        var result = orders.list(t, user, q, from, to, PageRequest.of(Math.max(page, 0), pageSize));
        model.addAttribute("type", t);
        model.addAttribute("page", result);
        model.addAttribute("totals", orders.listTotals(t, user, q, from, to));
        // Payment status (sales, purchases) or offer status (offers) for the rows on this page.
        var codes = result.getContent().stream().map(com.fms.repo.OrderSummary::orderCode).toList();
        if (com.fms.service.PaymentService.hasPayments(t)) {
            java.util.Map<String, java.math.BigDecimal> totalsByCode = new java.util.HashMap<>();
            java.util.Map<String, LocalDate> datesByCode = new java.util.HashMap<>();
            result.getContent().forEach(o -> {
                totalsByCode.put(o.orderCode(), o.valueWithVat());
                datesByCode.put(o.orderCode(), o.createdAt().toLocalDate());
            });
            model.addAttribute("payStatus", payments.statusFor(t, totalsByCode, datesByCode));
        } else if (!codes.isEmpty()) {
            java.util.Map<String, String> offerStatus = new java.util.HashMap<>();
            infos.findByOrderTypeAndOrderCodeIn(t, codes).forEach(i -> offerStatus.put(i.getOrderCode(), i.getOfferStatusLabel()));
            model.addAttribute("offerStatus", offerStatus);
        }
        model.addAttribute("q", q);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("size", pageSize);
        model.addAttribute("pageSizes", PAGE_SIZES);
        return "orders/list";
    }

    /** Excel export of the lines behind the current list filter (dates only). */
    @GetMapping("/{type:purchases|sales|offers}/export")
    public ResponseEntity<byte[]> export(@PathVariable String type,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         Principal principal) {
        OrderType t = OrderType.fromPath(type);
        Period p = Period.custom(from == null ? LocalDate.of(1900, 1, 1) : from, to == null ? LocalDate.now() : to);
        byte[] file = excel.orderLines(t.getPluralLabel(), reports.lines(t, currentUser.of(principal), p));
        return xlsx(file, t.getPluralLabel() + ".xlsx");
    }

    /** New order page. It has no number until the first product is added. */
    @GetMapping("/{type:purchases|sales|offers}/new")
    public String startNew(@PathVariable String type, Model model) {
        OrderType t = OrderType.fromPath(type);
        fillEditModel(t, orders.newOrder(t), model);
        return "orders/edit";
    }

    @PostMapping("/{type:purchases|sales|offers}/new/lines")
    public String addFirstLine(@PathVariable String type, @ModelAttribute("form") AddLineForm form,
                               Principal principal, RedirectAttributes redirect) {
        OrderType t = OrderType.fromPath(type);
        try {
            var line = orders.addLine(t, null, form, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Product added. This " + t.getLabel().toLowerCase()
                    + " is number " + line.getOrderCode() + ".");
            return "redirect:/" + t.getPath() + "/" + line.getOrderCode();
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addFlashAttribute("form", form);
            return "redirect:/" + t.getPath() + "/new";
        }
    }

    private void fillEditModel(OrderType t, OrderView order, Model model) {
        model.addAttribute("fx", fxService.ofOrder(t, order.isEmpty() ? null : order.code()));
        model.addAttribute("fxCurrencies", com.fms.service.CurrencyService.FOREIGN);
        if (t != OrderType.OFFER && !order.isEmpty() && configService.get().isModAccounting()) {   // accounting entries of this document
            model.addAttribute("accEntries", accountingService.documentEntries(t, order.code()));
        }
        java.util.Map<String, java.math.BigDecimal> fxToday = new java.util.LinkedHashMap<>();
        for (String c : com.fms.service.CurrencyService.FOREIGN) {
            var r = fxService.rateOn(c, java.time.LocalDate.now());
            if (r != null) fxToday.put(c, r);
        }
        model.addAttribute("fxToday", fxToday);
        if (t != OrderType.PURCHASE && !order.isEmpty()) {         // VAT of this sale or offer
            var vi = orderInfoRepo.findByOrderTypeAndOrderCode(t, order.code()).orElse(null);
            var client = clientRepo.findByName(order.lines().get(0).getBuyerName()).orElse(null);
            model.addAttribute("clientVat", client == null ? null : client.getVatRate());
            model.addAttribute("orderVat", vi != null && vi.getVatRate() != null ? vi.getVatRate() : null);
            model.addAttribute("orderVatNote", vi == null ? null : vi.getVatNote());
        }
        var fxNow = fxService.ofOrder(t, order.isEmpty() ? null : order.code());
        if (fxNow.foreign()) {
            var today = fxService.rateOn(fxNow.code(), java.time.LocalDate.now());
            model.addAttribute("payRateToday", today != null ? today : fxNow.rate());
        }
        model.addAttribute("type", t);
        model.addAttribute("order", order);
        model.addAttribute("providers", parties.providerNames(t));
        model.addAttribute("buyers", parties.buyerNames(t));
        var productList = products.findAllByOrderByNameAsc();
        model.addAttribute("products", productList);
        java.util.Map<String, String> codes = new java.util.HashMap<>();
        productList.forEach(p -> { if (p.getCode() != null) codes.put(p.getName(), p.getCode()); });
        model.addAttribute("productCodes", codes);
        model.addAttribute("allShelves", shelfService.all());
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new AddLineForm());
        }
        if (!order.isEmpty()) {
            var info = orders.info(t, order.code(), order.lines().get(0).getCreatedAt().toLocalDate());
            model.addAttribute("info", info);
            if (com.fms.service.PaymentService.hasPayments(t)) {
                model.addAttribute("balance", payments.balance(order, info));
                model.addAttribute("payments", payments.list(t, order.code()));
                model.addAttribute("payMethods", com.fms.domain.Payment.Method.values());
                model.addAttribute("cashAccounts", cashService.activeAccounts());
                java.util.Map<Long, String> accountNames = new java.util.HashMap<>();
                cashService.allAccounts().forEach(a -> accountNames.put(a.getId(), a.getName()));
                model.addAttribute("accountNames", accountNames);
            }
            model.addAttribute("today", LocalDate.now());
            model.addAttribute("mailReady", mailService.isReady());
            if ((t == OrderType.SALE || t == OrderType.PURCHASE) && configService.get().isModCostCenters()) {
                model.addAttribute("costCenters", costCenterService.active());
            }
            if (t == OrderType.SALE || t == OrderType.PURCHASE) {
                model.addAttribute("returnable", orders.returnable(order));
                model.addAttribute("returnReasons", com.fms.domain.ReturnReason.values());
            }
        }
    }

    @GetMapping("/{type:purchases|sales|offers}/{code:\\d+}")
    public String edit(@PathVariable String type, @PathVariable String code, Principal principal, Model model) {
        OrderType t = OrderType.fromPath(type);
        AppUser user = currentUser.of(principal);
        OrderView order = orders.load(t, code);
        if (!orders.canView(user, order)) {
            // Regular users only see their own orders, as in the original lists.
            throw new NotFoundException("Order not found");
        }
        fillEditModel(t, order, model);
        return "orders/edit";
    }

    @PostMapping("/{type:purchases|sales|offers}/{code:\\d+}/lines")
    public String addLine(@PathVariable String type, @PathVariable String code,
                          @ModelAttribute("form") AddLineForm form,
                          Principal principal, RedirectAttributes redirect) {
        OrderType t = OrderType.fromPath(type);
        try {
            orders.addLine(t, code, form, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Product added.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addFlashAttribute("form", form);
        }
        return "redirect:/" + t.getPath() + "/" + code;
    }

    @PostMapping("/{type:purchases|sales|offers}/{code:\\d+}/lines/{lineId}/delete")
    public String deleteLine(@PathVariable String type, @PathVariable String code, @PathVariable Long lineId,
                             Principal principal, RedirectAttributes redirect) {
        OrderType t = OrderType.fromPath(type);
        try {
            orders.deleteLine(t, code, lineId, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Line removed.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/" + t.getPath() + "/" + code;
    }

    @PostMapping("/{type:purchases|sales|offers}/{code:\\d+}/delete")
    public String deleteOrder(@PathVariable String type, @PathVariable String code,
                              Principal principal, RedirectAttributes redirect) {
        OrderType t = OrderType.fromPath(type);
        orders.deleteOrder(t, code, currentUser.of(principal));
        redirect.addFlashAttribute("message", t.getLabel() + " " + code + " deleted.");
        return "redirect:/" + t.getPath();
    }

    /** Creates (or recreates) the invoice PDF and opens it. */
    @PostMapping("/{type:purchases|sales|offers}/{code:\\d+}/invoice")
    public String invoice(@PathVariable String type, @PathVariable String code,
                          Principal principal, RedirectAttributes redirect) {
        OrderType t = OrderType.fromPath(type);
        AppUser user = currentUser.of(principal);
        if (t == OrderType.PURCHASE && !user.isAdmin()) {
            // Regular users had no access to purchase invoices in the original.
            redirect.addFlashAttribute("error", "Only administrators can create purchase invoices.");
            return "redirect:/" + t.getPath() + "/" + code;
        }
        if (!orders.canEdit(user, orders.load(t, code))) {
            throw new NotFoundException("Order not found");
        }
        try {
            String file = invoices.generate(t, code);
            audit.log("CREATE_INVOICE", t.getLabel(), code, file);
            redirect.addFlashAttribute("message", "Invoice created.");
            redirect.addFlashAttribute("invoiceUrl",
                    "/invoices/" + t.getPath() + "/file?name=" + URLEncoder.encode(file, StandardCharsets.UTF_8));
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/" + t.getPath() + "/" + code;
    }

    static int clampPageSize(int size) {
        for (int s : PAGE_SIZES) {
            if (s == size) {
                return s;
            }
        }
        return 25;
    }

    static ResponseEntity<byte[]> xlsx(byte[] file, String name) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    // ------------------------------------------------------------ pick list

    /** Printable pick list: what to take from which shelf for a sale. */
    @GetMapping("/sales/{code:\\d+}/picklist")
    public String pickList(@PathVariable String code, Principal principal, Model model) {
        OrderView order = orders.load(OrderType.SALE, code);
        if (order.isEmpty() || !orders.canView(currentUser.of(principal), order)) {
            throw new NotFoundException("Order not found");
        }
        model.addAttribute("order", order);
        model.addAttribute("productCodes", codesFor(order));
        return "orders/picklist";
    }

    @GetMapping("/sales/{code:\\d+}/picklist.pdf")
    public ResponseEntity<byte[]> pickListPdf(@PathVariable String code, Principal principal) {
        OrderView order = orders.load(OrderType.SALE, code);
        if (order.isEmpty() || !orders.canView(currentUser.of(principal), order)) {
            throw new NotFoundException("Order not found");
        }
        var codes = codesFor(order);
        var rows = order.lines().stream().map(l -> new com.fms.service.PickListPdf.Row(codes.get(l.getProductName()),
                l.getProductName(), l.getQuantity(), l.getUnit(), l.getShelfCode())).toList();
        var data = new com.fms.service.PickListPdf.PickList(code, order.lines().get(0).getCreatedAt(),
                order.buyer().name(), order.provider().name(), currentUser.of(principal).getFullName(), rows,
                shelfService.enabled(), invoices.pdfLabels());
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        pickListPdf.write(data, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"picklist-" + code + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }

    private java.util.Map<String, String> codesFor(OrderView order) {
        java.util.Map<String, String> codes = new java.util.HashMap<>();
        for (var l : order.lines()) {
            products.findByName(l.getProductName()).filter(p -> p.getCode() != null)
                    .ifPresent(p -> codes.put(l.getProductName(), p.getCode()));
        }
        return codes;
    }
}
