package com.flashsale.ordersystem.sale.domain;

import com.flashsale.ordersystem.product.domain.Product;
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(name = "sale_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal salePrice;
    @Column(name = "total_stock", nullable = false)
    private Integer totalStock;
    @Column(name = "available_stock", nullable = false)
    private Integer availableStock;
}
