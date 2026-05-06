package com.flashsale.ordersystem.product.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "description", nullable = false)
    private String  description;
    @Column(name = "price", nullable = false)
    private BigDecimal price;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @PrePersist
    public void prePersist(){
        this.createdAt=Instant.now();
    }
}
