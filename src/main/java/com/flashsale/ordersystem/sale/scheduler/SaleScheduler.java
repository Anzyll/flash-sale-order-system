package com.flashsale.ordersystem.sale.scheduler;

import com.flashsale.ordersystem.sale.service.SaleService;
import com.flashsale.ordersystem.shared.port.SaleStockPort;
import com.flashsale.ordersystem.sale.domain.enums.SaleStatus;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.sale.domain.model.SaleItem;
import com.flashsale.ordersystem.sale.repository.SaleItemRepository;
import com.flashsale.ordersystem.sale.repository.SaleRepository;
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
    private final SaleService saleService;
    @Scheduled(fixedRate = 5000)
    public void processSales(){
        LocalDateTime now = LocalDateTime.now();
        startSale(now);
        endSale(now);
    }

    private void startSale(LocalDateTime now){
        List<Sale> toStart = saleRepository
                .findByStartTimeBeforeAndStatus(now, SaleStatus.PENDING);
        for (Sale sale : toStart){
            saleService.activateSale(sale.getId());
        }
    }

    private  void  endSale(LocalDateTime now){
        List<Sale> toEnd = saleRepository
                .findByEndTimeBeforeAndStatus(now,SaleStatus.ACTIVE);
        for (Sale sale : toEnd){
            saleService.deactivateSale(sale.getId());
        }
    }
}
