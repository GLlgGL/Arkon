package com.fms.web;

import com.fms.domain.AppConfig;
import com.fms.service.AuditService;
import com.fms.service.ConfigService;
import com.fms.service.MailService;
import com.fms.service.OrderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Administration -> Email settings: the mail server used to email invoices.
 * Email is optional. When it's switched on, every field must be filled in;
 * otherwise saving is refused, the missing fields are listed and what was
 * typed stays in the form.
 */
@Controller
public class EmailSettingsController {

    /** What the form shows: the saved settings, or what was just typed. */
    public record EmailForm(boolean mailEnabled, String mailHost, int mailPort, String mailSecurity,
                            String mailUsername, String mailFrom) {
        static EmailForm of(AppConfig c) {
            return new EmailForm(c.isMailEnabled(), c.getMailHost(), c.getMailPort(), c.getMailSecurity(),
                    c.getMailUsername(), c.getMailFrom());
        }
    }

    private final ConfigService configService;
    private final MailService mail;
    private final AuditService audit;
    private final MessageSource messages;

    public EmailSettingsController(ConfigService configService, MailService mail, AuditService audit, MessageSource messages) {
        this.configService = configService;
        this.mail = mail;
        this.audit = audit;
        this.messages = messages;
    }

    @GetMapping("/admin/email")
    public String page(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", EmailForm.of(configService.get()));
        }
        return "admin/email";
    }

    @PostMapping("/admin/email")
    public String save(@RequestParam(defaultValue = "false") boolean mailEnabled,
                       @RequestParam(required = false) String mailHost,
                       @RequestParam(required = false) Integer mailPort,
                       @RequestParam(defaultValue = "STARTTLS") String mailSecurity,
                       @RequestParam(required = false) String mailUsername,
                       @RequestParam(required = false) String mailPassword,
                       @RequestParam(defaultValue = "false") boolean clearPassword,
                       @RequestParam(required = false) String mailFrom,
                       Model model, RedirectAttributes redirect) {
        AppConfig c = configService.get();
        String security = mailSecurity.matches("STARTTLS|SSL|NONE") ? mailSecurity : "STARTTLS";
        boolean passwordAfterSave = !clearPassword && ((mailPassword != null && !mailPassword.isEmpty()) || c.isMailPasswordSet());

        // Email switched on: everything must be filled in.
        if (mailEnabled) {
            Locale locale = LocaleContextHolder.getLocale();
            List<String> missing = new ArrayList<>();
            if (blank(mailHost)) missing.add(label("email.host", locale));
            if (mailPort == null || mailPort < 1 || mailPort > 65535) missing.add(label("email.port", locale));
            if (blank(mailUsername)) missing.add(label("email.username", locale));
            if (!passwordAfterSave) missing.add(label("email.password", locale));
            if (blank(mailFrom) || !mailFrom.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) missing.add(label("email.from", locale));
            if (!missing.isEmpty()) {
                model.addAttribute("error", messages.getMessage("email.missing",
                        new Object[] {String.join(", ", missing)}, locale));
                model.addAttribute("form", new EmailForm(true, mailHost, mailPort == null ? 587 : mailPort, security,
                        mailUsername, mailFrom));
                return "admin/email";
            }
        }

        c.setMailEnabled(mailEnabled);
        c.setMailHost(mailHost);
        c.setMailPort(mailPort == null || mailPort < 1 || mailPort > 65535 ? 587 : mailPort);
        c.setMailSecurity(security);
        c.setMailUsername(mailUsername);
        if (clearPassword) {
            c.setMailPassword(null);
        } else if (mailPassword != null && !mailPassword.isEmpty()) {
            c.setMailPassword(mailPassword);      // empty field = keep the saved password
        }
        c.setMailFrom(mailFrom);
        configService.save(c);
        audit.log("UPDATE", "Settings", "Email", (mailEnabled ? "on" : "off") + ", " + c.getMailHost() + ":" + c.getMailPort()
                + " " + security + ", from " + c.getMailFrom());
        redirect.addFlashAttribute("message", "Email settings saved.");
        return "redirect:/admin/email";
    }

    @PostMapping("/admin/email/test")
    public String test(@RequestParam String to, RedirectAttributes redirect) {
        try {
            mail.sendTest(to);
            audit.log("EMAIL_TEST", "Settings", "Email", "Test sent to " + to.trim());
            redirect.addFlashAttribute("message", "Test email sent. Check the inbox.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/email";
    }

    private String label(String key, Locale locale) {
        return messages.getMessage(key, null, key, locale);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
