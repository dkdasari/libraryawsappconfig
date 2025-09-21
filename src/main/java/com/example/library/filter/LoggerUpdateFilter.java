package com.example.library.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoggerUpdateFilter implements Filter {

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String topicArn = "arn:aws:sns:ap-south-1:634898291688:LoggerUpdateTopic";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Wrap the request to cache the input stream for multiple reads
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);

        if ("POST".equalsIgnoreCase(wrappedRequest.getMethod()) && wrappedRequest.getRequestURI().startsWith("/actuator/loggers/")) {
            // Read the cached body from wrapper
            String body = getRequestBody(wrappedRequest);

            String loggerName = wrappedRequest.getRequestURI().substring("/actuator/loggers/".length());
            log.info("Intercepted logger update for loggerName: {}, body: {}", loggerName, body);

            try {
                Map<String, String> bodyMap = objectMapper.readValue(body, Map.class);
                String level = bodyMap.get("configuredLevel");
                if (level != null) {
                    Map<String, String> snsMessage = Map.of(loggerName, level);
                    String messageJson = objectMapper.writeValueAsString(snsMessage);

                    snsClient.publish(PublishRequest.builder()
                            .topicArn(topicArn)
                            .message(messageJson)
                            .build());
                    log.info("Published logger update to SNS: {}", messageJson);
                } else {
                    log.warn("configuredLevel missing in request body");
                }
            } catch (Exception e) {
                log.error("Failed to process logger update in filter", e);
                // Decide whether to fail or continue; here we continue
            }
        }

        // Important: pass the wrapped request to the filter chain
        chain.doFilter(wrappedRequest, response);
    }

    // Helper to read cached request body from ContentCachingRequestWrapper
    private String getRequestBody(ContentCachingRequestWrapper request) throws IOException {
        // Read from the cached content byte array
        byte[] buf = request.getContentAsByteArray();
        if (buf.length > 0) {
            return new String(buf, 0, buf.length, StandardCharsets.UTF_8);
        }

        // If content not cached yet, read manually (first time)
        BufferedReader reader = request.getReader();
        return reader.lines().collect(Collectors.joining(System.lineSeparator()));
    }
}
