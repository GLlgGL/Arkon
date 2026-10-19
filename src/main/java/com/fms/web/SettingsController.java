package com.fms.web;

import com.fms.domain.AppConfig;
import com.fms.service.ConfigService;
import com.fms.service.StorageService;
import java.math.BigDecimal;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Configuration page: currency, VAT, invoice colour and logo. */
@Controller
public class SettingsController {

    private final ConfigService configService;
    private final StorageService storage;

    private final com.fms.service.AuditService audit;

    public SettingsController(ConfigService configService, StorageService storage, com.fms.service.AuditService audit) {
        this.configService = configService;
        this.storage = storage;
        this.audit = audit;
    }

    /** Default payment terms for sales and purchases, and how long offers stay valid. */
    @PostMapping("/settings/terms")
    public String saveTerms(@RequestParam int paymentTermsDays, @RequestParam int offerValidDays, RedirectAttributes redirect) {
        AppConfig c = configService.get();
        c.setPaymentTermsDays(paymentTermsDays);
        c.setOfferValidDays(offerValidDays);
        configService.save(c);
        audit.log("UPDATE", "Settings", "Terms", "payment " + c.getPaymentTermsDays() + " days, offers " + c.getOfferValidDays() + " days");
        redirect.addFlashAttribute("message", "Terms saved. They apply to orders created from now on.");
        return "redirect:/settings";
    }

    @GetMapping("/settings")
    public String page() {
        return "settings";
    }

    @PostMapping("/settings")
    public String save(@RequestParam String currency, @RequestParam BigDecimal vatPercent,
                       @RequestParam String brandHex, RedirectAttributes redirect) {
        if (currency == null || currency.isBlank() || vatPercent.signum() < 0 || vatPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            redirect.addFlashAttribute("error", "Enter a currency and a VAT rate between 0 and 100.");
            return "redirect:/settings";
        }
        AppConfig c = configService.get();
        c.setCurrency(currency.trim());
        c.setVatPercent(vatPercent);
        try {
            c.setBrandHex(brandHex);
        } catch (NumberFormatException e) {
            redirect.addFlashAttribute("error", "The colour must be a hex value like #0b4779.");
            return "redirect:/settings";
        }
        configService.save(c);
        audit.log("UPDATE", "Settings", "Money", "currency " + c.getCurrency() + ", VAT " + c.getVatPercent() + "%");
        redirect.addFlashAttribute("message", "Settings saved. New lines use the new VAT rate; saved lines keep theirs.");
        return "redirect:/settings";
    }

    private static final java.util.regex.Pattern HEX = java.util.regex.Pattern.compile("#[0-9a-fA-F]{6}");

    /** Page background: gradient colours, watermark strength, panel opacity. */
    @PostMapping("/settings/background")
    public String saveBackground(@RequestParam String bgColor1, @RequestParam String bgColor2,
                                 @RequestParam int bgPatternOpacity, @RequestParam int panelOpacity,
                                 RedirectAttributes redirect) {
        if (!HEX.matcher(bgColor1).matches() || !HEX.matcher(bgColor2).matches()) {
            redirect.addFlashAttribute("error", "Colours must be hex values like #e6eef8.");
            return "redirect:/settings";
        }
        AppConfig c = configService.get();
        c.setBgColor1(bgColor1.toLowerCase());
        c.setBgColor2(bgColor2.toLowerCase());
        c.setBgPatternOpacity(bgPatternOpacity);
        c.setPanelOpacity(panelOpacity);
        configService.save(c);
        redirect.addFlashAttribute("message", "Background saved.");
        return "redirect:/settings";
    }

    @PostMapping("/settings/background/reset")
    public String resetBackground(RedirectAttributes redirect) {
        AppConfig c = configService.get();
        c.setBgColor1(AppConfig.DEFAULT_BG_1);
        c.setBgColor2(AppConfig.DEFAULT_BG_2);
        c.setBgPatternOpacity(AppConfig.DEFAULT_PATTERN_OPACITY);
        c.setPanelOpacity(AppConfig.DEFAULT_PANEL_OPACITY);
        configService.save(c);
        redirect.addFlashAttribute("message", "Background reset to the default pastel blue and peach.");
        return "redirect:/settings";
    }

    @PostMapping("/settings/logo")
    public String uploadLogo(@RequestParam("file") MultipartFile file, RedirectAttributes redirect) {
        if (file.isEmpty()) {
            redirect.addFlashAttribute("error", "Choose an image to upload.");
            return "redirect:/settings";
        }
        try {
            AppConfig c = configService.get();
            c.setLogoFile(storage.storeLogo(file));
            configService.save(c);
            redirect.addFlashAttribute("message", "Logo uploaded.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/settings/logo/delete")
    public String deleteLogo(RedirectAttributes redirect) {
        AppConfig c = configService.get();
        storage.deleteLogo(c.getLogoFile());
        c.setLogoFile(null);
        configService.save(c);
        redirect.addFlashAttribute("message", "Logo removed. Invoices will be created without a logo.");
        return "redirect:/settings";
    }

    @GetMapping("/settings/logo")
    public ResponseEntity<Resource> logo() {
        var p = storage.logoPath(configService.get().getLogoFile());
        if (p == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType type = MediaTypeFactory.getMediaType(p.getFileName().toString()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type).body(new FileSystemResource(p));
    }
}
