package com.lms.auth_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MongoDebugLogger implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoDebugLogger.class);

    @Value("${spring.data.mongodb.uri:}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Override
    public void run(String... args) {
        log.atDebug()
                .addKeyValue("event.action", "mongo_connect")
                .addKeyValue("db.name", databaseName)
                .log("MongoDB configuration loaded");
    }
}
