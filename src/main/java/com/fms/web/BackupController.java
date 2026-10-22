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
    public String page(org.springframework.ui.Model model) {
        var c = configService.get();
        model.addAttribute("bkNext", autoBackup.next(c));
        return "admin/backup";
    }

    /** Automatic backup settings. */
    @PostMapping("/admin/backup/auto")
    public String autoSettings(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean auto,
                               @org.springframework.web.bind.annotation.RequestParam(required = false) String dir,
                               @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int everyHours,
                               @org.springframework.web.bind.annotation.RequestParam(defaultValue = "02:00") String at,
                               @org.springframework.web.bind.annotation.RequestParam(defaultValue = "true") boolean full,
                               @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean replace,
                               @org.springframework.web.bind.annotation.RequestParam(defaultValue = "14") int keepCount,
                               @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int keepDays,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        var c = configService.get();
        if (auto && (dir == null || dir.isBlank())) {
            redirect.addFlashAttribute("error", "Choose the folder for the automatic backups.");
            return "redirect:/admin/backup";
        }
        c.setBkAuto(auto);
        c.setBkDir(dir == null || dir.isBlank() ? null : dir.trim());
        c.setBkEveryHours(Math.max(0, Math.min(168, everyHours)));
        c.setBkAt(at != null && at.matches("\\d{2}:\\d{2}") ? at : "02:00");
        c.setBkFull(full);
        c.setBkReplace(replace);
        c.setBkKeepCount(Math.max(0, keepCount));
        c.setBkKeepDays(Math.max(0, keepDays));
        configService.save(c);
        redirect.addFlashAttribute("message", "Automatic backup settings saved.");
        return "redirect:/admin/backup";
    }

    /** A backup to the folder now (also to test the folder). */
    @PostMapping("/admin/backup/auto/run")
    public String runNow(org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        var r = autoBackup.run("manual (run now)");
        redirect.addFlashAttribute(r.ok() ? "message" : "error", r.ok() ? "Backup saved: " + r.message() : "Backup failed: " + r.message());
        return "redirect:/admin/backup";
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.service.AutoBackupService autoBackup;
    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.service.ConfigService configService;

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
