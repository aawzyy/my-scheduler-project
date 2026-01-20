package com.personal.scheduler_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Terapkan aturan CORS untuk SEMUA endpoint (/**)
        // Setting ini harus sinkron dengan SecurityConfig
        registry.addMapping("/**")
                .allowedOrigins(
                    "http://localhost:8081",
                    "http://localhost:5173",
                    "http://18.142.145.100",
                    "https://aawzyy.my.id",
                    "https://www.aawzyy.my.id"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}