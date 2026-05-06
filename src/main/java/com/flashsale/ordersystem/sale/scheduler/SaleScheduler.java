package com.flashsale.ordersystem.sale.scheduler;

import com.flashsale.ordersystem.sale.service.SaleService;
import com.flashsale.ordersystem.sale.domain.enums.SaleStatus;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.sale.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleScheduler {
    private final   SaleRepository saleRepository;
    private final SaleService saleService;
    @Scheduled(fixedRate = 5000)
    public void processSales(){
        Instant now = Instant.now();
        startSale(now);
        endSale(now);
    }

    private void startSale(Instant now){
        List<Sale> toStart = saleRepository
                .findByStartTimeBeforeAndStatus(now, SaleStatus.PENDING);
        for (Sale sale : toStart){
            saleService.activateSale(sale.getId());
        }
    }

    private  void  endSale(Instant now){
        List<Sale> toEnd = saleRepository
                .findByEndTimeBeforeAndStatus(now,SaleStatus.ACTIVE);
        for (Sale sale : toEnd){
            saleService.deactivateSale(sale.getId());
        }
    }
}
