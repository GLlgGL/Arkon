package com.fms.web;

import com.fms.service.ExcelImportService;
import com.fms.service.ExcelImportService.Kind;
import com.fms.service.OrderException;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Import products, clients, merchants and opening stock from Excel (administrators). */
@Controller
public class ImportController {

    private final ExcelImportService imports;
    private final CurrentUser currentUser;

    public ImportController(ExcelImportService imports, CurrentUser currentUser) {
        this.imports = imports;
        this.currentUser = currentUser;
    }

    private static Kind kind(String k) {
        try { return Kind.valueOf(k.toUpperCase().replace('-', '_')); } catch (Exception e) { return Kind.PRODUCTS; }
    }

    @GetMapping("/admin/import")
    public String page(HttpSession session, Model model) {
        model.addAttribute("kinds", Kind.values());
        Object p = session.getAttribute("importPreview");
        if (p != null) {
            model.addAttribute("preview", p);
            model.addAttribute("previewUpdate", session.getAttribute("importUpdate"));
            model.addAttribute("previewName", session.getAttribute("importName"));
        }
        return "admin/import";
    }

    @GetMapping("/admin/import/template/{kind}")
    public ResponseEntity<byte[]> template(@PathVariable String kind) {
        Kind k = kind(kind);
        return OrderController.xlsx(imports.template(k), "Arkon-import-" + k.name().toLowerCase().replace('_', '-') + ".xlsx");
    }

    /** Checks the file and shows what would happen; nothing is saved yet. */
    @PostMapping("/admin/import/preview")
    public String preview(@RequestParam String kind, @RequestParam("file") MultipartFile file,
                          @RequestParam(defaultValue = "false") boolean update, HttpSession session, RedirectAttributes redirect) {
        Kind k = kind(kind);
        try {
            if (file == null || file.isEmpty()) throw new OrderException("Choose the Excel file.");
            byte[] bytes = file.getBytes();
            var p = imports.preview(k, bytes, update);
            session.setAttribute("importBytes", bytes);
            session.setAttribute("importKind", k);
            session.setAttribute("importUpdate", update);
            session.setAttribute("importName", file.getOriginalFilename());
            session.setAttribute("importPreview", p);
        } catch (OrderException | IOException e) {
            clear(session);
            redirect.addFlashAttribute("error", e instanceof OrderException ? e.getMessage() : "The file could not be read.");
        }
        return "redirect:/admin/import#preview";
    }

    @PostMapping("/admin/import/apply")
    public String apply(HttpSession session, Principal principal, RedirectAttributes redirect) {
        byte[] bytes = (byte[]) session.getAttribute("importBytes");
        Kind k = (Kind) session.getAttribute("importKind");
        Boolean update = (Boolean) session.getAttribute("importUpdate");
        if (bytes == null || k == null) {
            redirect.addFlashAttribute("error", "Upload the file again.");
            return "redirect:/admin/import";
        }
        try {
            var r = imports.apply(k, bytes, Boolean.TRUE.equals(update), currentUser.of(principal));
            clear(session);
            redirect.addFlashAttribute("message", "Imported: " + r.create() + " created, " + r.update() + " updated, " + r.skip() + " unchanged.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/import";
    }

    @PostMapping("/admin/import/cancel")
    public String cancel(HttpSession session) {
        clear(session);
        return "redirect:/admin/import";
    }

    private static void clear(HttpSession s) {
        for (String a : new String[] {"importBytes", "importKind", "importUpdate", "importName", "importPreview"}) s.removeAttribute(a);
    }
}
