package com.example.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/actuator/loggers")
@RequiredArgsConstructor
public class LoggerUpdateController {

    private final SnsClient snsClient;
    private final String topicArn = "arn:aws:sns:ap-south-1:634898291688:LoggerUpdateTopic";

    @PostMapping("/{loggerName}")
    public ResponseEntity<Void> updateLogger(@PathVariable String loggerName,
                                             @RequestBody Map<String, String> body) {
        log.info("Received SNS notification to overridden controller: {}", loggerName);
        log.info("Received SNS notification to overridden controller: {}", body);
        String level = body.get("configuredLevel");

        Map<String, String> config = Map.of(loggerName, level);
        try {
            log.info("=====publishing notification==========");
            snsClient.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(new ObjectMapper().writeValueAsString(config))
                    .build());
            log.info("=====publishing notification success==========");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

