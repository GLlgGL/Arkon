package com.fms.domain;

import jakarta.persistence.*;

/** Units of one product on one shelf. */
@Entity
@Table(name = "SHELF_STOCK")
public class ShelfStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "SHELF_ID")
    private Shelf shelf;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "PRODUCT_ID")
    private Product product;

    @Column(nullable = false)
    private int quantity;

    public Long getId() { return id; }
    public Shelf getShelf() { return shelf; }
    public void setShelf(Shelf shelf) { this.shelf = shelf; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
