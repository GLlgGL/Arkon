package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCTION_LINE")
public class ProductionLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ORDER_ID")
    private Long orderId;

    @Column(name = "COMPONENT_NAME")
    private String componentName;

    @Column(name = "UNIT")
    private String unit;

    @Column(name = "QUANTITY")
    private Integer quantity;

    @Column(name = "UNIT_COST")
    private BigDecimal unitCost;

    public Long getId() { return id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long v) { this.orderId = v; }

    public String getComponentName() { return componentName; }
    public void setComponentName(String v) { this.componentName = v; }

    public String getUnit() { return unit; }
    public void setUnit(String v) { this.unit = v; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer v) { this.quantity = v; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal v) { this.unitCost = v; }

}
