package com.fms.web;

import com.fms.domain.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class ProductForm {

    @NotBlank(message = "Enter a product name.")
    @Size(max = 200)
    private String name;
    @NotBlank(message = "Enter a unit, e.g. pcs or kg.")
    @Size(max = 30)
    private String unit;
    @NotNull(message = "Enter the buy price.")
    @DecimalMin(value = "0", message = "Prices can't be negative.")
    private BigDecimal buyPrice;
    @NotNull(message = "Enter the sale price.")
    @DecimalMin(value = "0", message = "Prices can't be negative.")
    private BigDecimal salePrice;
    @Min(value = 0, message = "The limit can't be negative.")
    private int quantityLimit;
    private Long classId;
    @Size(max = 50)
    private String code;

    public static ProductForm of(Product p) {
        ProductForm f = new ProductForm();
        f.name = p.getName();
        f.unit = p.getUnit();
        f.buyPrice = p.getBuyPrice();
        f.salePrice = p.getSalePrice();
        f.quantityLimit = p.getQuantityLimit();
        f.classId = p.getProductClass() == null ? null : p.getProductClass().getId();
        f.code = p.getCode();
        return f;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? null : name.trim(); }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit == null ? null : unit.trim(); }
    public BigDecimal getBuyPrice() { return buyPrice; }
    public void setBuyPrice(BigDecimal buyPrice) { this.buyPrice = buyPrice; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public int getQuantityLimit() { return quantityLimit; }
    public void setQuantityLimit(int quantityLimit) { this.quantityLimit = quantityLimit; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code == null || code.isBlank() ? null : code.trim(); }
    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
}
