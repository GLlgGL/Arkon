package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "STOCK_COUNT_LINE")
public class StockCountLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "COUNT_ID")
    private Long countId;

    @Column(name = "PRODUCT_NAME")
    private String productName;

    @Column(name = "UNIT")
    private String unit;

    @Column(name = "EXPECTED")
    private Long expected;

    @Column(name = "COUNTED")
    private Long counted;

    public Long getId() { return id; }

    public Long getCountId() { return countId; }
    public void setCountId(Long v) { this.countId = v; }

    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }

    public String getUnit() { return unit; }
    public void setUnit(String v) { this.unit = v; }

    public Long getExpected() { return expected; }
    public void setExpected(Long v) { this.expected = v; }

    public Long getCounted() { return counted; }
    public void setCounted(Long v) { this.counted = v; }


    /** Counted minus expected (null while not counted). */
    public Long getDifference() { return counted == null ? null : counted - expected; }
}
