package com.jokati.invoice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jokati.invoice.email.EmailTemplates;

@Configuration
public class EmailConfig {

    @Bean
    public EmailTemplates emailTemplates() {
        return new EmailTemplates();
    }
}
