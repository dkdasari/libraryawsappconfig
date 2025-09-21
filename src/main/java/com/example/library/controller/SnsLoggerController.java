package com.example.library.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URL;
import java.util.Map;


@RestController
@RequestMapping("/internal/sns-logger-update")
@Slf4j
public class SnsLoggerController {

    private final LoggingSystem loggingSystem;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SnsLoggerController(LoggingSystem loggingSystem) {
        this.loggingSystem = loggingSystem;
    }

    @PostMapping(consumes = {"application/json", "text/plain"})
    public ResponseEntity<String> receiveFromSns(@RequestBody String rawBody,
                                                 @RequestHeader Map<String, String> headers) {
        log.info("Received raw SNS notification: {}", rawBody);

        Map<String, Object> body;
        try {
            body = objectMapper.readValue(rawBody, new TypeReference<>() {});
        } catch (IOException e) {
            log.error("Failed to parse SNS message body", e);
            return ResponseEntity.badRequest().body("Invalid JSON");
        }

        String messageType = headers.get("x-amz-sns-message-type");
        if ("SubscriptionConfirmation".equalsIgnoreCase(messageType)) {
            String subscribeUrl = (String) body.get("SubscribeURL");
            confirmSubscription(subscribeUrl);
            return ResponseEntity.ok("Confirmed");
        }

        if ("Notification".equalsIgnoreCase(messageType)) {
            String message = (String) body.get("Message");
            applyLoggerConfig(message);
        }

        return ResponseEntity.ok("Processed");
    }

    private void confirmSubscription(String url) {
        try {
            new URL(url).openStream().close();
            log.info("SNS subscription confirmed");
        } catch (IOException e) {
            log.error("Failed to confirm subscription", e);
        }
    }

    private void applyLoggerConfig(String message) {
        // Assume message = {"com.example.package":"DEBUG"}
        try {
            Map<String, String> config = objectMapper.readValue(message, new TypeReference<>() {});
            config.forEach((loggerName, level) -> {
                loggingSystem.setLogLevel(loggerName, LogLevel.valueOf(level));
                log.info("Logger {} updated to {}", loggerName, level);
            });
        } catch (Exception e) {
            log.error("Failed to apply logger config", e);
        }
    }
}


