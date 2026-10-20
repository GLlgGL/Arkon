package com.fms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A stock change that isn't a purchase or a sale: a return, damage, loss or a
 * correction after counting the warehouse. Quantity is signed: + adds, - removes.
 */
@Entity
@Table(name = "STOCK_ADJUSTMENT")
public class StockAdjustment {

    /** Why stock changed; each reason knows whether it adds or removes stock. */
    public enum Reason {
        RETURN_FROM_CLIENT("Returned by a client", 1),
        RETURN_TO_MERCHANT("Returned to a merchant", -1),
        DAMAGED("Damaged", -1),
        LOST("Lost or stolen", -1),
        COUNT_MORE("Stock count: more than recorded", 1),
        COUNT_LESS("Stock count: less than recorded", -1),
        OTHER_IN("Other: add to stock", 1),
        OTHER_OUT("Other: remove from stock", -1),
        // Production module (not chosen by hand)
        PRODUCTION_IN("Produced", 1),
        PRODUCTION_OUT("Used in production", -1);

        private final String label;
        private final int sign;

        Reason(String label, int sign) {
            this.label = label;
            this.sign = sign;
        }

        public String getLabel() { return label; }
        public int getSign() { return sign; }
        public boolean isAdding() { return sign > 0; }
        public boolean isManual() { return this != PRODUCTION_IN && this != PRODUCTION_OUT; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PRODUCT_NAME", nullable = false)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Reason reason;

    @Column(name = "REF_NO")
    private String refNo;

    private String note;

    /** Shelf the goods were taken from (sale) or put on (purchase, adjustment); optional. */
    @Column(name = "SHELF_ID")
    private Long shelfId;

    @Column(name = "SHELF_CODE")
    private String shelfCode;

    @Column(name = "CREATED_BY", nullable = false)
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Reason getReason() { return reason; }
    public void setReason(Reason reason) { this.reason = reason; }
    public String getRefNo() { return refNo; }
    public void setRefNo(String refNo) { this.refNo = refNo; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getShelfId() { return shelfId; }
    public void setShelfId(Long shelfId) { this.shelfId = shelfId; }
    public String getShelfCode() { return shelfCode; }
    public void setShelfCode(String shelfCode) { this.shelfCode = shelfCode; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
