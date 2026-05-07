package com.flashsale.ordersystem.sale;

import com.flashsale.ordersystem.sale.domain.enums.SaleStatus;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.sale.repository.SaleRepository;
import com.flashsale.ordersystem.sale.scheduler.SaleScheduler;
import com.flashsale.ordersystem.sale.service.SaleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleSchedulerTest {
    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SaleService saleService;

    @InjectMocks
    private SaleScheduler saleScheduler;

    @Test
    void shouldActivateScheduledSales() {
        Sale sale1 = new Sale();
        sale1.setId(1L);

        Sale sale2 = new Sale();
        sale2.setId(2L);

        when(saleRepository
                .findByStartTimeBeforeAndStatus(
                        any(),
                        eq(SaleStatus.PENDING)
                ))
                .thenReturn(List.of(sale1, sale2));

        when(saleRepository
                .findByEndTimeBeforeAndStatus(
                        any(),
                        eq(SaleStatus.ACTIVE)
                ))
                .thenReturn(List.of());

        saleScheduler.processSales();

        verify(saleService)
                .activateSale(1L);

        verify(saleService)
                .activateSale(2L);

        verify(saleService, never())
                .deactivateSale(any());
    }

    @Test
    void shouldExpireFinishedSales() {
        Sale sale1 = new Sale();
        sale1.setId(10L);

        Sale sale2 = new Sale();
        sale2.setId(20L);

        when(saleRepository
                .findByStartTimeBeforeAndStatus(
                        any(),
                        eq(SaleStatus.PENDING)
                ))
                .thenReturn(List.of());

        when(saleRepository
                .findByEndTimeBeforeAndStatus(
                        any(),
                        eq(SaleStatus.ACTIVE)
                ))
                .thenReturn(List.of(sale1, sale2));

        saleScheduler.processSales();

        verify(saleService)
                .deactivateSale(10L);

        verify(saleService)
                .deactivateSale(20L);

        verify(saleService, never())
                .activateSale(any());
    }

    @Test
    void shouldNotProcessSalesWhenNoMatchingSalesFound() {
        when(saleRepository
                .findByStartTimeBeforeAndStatus(any(), any()))
                .thenReturn(List.of());

        when(saleRepository
                .findByEndTimeBeforeAndStatus(any(), any()))
                .thenReturn(List.of());

        saleScheduler.processSales();

        verifyNoInteractions(saleService);
    }
}