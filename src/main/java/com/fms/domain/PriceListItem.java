package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "PRICE_LIST_ITEM")
public class PriceListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "LIST_ID")
    private Long listId;

    @Column(name = "PRODUCT_ID")
    private Long productId;

    @Column(name = "PRICE")
    private BigDecimal price;

    public Long getId() { return id; }

    public Long getListId() { return listId; }
    public void setListId(Long v) { this.listId = v; }

    public Long getProductId() { return productId; }
    public void setProductId(Long v) { this.productId = v; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal v) { this.price = v; }

}
