package com.amazon.agenticworkstation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;

/**
 * Database configuration for the Agentic Workstation application
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.amazon.agenticworkstation.repository")
@EnableTransactionManagement
@ConfigurationProperties(prefix = "spring.datasource")
public class DatabaseConfig {
    
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    
    /**
     * Production DataSource configuration for PostgreSQL (Neon)
     */
    @Bean
    @Profile("prod")
    public DataSource productionDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(url != null ? url : "jdbc:postgresql://ep-tiny-flower-a1bscmef-pooler.ap-southeast-1.aws.neon.tech/agentic_workstation?sslmode=require")
                .username(username != null ? username : "neondb_owner")
                .password(password != null ? password : "npg_bzfN1C9jdJtL")
                .build();
    }
    
    /**
     * Development DataSource configuration for PostgreSQL (Neon)
     */
    @Bean
    @Profile({"dev", "test", "default"})
    public DataSource developmentDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url("jdbc:postgresql://ep-tiny-flower-a1bscmef-pooler.ap-southeast-1.aws.neon.tech/agentic_workstation?sslmode=require")
                .username("neondb_owner")
                .password("npg_bzfN1C9jdJtL")
                .build();
    }
    
    /**
     * Password encoder for secure password storage
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    // Getters and setters for configuration properties
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getDriverClassName() {
        return driverClassName;
    }
    
    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }
}