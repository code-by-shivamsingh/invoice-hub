
package com.jokati.invoice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")   // or .allowedOrigins("*") for SB < 2.4
                        .allowedMethods("*")          // VERY IMPORTANT: includes DELETE + OPTIONS
                        .allowedHeaders("*")
                        .exposedHeaders("*")
                        .allowCredentials(false)      // set true only if you need cookies/Authorization with specific origins
                        .maxAge(3600);
            }
        };
    }
}
