package com.flashsale.ordersystem.product.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.flashsale.ordersystem.product.domain.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long> {
}
