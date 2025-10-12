package com.amazon.agenticworkstation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Tau Bench API integration
 */
@Configuration
@ConfigurationProperties(prefix = "tau.bench")
public class TauBenchConfig {
    
    private Api api = new Api();
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    public Api getApi() {
        return api;
    }
    
    public void setApi(Api api) {
        this.api = api;
    }
    
    public static class Api {
        private String baseUrl = "https://tau-bench.turing.com";
        private int connectTimeout = 30000; // 30 seconds
        private int readTimeout = 300000;   // 5 minutes
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public int getConnectTimeout() {
            return connectTimeout;
        }
        
        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }
        
        public int getReadTimeout() {
            return readTimeout;
        }
        
        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }
    }
}