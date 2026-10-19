package com.fms.web;

import com.fms.domain.OrderType;
import com.fms.service.StorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
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

/**
 * Saved invoice PDFs. Replaces PurchaseInvoices, SaleInvoices and OfferInvoices.
 * Purchase invoices and deleting are administrator-only (see SecurityConfig).
 */
@Controller
public class InvoiceController {

    private final StorageService storage;

    private final com.fms.service.AuditService audit;

    public InvoiceController(StorageService storage, com.fms.service.AuditService audit) {
        this.storage = storage;
        this.audit = audit;
    }

    @GetMapping("/invoices/{type:purchases|sales|offers}")
    public String list(@PathVariable String type, @RequestParam(required = false) String q, Model model) {
        OrderType t = OrderType.fromPath(type);
        model.addAttribute("type", t);
        model.addAttribute("files", storage.listInvoices(t, q));
        model.addAttribute("q", q);
        return "invoices/list";
    }

    /** Opens the PDF in the browser; add ?download to save it instead. */
    @GetMapping("/invoices/{type:purchases|sales|offers}/file")
    public ResponseEntity<Resource> file(@PathVariable String type, @RequestParam String name,
                                         @RequestParam(required = false) String download) throws IOException {
        Path p = storage.resolveInvoice(OrderType.fromPath(type), name);
        ContentDisposition cd = (download != null ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(p.getFileName().toString(), java.nio.charset.StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(Files.size(p))
                .body(new FileSystemResource(p));
    }

    @PostMapping("/invoices/{type:purchases|sales|offers}/delete")
    public String delete(@PathVariable String type, @RequestParam String name, RedirectAttributes redirect) {
        OrderType t = OrderType.fromPath(type);
        storage.deleteInvoice(t, name);
        audit.log("DELETE_INVOICE", t.getLabel(), name, null);
        redirect.addFlashAttribute("message", "Deleted " + name + ".");
        return "redirect:/invoices/" + t.getPath();
    }
}
