package com.ci;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CIApplication {
    public static void main(String[] args) {
        SpringApplication.run(CIApplication.class, args);
    }
}
