package com.flashsale.ordersystem.sale.application.service;

import com.flashsale.ordersystem.common.exception.CustomException;
import com.flashsale.ordersystem.common.exception.ErrorCode;
import com.flashsale.ordersystem.product.domain.Product;
import com.flashsale.ordersystem.product.infrastructure.ProductRepository;
import com.flashsale.ordersystem.sale.presentation.dto.AddProductToSaleRequest;
import com.flashsale.ordersystem.sale.presentation.dto.SaleItemResponse;
import com.flashsale.ordersystem.sale.domain.Sale;
import com.flashsale.ordersystem.sale.presentation.dto.CreateSaleRequest;
import com.flashsale.ordersystem.sale.presentation.dto.SaleResponse;
import com.flashsale.ordersystem.sale.domain.SaleItem;
import com.flashsale.ordersystem.sale.application.mapper.SaleItemMapper;
import com.flashsale.ordersystem.sale.application.mapper.SaleMapper;
import com.flashsale.ordersystem.sale.infrastructure.SaleItemRepository;
import com.flashsale.ordersystem.sale.infrastructure.SaleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {
    private  final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;
    private final StringRedisTemplate redisTemplate;
    @Transactional
    public Sale createSale(@Valid CreateSaleRequest request) {
        Sale sale = SaleMapper.toEntity(request);
        return saleRepository.save(sale);
    }

    @Transactional
    public SaleItem addProductToSale(Long saleId, AddProductToSaleRequest request) {

        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new CustomException(ErrorCode.SALE_NOT_FOUND));

        if (sale.getEndTime().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.SALE_EXPIRED);
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (request.salePrice().signum() <= 0) {
            throw new CustomException(ErrorCode.INVALID_PRICE);
        }

        if (request.totalStock() <= 0) {
            throw new CustomException(ErrorCode.INVALID_STOCK);
        }


        SaleItem saleItem = SaleItemMapper.toEntity(request, sale, product);

        try {
            SaleItem savedItem = saleItemRepository.save(saleItem);

            // 🔥 Redis key remains same
            String key = "stock:%d:%d".formatted(saleId, request.productId());

            redisTemplate.opsForValue().set(
                    key,
                    String.valueOf(request.totalStock())
            );

            log.info("Initialized stock in Redis for product {} with stock {}",
                    savedItem.getProduct().getId(),
                    savedItem.getTotalStock());

            return savedItem;

        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.PRODUCT_ALREADY_IN_SALE);
        }
    }

    public List<Sale> getSales() {
        return saleRepository.findAll();
    }

    public List<SaleItem> getSaleItems(Long saleId) {
        saleRepository.findById(saleId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SALE_NOT_FOUND));

            return saleItemRepository.findAllBySaleId(saleId);
        }
}
