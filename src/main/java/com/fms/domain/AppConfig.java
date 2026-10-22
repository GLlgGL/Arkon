package com.fms.domain;

import jakarta.persistence.*;
import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Single-row settings (was Configurations, Id = 1). */
@Entity
@Table(name = "APP_CONFIG")
public class AppConfig {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(nullable = false)
    private String currency = "ALL";

    /** VAT in percent, e.g. 20. The original called this column "TIN". */
    @Column(name = "VAT_PERCENT", nullable = false)
    private BigDecimal vatPercent = BigDecimal.valueOf(20);

    /** Invoice accent colour as "R,G,B". */
    @Column(name = "BRAND_RGB", nullable = false)
    private String brandRgb = "11,71,121";

    @Column(name = "LOGO_FILE")
    private String logoFile;

    public static final String DEFAULT_BG_1 = "#e6eef8";
    public static final String DEFAULT_BG_2 = "#fff1dc";
    public static final int DEFAULT_PATTERN_OPACITY = 10;
    public static final int DEFAULT_PANEL_OPACITY = 90;

    /** Page background gradient, first colour (top left), as #rrggbb. */
    @Column(name = "BG_COLOR_1", nullable = false)
    private String bgColor1 = DEFAULT_BG_1;

    /** Page background gradient, second colour (bottom right), as #rrggbb. */
    @Column(name = "BG_COLOR_2", nullable = false)
    private String bgColor2 = DEFAULT_BG_2;

    /** Watermark icon strength, 0-100 %. 0 hides the watermark. */
    @Column(name = "BG_PATTERN_OPACITY", nullable = false)
    private int bgPatternOpacity = DEFAULT_PATTERN_OPACITY;

    /** Opacity of the white panels and tables, 0-100 %. Lower lets more background show through. */
    @Column(name = "PANEL_OPACITY", nullable = false)
    private int panelOpacity = DEFAULT_PANEL_OPACITY;

    /** VAT as a fraction, e.g. 0.20 (the original's Session["TIN"]). */
    public BigDecimal getVatRate() {
        return vatPercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    }

    public Color getBrandColor() {
        try {
            String[] p = brandRgb.split(",");
            return new Color(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()));
        } catch (RuntimeException e) {
            return new Color(11, 71, 121);
        }
    }

    public String getBrandHex() {
        Color c = getBrandColor();
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    public void setBrandHex(String hex) {
        Color c = Color.decode(hex.startsWith("#") ? hex : "#" + hex);
        this.brandRgb = c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    // ---- Payroll, assets, production, taxes (V13)
    @Column(name = "MOD_PAYROLL", nullable = false)
    private boolean modPayroll;
    @Column(name = "MOD_ASSETS", nullable = false)
    private boolean modAssets;
    @Column(name = "MOD_PRODUCTION", nullable = false)
    private boolean modProduction;
    @Column(name = "MOD_TAXES", nullable = false)
    private boolean modTaxes;
    @Column(name = "PR_EMP_SOCIAL", nullable = false)
    private BigDecimal prEmpSocial = new BigDecimal("9.5");
    @Column(name = "PR_EMP_HEALTH", nullable = false)
    private BigDecimal prEmpHealth = new BigDecimal("1.7");
    @Column(name = "PR_ER_SOCIAL", nullable = false)
    private BigDecimal prErSocial = new BigDecimal("15");
    @Column(name = "PR_ER_HEALTH", nullable = false)
    private BigDecimal prErHealth = new BigDecimal("1.7");
    @Column(name = "PR_MIN_BASE", nullable = false)
    private BigDecimal prMinBase = new BigDecimal("50000");
    @Column(name = "PR_MAX_BASE", nullable = false)
    private BigDecimal prMaxBase = new BigDecimal("186416");
    @Column(name = "PR_TAX_ON_NET", nullable = false)
    private boolean prTaxOnNet;
    @Column(name = "TAX_VAT_DAY", nullable = false)
    private int taxVatDay = 14;
    @Column(name = "TAX_PAYROLL_DAY", nullable = false)
    private int taxPayrollDay = 20;
    public boolean isModPayroll() { return modPayroll; }
    public void setModPayroll(boolean v) { this.modPayroll = v; }
    public boolean isModAssets() { return modAssets; }
    public void setModAssets(boolean v) { this.modAssets = v; }
    public boolean isModProduction() { return modProduction; }
    public void setModProduction(boolean v) { this.modProduction = v; }
    public boolean isModTaxes() { return modTaxes; }
    public void setModTaxes(boolean v) { this.modTaxes = v; }
    public BigDecimal getPrEmpSocial() { return prEmpSocial; }
    public void setPrEmpSocial(BigDecimal v) { this.prEmpSocial = v; }
    public BigDecimal getPrEmpHealth() { return prEmpHealth; }
    public void setPrEmpHealth(BigDecimal v) { this.prEmpHealth = v; }
    public BigDecimal getPrErSocial() { return prErSocial; }
    public void setPrErSocial(BigDecimal v) { this.prErSocial = v; }
    public BigDecimal getPrErHealth() { return prErHealth; }
    public void setPrErHealth(BigDecimal v) { this.prErHealth = v; }
    public BigDecimal getPrMinBase() { return prMinBase; }
    public void setPrMinBase(BigDecimal v) { this.prMinBase = v; }
    public BigDecimal getPrMaxBase() { return prMaxBase; }
    public void setPrMaxBase(BigDecimal v) { this.prMaxBase = v; }
    public boolean isPrTaxOnNet() { return prTaxOnNet; }
    public void setPrTaxOnNet(boolean v) { this.prTaxOnNet = v; }
    public int getTaxVatDay() { return taxVatDay; }
    public void setTaxVatDay(int v) { this.taxVatDay = v; }
    public int getTaxPayrollDay() { return taxPayrollDay; }
    public void setTaxPayrollDay(int v) { this.taxPayrollDay = v; }
    @Column(name = "MOD_ACCOUNTING", nullable = false)
    private boolean modAccounting;
    public boolean isModAccounting() { return modAccounting; }
    public void setModAccounting(boolean v) { this.modAccounting = v; }

    /** Any of the modules shown in the "More" menu is on. */
    public boolean isAnyMoreModule() { return modPayroll || modAssets || modProduction || modTaxes || modAccounting; }

    // ---- Statements, stock count, reminders, period lock, installable app (V16)
    @Column(name = "MOD_STATEMENTS", nullable = false)
    private boolean modStatements;
    @Column(name = "MOD_STOCK_COUNT", nullable = false)
    private boolean modStockCount;
    @Column(name = "MOD_REMINDERS", nullable = false)
    private boolean modReminders;
    @Column(name = "MOD_PERIOD_LOCK", nullable = false)
    private boolean modPeriodLock;
    @Column(name = "MOD_PWA", nullable = false)
    private boolean modPwa;
    @Column(name = "LOCK_DATE")
    private java.time.LocalDate lockDate;
    @Column(name = "REM_DAYS_BEFORE", nullable = false)
    private int remDaysBefore = 3;
    @Column(name = "REM_DAYS_AFTER", nullable = false)
    private int remDaysAfter = 7;
    @Column(name = "REM_AUTO", nullable = false)
    private boolean remAuto;
    public boolean isModStatements() { return modStatements; }
    public void setModStatements(boolean v) { this.modStatements = v; }
    public boolean isModStockCount() { return modStockCount; }
    public void setModStockCount(boolean v) { this.modStockCount = v; }
    public boolean isModReminders() { return modReminders; }
    public void setModReminders(boolean v) { this.modReminders = v; }
    public boolean isModPeriodLock() { return modPeriodLock; }
    public void setModPeriodLock(boolean v) { this.modPeriodLock = v; }
    public boolean isModPwa() { return modPwa; }
    public void setModPwa(boolean v) { this.modPwa = v; }
    public java.time.LocalDate getLockDate() { return lockDate; }
    public void setLockDate(java.time.LocalDate v) { this.lockDate = v; }
    public int getRemDaysBefore() { return remDaysBefore; }
    public void setRemDaysBefore(int v) { this.remDaysBefore = v; }
    public int getRemDaysAfter() { return remDaysAfter; }
    public void setRemDaysAfter(int v) { this.remDaysAfter = v; }
    public boolean isRemAuto() { return remAuto; }
    public void setRemAuto(boolean v) { this.remAuto = v; }

    // ---- Budgets, two-step login, price lists (V17)
    @Column(name = "MOD_BUDGETS", nullable = false)
    private boolean modBudgets;
    @Column(name = "MOD_2FA", nullable = false)
    private boolean mod2fa;
    @Column(name = "MOD_PRICE_LISTS", nullable = false)
    private boolean modPriceLists;
    @Column(name = "TFA_REQUIRED", nullable = false)
    private boolean tfaRequired;
    public boolean isModBudgets() { return modBudgets; }
    public void setModBudgets(boolean v) { this.modBudgets = v; }
    public boolean isMod2fa() { return mod2fa; }
    public void setMod2fa(boolean v) { this.mod2fa = v; }
    public boolean isModPriceLists() { return modPriceLists; }
    public void setModPriceLists(boolean v) { this.modPriceLists = v; }
    public boolean isTfaRequired() { return tfaRequired; }
    public void setTfaRequired(boolean v) { this.tfaRequired = v; }

    @Column(name = "MOD_CURRENCIES", nullable = false)
    private boolean modCurrencies;
    public boolean isModCurrencies() { return modCurrencies; }
    public void setModCurrencies(boolean v) { this.modCurrencies = v; }

    // ---- Quick sale (V21)
    @Column(name = "MOD_POS", nullable = false)
    private boolean modPos;
    @Column(name = "POS_COMPANY")
    private String posCompany;
    @Column(name = "POS_CLIENT")
    private String posClient;
    public boolean isModPos() { return modPos; }
    public void setModPos(boolean v) { this.modPos = v; }
    public String getPosCompany() { return posCompany; }
    public void setPosCompany(String v) { this.posCompany = v == null || v.isBlank() ? null : v.trim(); }
    public String getPosClient() { return posClient; }
    public void setPosClient(String v) { this.posClient = v == null || v.isBlank() ? null : v.trim(); }

    @Column(name = "MOD_LOCATIONS", nullable = false)
    private boolean modLocations;
    public boolean isModLocations() { return modLocations; }
    public void setModLocations(boolean v) { this.modLocations = v; }

    @Column(name = "MOD_TRACKING", nullable = false)
    private boolean modTracking;
    public boolean isModTracking() { return modTracking; }
    public void setModTracking(boolean v) { this.modTracking = v; }

    // ---- Company details for the official financial statements (V27)
    @Column(name = "CO_LEGAL_FORM")
    private String coLegalForm;
    @Column(name = "CO_FOUNDED")
    private java.time.LocalDate coFounded;
    @Column(name = "CO_ACTIVITY")
    private String coActivity;
    public String getCoLegalForm() { return coLegalForm; }
    public void setCoLegalForm(String v) { this.coLegalForm = v == null || v.isBlank() ? null : v.trim(); }
    public java.time.LocalDate getCoFounded() { return coFounded; }
    public void setCoFounded(java.time.LocalDate v) { this.coFounded = v; }
    public String getCoActivity() { return coActivity; }
    public void setCoActivity(String v) { this.coActivity = v == null || v.isBlank() ? null : v.trim(); }

    /** Optional modules (Settings -> Modules). Off: their menus, pages and fields are hidden. */
    @Column(name = "MOD_CASH", nullable = false)
    private boolean modCash;

    @Column(name = "MOD_COST_CENTERS", nullable = false)
    private boolean modCostCenters;

    public boolean isModCash() { return modCash; }
    public void setModCash(boolean v) { this.modCash = v; }
    public boolean isModCostCenters() { return modCostCenters; }
    public void setModCostCenters(boolean v) { this.modCostCenters = v; }

    /** Language of invoice PDFs, credit notes, purchase orders and pick lists: "en" or "sq". */
    @Column(name = "INVOICE_LANG", nullable = false)
    private String invoiceLang = "en";

    public String getInvoiceLang() { return invoiceLang; }
    public void setInvoiceLang(String v) { this.invoiceLang = "sq".equals(v) ? "sq" : "en"; }

    // ---- Email (Administration -> Email settings)
    @Column(name = "MAIL_ENABLED", nullable = false)
    private boolean mailEnabled;
    @Column(name = "MAIL_HOST")
    private String mailHost;
    @Column(name = "MAIL_PORT", nullable = false)
    private int mailPort = 587;
    /** STARTTLS, SSL or NONE. */
    @Column(name = "MAIL_SECURITY", nullable = false)
    private String mailSecurity = "STARTTLS";
    @Column(name = "MAIL_USERNAME")
    private String mailUsername;
    /** Stored as entered; never shown back in the form. */
    @Column(name = "MAIL_PASSWORD")
    private String mailPassword;
    @Column(name = "MAIL_FROM")
    private String mailFrom;

    /** Email is switched on and has at least a server and a sender address. */
    public boolean isMailReady() {
        return mailEnabled && mailHost != null && !mailHost.isBlank() && mailFrom != null && !mailFrom.isBlank();
    }
    public boolean isMailEnabled() { return mailEnabled; }
    public void setMailEnabled(boolean v) { this.mailEnabled = v; }
    public String getMailHost() { return mailHost; }
    public void setMailHost(String v) { this.mailHost = blank(v); }
    public int getMailPort() { return mailPort; }
    public void setMailPort(int v) { this.mailPort = v; }
    public String getMailSecurity() { return mailSecurity; }
    public void setMailSecurity(String v) { this.mailSecurity = v; }
    public String getMailUsername() { return mailUsername; }
    public void setMailUsername(String v) { this.mailUsername = blank(v); }
    public String getMailPassword() { return mailPassword; }
    public void setMailPassword(String v) { this.mailPassword = v; }
    public boolean isMailPasswordSet() { return mailPassword != null && !mailPassword.isEmpty(); }
    public String getMailFrom() { return mailFrom; }
    public void setMailFrom(String v) { this.mailFrom = blank(v); }
    private static String blank(String v) { return v == null || v.isBlank() ? null : v.trim(); }

    /** Shelves on or off. Off: no shelf fields anywhere and shelves never limit anything. */
    @Column(name = "USE_SHELVES", nullable = false)
    private boolean useShelves = false;

    public boolean isUseShelves() { return useShelves; }
    public void setUseShelves(boolean useShelves) { this.useShelves = useShelves; }

    /** Default days until a sale or purchase must be paid. */
    @Column(name = "PAYMENT_TERMS_DAYS", nullable = false)
    private int paymentTermsDays = 30;

    /** Default days an offer stays valid. */
    @Column(name = "OFFER_VALID_DAYS", nullable = false)
    private int offerValidDays = 15;

    public int getPaymentTermsDays() { return paymentTermsDays; }
    public void setPaymentTermsDays(int v) { this.paymentTermsDays = Math.max(0, Math.min(3650, v)); }
    public int getOfferValidDays() { return offerValidDays; }
    public void setOfferValidDays(int v) { this.offerValidDays = Math.max(1, Math.min(3650, v)); }

    /** CSS opacity values (0.0-1.0) for the page template. */
    public String getPatternAlpha() { return String.valueOf(bgPatternOpacity / 100.0); }
    public String getPanelAlpha() { return String.valueOf(panelOpacity / 100.0); }

    public String getBgColor1() { return bgColor1; }
    public void setBgColor1(String bgColor1) { this.bgColor1 = bgColor1; }
    public String getBgColor2() { return bgColor2; }
    public void setBgColor2(String bgColor2) { this.bgColor2 = bgColor2; }
    public int getBgPatternOpacity() { return bgPatternOpacity; }
    public void setBgPatternOpacity(int v) { this.bgPatternOpacity = Math.max(0, Math.min(100, v)); }
    public int getPanelOpacity() { return panelOpacity; }
    public void setPanelOpacity(int v) { this.panelOpacity = Math.max(0, Math.min(100, v)); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getVatPercent() { return vatPercent; }
    public void setVatPercent(BigDecimal vatPercent) { this.vatPercent = vatPercent; }
    public String getBrandRgb() { return brandRgb; }
    public void setBrandRgb(String brandRgb) { this.brandRgb = brandRgb; }
    public String getLogoFile() { return logoFile; }
    public void setLogoFile(String logoFile) { this.logoFile = logoFile; }
}
