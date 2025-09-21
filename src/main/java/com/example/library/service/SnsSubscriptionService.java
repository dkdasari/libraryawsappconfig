package com.example.library.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

@Service
@Slf4j
public class SnsSubscriptionService {

    private final SnsClient snsClient;
    private final String topicArn = "arn:aws:sns:ap-south-1:634898291688:LoggerUpdateTopic";

    private String subscriptionArn;

    public SnsSubscriptionService(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void subscribe() {
        try {
            String endpoint = "http://" + getPublicIp() + ":8080/internal/sns-logger-update";

            SubscribeResponse response = snsClient.subscribe(SubscribeRequest.builder()
                    .topicArn(topicArn)
                    .protocol("http") // or "https" if SSL enabled
                    .endpoint(endpoint)
                    .returnSubscriptionArn(true)
                    .build());

            subscriptionArn = response.subscriptionArn();
            log.info("Subscribed instance to SNS topic: {}", subscriptionArn);
        } catch (Exception e) {
            log.error("Failed to subscribe to SNS", e);
        }
    }

    @PreDestroy
    public void unsubscribe() {
        try {
            if (subscriptionArn != null) {
                snsClient.unsubscribe(UnsubscribeRequest.builder()
                        .subscriptionArn(subscriptionArn)
                        .build());
                log.info("Unsubscribed instance from SNS");
            }
        } catch (Exception e) {
            log.error("Failed to unsubscribe from SNS", e);
        }
    }

    private String getPublicIp() throws IOException {
        // Works on EC2; if ECS, use task metadata endpoint
        return new BufferedReader(new InputStreamReader(
                new URL("http://169.254.169.254/latest/meta-data/public-ipv4").openStream()
        )).readLine();
    }
}

