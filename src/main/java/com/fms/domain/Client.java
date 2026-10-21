package com.fms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "CLIENT")
public class Client extends Party {

    /** Price list used for this client's sales and offers (Price lists module). */
    @Column(name = "PRICE_LIST_ID")
    private Long priceListId;

    public Long getPriceListId() { return priceListId; }
    public void setPriceListId(Long v) { this.priceListId = v; }

    // ---- VAT (V20): null rate = the normal rate from Settings
    @Column(name = "VAT_RATE")
    private java.math.BigDecimal vatRate;
    @Column(name = "VAT_NOTE")
    private String vatNote;

    public java.math.BigDecimal getVatRate() { return vatRate; }
    public void setVatRate(java.math.BigDecimal r) { this.vatRate = r; }
    public String getVatNote() { return vatNote; }
    public void setVatNote(String n) { this.vatNote = n == null || n.isBlank() ? null : n.trim(); }

    /** Sales book column for this client's 0% sales: EXPORT (default), ZERO, EXEMPT or NOVAT. */
    @Column(name = "VAT_BOOK")
    private String vatBook;

    public String getVatBook() { return vatBook == null ? "EXPORT" : vatBook; }
    public void setVatBook(String v) { this.vatBook = v == null || v.isBlank() ? null : v; }
}
