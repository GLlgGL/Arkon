package com.fms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "MERCHANT")
public class Merchant extends Party {

    /** Default type of purchases from this supplier (purchase book): LOCAL, IMPORT, INVEST_LOCAL, INVEST_IMPORT, FARMER. */
    @Column(name = "PURCHASE_KIND")
    private String purchaseKind;

    public String getPurchaseKind() { return purchaseKind == null ? "LOCAL" : purchaseKind; }
    public void setPurchaseKind(String k) { this.purchaseKind = k == null || k.isBlank() || "LOCAL".equals(k) ? null : k; }
}
