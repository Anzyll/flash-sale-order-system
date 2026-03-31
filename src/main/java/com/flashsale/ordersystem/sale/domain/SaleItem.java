package com.flashsale.ordersystem.sale.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


@Entity
@Table(
        name = "sale_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_sale_product", columnNames = {"sale_id", "product_id"})
        }
)
@Getter
@Setter
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "sale_id", nullable = false)
    private Long saleId;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(name = "sale_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal salePrice;
    @Column(name = "total_stock", nullable = false)
    private Integer totalStock;
    @Column(name = "available_stock", nullable = false)
    private Integer availableStock;
}
