package com.flashsale.ordersystem.sale.service;

import com.flashsale.ordersystem.common.exception.CustomException;
import com.flashsale.ordersystem.common.exception.ErrorCode;
import com.flashsale.ordersystem.product.repository.ProductRepository;
import com.flashsale.ordersystem.sale.dto.AddProductToSaleRequest;
import com.flashsale.ordersystem.sale.dto.SaleItemResponse;
import com.flashsale.ordersystem.sale.entity.Sale;
import com.flashsale.ordersystem.sale.dto.CreateSaleRequest;
import com.flashsale.ordersystem.sale.dto.SaleResponse;
import com.flashsale.ordersystem.sale.entity.SaleItem;
import com.flashsale.ordersystem.sale.mapper.SaleItemMapper;
import com.flashsale.ordersystem.sale.mapper.SaleMapper;
import com.flashsale.ordersystem.sale.repository.SaleItemRepository;
import com.flashsale.ordersystem.sale.repository.SaleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {
    private  final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;
    @Transactional
    public SaleResponse createSale(@Valid CreateSaleRequest request) {
        Sale sale = SaleMapper.toEntity(request);
        Sale savedSale = saleRepository.save(sale);
        return SaleMapper.toResponse(savedSale);
    }

    @Transactional
    public SaleItemResponse addProductToSale(Long saleId, AddProductToSaleRequest request) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new CustomException(ErrorCode.SALE_NOT_FOUND));

        if (sale.getEndTime().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.SALE_EXPIRED);
        }

        if (!productRepository.existsById(request.productId())) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (request.salePrice().signum() <= 0) {
            throw new CustomException(ErrorCode.INVALID_PRICE);
        }

        if (request.totalStock() <= 0) {
            throw new CustomException(ErrorCode.INVALID_STOCK);
        }

        SaleItem saleItem = SaleItemMapper.toEntity(
                request,
                saleId
        );
        try {
            SaleItem saved = saleItemRepository.save(saleItem);
            return SaleItemMapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.PRODUCT_ALREADY_IN_SALE);
        }
    }

    public List<SaleResponse> getSales() {
        return saleRepository.findAll()
                .stream()
                .map(sale->new SaleResponse(
                        sale.getId(),
                        sale.getTitle(),
                        sale.getStartTime(),
                        sale.getEndTime()
                ))
                .toList();
    }

    public List<SaleItemResponse> getSaleItems(Long saleId) {
        saleRepository.findById(saleId)
                .orElseThrow(()->new CustomException(ErrorCode.SALE_NOT_FOUND));
        return saleItemRepository.findAllBySaleId(saleId)
                .stream()
                .map(item->new SaleItemResponse(
                        item.getId(),
                        item.getSaleId(),
                        item.getProductId(),
                        item.getSalePrice(),
                        item.getAvailableStock()
                ))
                .toList();
    }
}
