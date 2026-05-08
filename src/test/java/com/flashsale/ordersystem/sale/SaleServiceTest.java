package com.flashsale.ordersystem.sale;

import com.flashsale.ordersystem.product.domain.Product;
import com.flashsale.ordersystem.product.service.ProductService;
import com.flashsale.ordersystem.sale.domain.enums.SaleStatus;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.sale.domain.model.SaleItem;
import com.flashsale.ordersystem.sale.presentation.dto.AddProductToSaleRequest;
import com.flashsale.ordersystem.sale.repository.SaleItemRepository;
import com.flashsale.ordersystem.sale.repository.SaleRepository;
import com.flashsale.ordersystem.sale.service.SaleService;
import com.flashsale.ordersystem.shared.exception.BusinessException;
import com.flashsale.ordersystem.shared.exception.ErrorCode;
import com.flashsale.ordersystem.shared.port.SaleStockPort;
import com.flashsale.ordersystem.shared.port.StockReservationPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductService productService;

    @Mock
    private SaleItemRepository saleItemRepository;

    @Mock
    private SaleStockPort saleStockPort;

    @Mock
    private StockReservationPort stockReservationPort;

    @InjectMocks
    private SaleService saleService;

    @Test
    void shouldAddProductToSaleSuccessfully() {

        Sale sale = new Sale();
        sale.setId(1L);
        sale.setEndTime(Instant.now().plusSeconds(3600));

        Product product = new Product();
        product.setId(100L);

        AddProductToSaleRequest request =
                new AddProductToSaleRequest(
                        100L,
                        BigDecimal.valueOf(500),
                        10
                );

        SaleItem saleItem = new SaleItem();

        when(saleRepository.findById(1L))
                .thenReturn(Optional.of(sale));

        when(productService.getProductOrThrow(100L))
                .thenReturn(product);

        when(saleItemRepository.save(any(SaleItem.class)))
                .thenReturn(saleItem);

        SaleItem result =
                saleService.addProductToSale(1L, request);

        assertNotNull(result);

        verify(saleItemRepository)
                .save(any(SaleItem.class));
    }

    @Test
    void shouldThrowWhenProductAlreadyInSale() {

        Sale sale = new Sale();
        sale.setId(1L);
        sale.setEndTime(Instant.now().plusSeconds(3600));

        Product product = new Product();
        product.setId(100L);

        AddProductToSaleRequest request =
                new AddProductToSaleRequest(
                        100L,
                        BigDecimal.valueOf(500),
                        10
                );

        when(saleRepository.findById(1L))
                .thenReturn(Optional.of(sale));

        when(productService.getProductOrThrow(100L))
                .thenReturn(product);

        when(saleItemRepository.save(any(SaleItem.class)))
                .thenThrow(DataIntegrityViolationException.class);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> saleService.addProductToSale(1L, request)
                );

        assertEquals(
                ErrorCode.PRODUCT_ALREADY_IN_SALE,
                exception.getErrorCode()
        );
    }

    @Test
    void shouldActivatePendingSale() {

        Sale sale = new Sale();
        sale.setId(1L);
        sale.setTitle("Flash Sale");
        sale.setStartTime(Instant.now());
        sale.setEndTime(
                Instant.now().plusSeconds(3600)
        );
        sale.setStatus(SaleStatus.PENDING);

        Product product = new Product();
        product.setId(100L);

        SaleItem item = new SaleItem();
        item.setProduct(product);
        item.setTotalStock(10);

        when(saleRepository.findById(1L))
                .thenReturn(Optional.of(sale));

        when(saleItemRepository.findAllBySaleId(1L))
                .thenReturn(List.of(item));

        saleService.activateSale(1L);

        assertEquals(SaleStatus.ACTIVE, sale.getStatus());

        verify(saleRepository)
                .save(sale);

        verify(saleStockPort)
                .initializeSaleStock(
                        eq(1L),
                        eq(100L),
                        eq(10),
                        anyLong()
                );

        verify(saleStockPort)
                .activateSale(eq(1L), anyLong());
    }

    @Test
    void shouldDeactivateActiveSale() {

        Sale sale = new Sale();
        sale.setId(1L);
        sale.setStatus(SaleStatus.ACTIVE);

        Product product = new Product();
        product.setId(100L);

        SaleItem item = new SaleItem();
        item.setProduct(product);

        when(saleRepository.findById(1L))
                .thenReturn(Optional.of(sale));

        when(saleItemRepository.findAllBySaleId(1L))
                .thenReturn(List.of(item));

        saleService.deactivateSale(1L);

        assertEquals(SaleStatus.ENDED, sale.getStatus());

        verify(saleRepository)
                .save(sale);

        verify(saleStockPort)
                .deactivateSale(1L, 100L);

        verify(saleStockPort)
                .deactivateSale(1L);
    }
}