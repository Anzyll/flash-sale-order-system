package com.flashsale.ordersystem.sale.service;

import com.flashsale.ordersystem.shared.exception.BusinessException;
import com.flashsale.ordersystem.shared.exception.ErrorCode;
import com.flashsale.ordersystem.product.service.ProductService;
import com.flashsale.ordersystem.product.domain.Product;
import com.flashsale.ordersystem.sale.domain.enums.SaleStatus;
import com.flashsale.ordersystem.sale.presentation.dto.AddProductToSaleRequest;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.sale.presentation.dto.CreateSaleRequest;
import com.flashsale.ordersystem.sale.domain.model.SaleItem;
import com.flashsale.ordersystem.sale.presentation.mapper.SaleItemMapper;
import com.flashsale.ordersystem.sale.presentation.mapper.SaleMapper;
import com.flashsale.ordersystem.sale.repository.SaleItemRepository;
import com.flashsale.ordersystem.sale.repository.SaleRepository;
import com.flashsale.ordersystem.sale.presentation.dto.SaleData;
import com.flashsale.ordersystem.shared.port.SaleStockPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {
    private  final SaleRepository saleRepository;
    private final ProductService productService;
    private final SaleItemRepository saleItemRepository;
    private final SaleStockPort saleStockPort;

    @Transactional
    public Sale createSale(@Valid CreateSaleRequest request) {
        log.info("Creating sale. name={}, startAt={}, endAt={}",
                request.title(), request.startTime(), request.endTime());
        Sale sale = SaleMapper.toEntity(request);
        return saleRepository.save(sale);
    }

    @Transactional
    public SaleItem addProductToSale(Long saleId, AddProductToSaleRequest request) {
        log.info("Add product to sale. saleId={}, productId={}",
                saleId, request.productId());
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SALE_NOT_FOUND));

        if (sale.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.SALE_EXPIRED);
        }

        Product product = productService.getProductOrThrow(request.productId());

        if (request.salePrice().signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PRICE);
        }

        if (request.totalStock() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_STOCK);
        }

        SaleItem saleItem = SaleItemMapper.toEntity(request, sale, product);

        try {
            SaleItem savedItem = saleItemRepository.save(saleItem);
            log.info("Product added to sale. saleId={}, productId={}",
                    saleId, request.productId());
            return savedItem;

        } catch (DataIntegrityViolationException e) {
            log.warn("Product already in sale. saleId={}, productId={}",
                    saleId, request.productId());
            throw new BusinessException(ErrorCode.PRODUCT_ALREADY_IN_SALE);
        }
    }

    public List<Sale> getSales() {
        return saleRepository.findAll();
    }

    public List<SaleItem> getSaleItems(Long saleId) {
        saleRepository.findById(saleId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SALE_NOT_FOUND));

            return saleItemRepository.findAllBySaleId(saleId);
        }

    public void validateSaleExists(Long saleId) {
       Sale sale = saleRepository.findById(saleId)
                .orElseThrow(()->new BusinessException(ErrorCode.SALE_NOT_FOUND));

       if (sale.getStatus() != SaleStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.SALE_NOT_ACTIVE);
        }
    }

    public void validateProductInSale(Long saleId, Long productId) {
        boolean exists = saleItemRepository.existsBySaleIdAndProductId(saleId, productId);
        if (!exists) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    public SaleData getSaleAndItem(Long saleId, Long productId) {

        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SALE_NOT_FOUND));

        SaleItem item = saleItemRepository
                .findBySaleIdAndProductId(saleId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        return new SaleData(
                sale,
                item.getProduct(),
                item.getSalePrice()
        );
    }
    public int getTotalStock(Long saleId, Long productId) {

        SaleItem item = saleItemRepository
                .findBySaleIdAndProductId(saleId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        return item.getTotalStock();
    }

    @Transactional
    public void activateSale(Long saleId){
        Sale sale = saleRepository.findById(saleId).orElseThrow();
        if (sale.getStatus() != SaleStatus.PENDING) return;
        List<SaleItem> items = saleItemRepository.findAllBySaleId(saleId);
        long ttl = Duration
                .between(LocalDateTime.now(), sale.getEndTime())
                .getSeconds();
        ttl = Math.max(ttl, 60);

        sale.setStatus(SaleStatus.ACTIVE);
        saleRepository.save(sale);

        try {
            for (SaleItem item : items){
                saleStockPort.initializeSaleStock(
                        saleId,
                        item.getProduct().getId(),
                        item.getTotalStock(),
                        ttl
                );
            }
            saleStockPort.activateSale(saleId, ttl);

        } catch (Exception e) {
            log.error("Redis init failed for sale {}", saleId, e);
        }
    }

    @Transactional
    public void deactivateSale(Long saleId){
        Sale sale = saleRepository.findById(saleId).orElseThrow();
        if (sale.getStatus() != SaleStatus.ACTIVE) return;
        List<SaleItem> items = saleItemRepository.findAllBySaleId(saleId);

        sale.setStatus(SaleStatus.ENDED);
        saleRepository.save(sale);

        try {
            for (SaleItem item : items){
                saleStockPort.deactivateSale(
                        saleId,
                        item.getProduct().getId()
                );
            }
            saleStockPort.deactivateSale(saleId);

        } catch (Exception e) {
            log.error("Redis deactivate failed for sale {}", saleId, e);
        }
    }
}
