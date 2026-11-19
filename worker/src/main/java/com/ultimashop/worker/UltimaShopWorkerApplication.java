package com.ultimashop.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UltimaShopWorkerApplication {
    public static void main(String[] args) { SpringApplication.run(UltimaShopWorkerApplication.class, args); }
}
