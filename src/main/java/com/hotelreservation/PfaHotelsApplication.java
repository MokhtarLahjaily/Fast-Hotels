package com.hotelreservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PfaHotelsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PfaHotelsApplication.class, args);
    }
}
