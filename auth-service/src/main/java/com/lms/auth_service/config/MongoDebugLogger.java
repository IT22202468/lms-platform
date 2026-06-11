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

    @Override
    public void run(String... args) {
        log.atDebug()
                .addKeyValue("event.action", "mongo_connect")
                .addKeyValue("db.name", extractDbName(mongoUri))
                .log("MongoDB configuration loaded");
    }

    private String extractDbName(String uri) {
        if (uri == null || uri.isBlank()) return "unknown";
        int dbStart = uri.indexOf('/', uri.indexOf("//") + 2);
        if (dbStart < 0) return "unknown";
        int dbEnd = uri.indexOf('?', dbStart);
        return (dbEnd > dbStart) ? uri.substring(dbStart + 1, dbEnd) : uri.substring(dbStart + 1);
    }
}
