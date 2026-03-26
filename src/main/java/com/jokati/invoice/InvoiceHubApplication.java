package com.jokati.invoice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing; // <--- add this
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableMongoAuditing   // <--- enable auditing
public class InvoiceHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(InvoiceHubApplication.class, args);
    }
}