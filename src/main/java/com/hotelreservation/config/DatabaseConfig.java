package com.hotelreservation.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.hotelreservation.repository")
@EntityScan(basePackages = "com.hotelreservation.model")
public class DatabaseConfig {
    // Configuration is handled by annotations
}
