package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One client's discount percentage for one product class.
 * Replaces the CClass table, which grew a new column per class at runtime.
 */
@Entity
@Table(name = "CLIENT_CLASS_DISCOUNT")
public class ClientClassDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "CLIENT_ID")
    private Client client;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "PRODUCT_CLASS_ID")
    private ProductClass productClass;

    @Column(name = "DISCOUNT_PERCENT", nullable = false)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "REGISTERED_AT", nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public ProductClass getProductClass() { return productClass; }
    public void setProductClass(ProductClass productClass) { this.productClass = productClass; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}
