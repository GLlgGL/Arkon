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
