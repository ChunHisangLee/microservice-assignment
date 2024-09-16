package com.jack.outboxservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OutboxServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OutboxServiceApplication.class, args);
    }

}
