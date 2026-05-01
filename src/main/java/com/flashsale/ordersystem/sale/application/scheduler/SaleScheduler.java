package com.flashsale.ordersystem.sale.application.scheduler;

import com.flashsale.ordersystem.order.application.port.StockService;
import com.flashsale.ordersystem.sale.domain.enums.SaleStatus;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.sale.domain.model.SaleItem;
import com.flashsale.ordersystem.sale.infrastructure.SaleItemRepository;
import com.flashsale.ordersystem.sale.infrastructure.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleScheduler {
    private final   SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final StockService stockService;
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void processSales(){
        LocalDateTime now = LocalDateTime.now();
        startSale(now);
        endSale(now);
    }

    private void startSale(LocalDateTime now){
        List<Sale> toStart = saleRepository
                .findByStartTimeBeforeAndStatus(now, SaleStatus.PENDING);
        for (Sale sale : toStart){
            activateSale(sale);
        }
    }

    private void activateSale(Sale sale){
        log.info("activating sale {}",sale.getId());
        List<SaleItem> items = saleItemRepository.findAllBySaleId(sale.getId());
        long ttl = java.time.Duration
                .between(LocalDateTime.now(), sale.getEndTime())
                .getSeconds();
        ttl = Math.max(ttl, 60);

        for (SaleItem item : items){
            stockService.initializeSaleStock(sale.getId(),
                    item.getProduct().getId(),
                    item.getTotalStock(),
                    ttl);
        }
        stockService.activateSale(sale.getId(),ttl);
        sale.setStatus(SaleStatus.ACTIVE);
        saleRepository.save(sale);
    }

    private  void  endSale(LocalDateTime now){
        List<Sale> toEnd = saleRepository
                .findByEndTimeBeforeAndStatus(now,SaleStatus.ACTIVE);
        for (Sale sale : toEnd){
            deactivateSale(sale);
        }
    }

    private void deactivateSale(Sale sale){
        log.info("Ending sale {}",sale.getId());

        List<SaleItem> items = saleItemRepository.findAllBySaleId(sale.getId());
        for (SaleItem item : items) {
            stockService.deactivateSale(sale.getId(),item.getProduct().getId());
        }
        stockService.deactivateSale(sale.getId());
        sale.setStatus(SaleStatus.ENDED);
        saleRepository.save(sale);
    }
}
