package com.spendsmart.recurring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling activates Spring's @Scheduled annotation.
// Without this, the @Scheduled method in RecurringServiceImpl
@SpringBootApplication
@EnableScheduling
@EnableFeignClients
public class RecurringServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecurringServiceApplication.class, args);
    }
}
