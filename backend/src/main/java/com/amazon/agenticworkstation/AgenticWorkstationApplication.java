package com.amazon.agenticworkstation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgenticWorkstationApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgenticWorkstationApplication.class, args);
    }
}
