package com.flashsale.ordersystem.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.flashsale.ordersystem.product.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long> {
}
