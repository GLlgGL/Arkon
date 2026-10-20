package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "BOM_LINE")
public class BomLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PRODUCT_ID")
    private Long productId;

    @Column(name = "COMPONENT_ID")
    private Long componentId;

    @Column(name = "QUANTITY")
    private Integer quantity;

    public Long getId() { return id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long v) { this.productId = v; }

    public Long getComponentId() { return componentId; }
    public void setComponentId(Long v) { this.componentId = v; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer v) { this.quantity = v; }

}
