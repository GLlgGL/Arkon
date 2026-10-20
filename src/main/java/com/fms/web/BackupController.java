package com.fms.web;

import com.fms.service.AuditService;
import com.fms.service.BackupService;
import com.fms.service.OrderException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Administration -> Backup and restore. */
@Controller
public class BackupController {

    private final BackupService backups;
    private final AuditService audit;

    public BackupController(BackupService backups, AuditService audit) {
        this.backups = backups;
        this.audit = audit;
    }

    @GetMapping("/admin/backup")
    public String page() {
        return "admin/backup";
    }

    /** Streams the backup file; the page lets the browser ask where to save it. */
    @GetMapping("/admin/backup/export")
    public void export(HttpServletResponse response) throws IOException {
        String name = backups.fileName();
        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"");
        response.setHeader("X-Backup-Name", name);
        backups.export(response.getOutputStream());
        audit.log("EXPORT_BACKUP", "Backup", name, null);
    }

    @PostMapping("/admin/backup/restore")
    public String restore(@RequestParam("file") MultipartFile file, @RequestParam(defaultValue = "false") boolean confirm,
                          Principal principal, HttpServletRequest request, RedirectAttributes redirect) throws IOException {
        if (!confirm) {
            redirect.addFlashAttribute("error", "Tick the box to confirm that all current data will be replaced.");
            return "redirect:/admin/backup";
        }
        if (file == null || file.isEmpty()) {
            redirect.addFlashAttribute("error", "Choose a backup file.");
            return "redirect:/admin/backup";
        }
        try {
            backups.restore(file.getInputStream(), principal.getName());
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/backup";
        }
        // Users and passwords may have changed with the restore: log in again.
        request.getSession().invalidate();
        return "redirect:/login?restored";
    }
}
