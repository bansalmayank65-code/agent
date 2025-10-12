package com.amazon.agenticworkstation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Service to initialize sample data in the database on startup
 */
@Component
public class DataInitializationService implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializationService.class);

    
    @Override
    public void run(String... args) throws Exception {
        // Automatic sample user initialization disabled
        logger.info("Sample data initialization is disabled");
    }

}