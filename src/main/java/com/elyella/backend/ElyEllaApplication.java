package com.elyella.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ElyEllaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElyEllaApplication.class, args);
    }
}
