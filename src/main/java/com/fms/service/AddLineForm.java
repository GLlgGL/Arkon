package com.fms.service;

import java.math.BigDecimal;

/**
 * Fields posted by the "Add product" button on an order page.
 * The prospect* fields are only used for offers to someone who is not a client.
 */
public class AddLineForm {

    private String provider;
    private String buyer;
    private String product;
    private int quantity = 1;
    private BigDecimal manualDiscount;
    /** Purchases only: the unit price actually paid, if different from the product's buy price. */
    private BigDecimal unitPrice;
    /** Sales: shelf to take from; purchases: shelf to put on. Null = not on a shelf. */
    private Long shelfId;
    private String prospectTin;
    private String prospectAddress;
    private String prospectEmail;
    private String prospectPhone;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = trim(provider); }
    public String getBuyer() { return buyer; }
    public void setBuyer(String buyer) { this.buyer = trim(buyer); }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = trim(product); }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Long getShelfId() { return shelfId; }
    public void setShelfId(Long shelfId) { this.shelfId = shelfId; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getManualDiscount() { return manualDiscount; }
    public void setManualDiscount(BigDecimal manualDiscount) { this.manualDiscount = manualDiscount; }
    public String getProspectTin() { return prospectTin; }
    public void setProspectTin(String v) { this.prospectTin = trim(v); }
    public String getProspectAddress() { return prospectAddress; }
    public void setProspectAddress(String v) { this.prospectAddress = trim(v); }
    public String getProspectEmail() { return prospectEmail; }
    public void setProspectEmail(String v) { this.prospectEmail = trim(v); }
    public String getProspectPhone() { return prospectPhone; }
    public void setProspectPhone(String v) { this.prospectPhone = trim(v); }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
