package com.flashsale.ordersystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlashSaleOrderSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlashSaleOrderSystemApplication.class, args);
    }

}
