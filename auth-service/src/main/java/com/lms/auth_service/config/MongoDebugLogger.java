package com.lms.auth_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MongoDebugLogger implements CommandLineRunner {
    @Value("${spring.data.mongodb.uri:}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Override
    public void run(String... args) {
        System.out.println(">>> spring.data.mongodb.uri = " + mongoUri);
        System.out.println(">>> spring.data.mongodb.database = " + databaseName);

    }
}
