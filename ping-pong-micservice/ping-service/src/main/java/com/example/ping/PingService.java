package com.example.ping;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

@Service
public class PingService {
    private static final Logger logger = LoggerFactory.getLogger(PingService.class);
    private static final AtomicInteger requestCount = new AtomicInteger(0);
    private final WebClient webClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MongoTemplate mongoTemplate;

    public PingService(WebClient webClient, KafkaTemplate<String, String> kafkaTemplate, MongoTemplate mongoTemplate) {
        this.webClient = webClient;
        this.kafkaTemplate = kafkaTemplate;
        this.mongoTemplate = mongoTemplate;
    }

    public void sendPingRequestPeriodically() {
        try {
            File file = new File("rate_limit.lock");
            FileOutputStream fos = new FileOutputStream(file);
            FileLock lock = fos.getChannel().tryLock();
            if (lock!= null) {
                if (requestCount.getAndIncrement() < 2) {
                    Mono<String> response = webClient.post()
                            .uri("/pong")
                            .contentType(MediaType.TEXT_PLAIN)
                            .bodyValue("Hello")
                            .retrieve()
                            .onStatus(HttpStatus::isError, clientResponse -> {
                                if (clientResponse.statusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                                    logger.info("Request send & Pong throttled it.");
                                    recordRequestInfo("Pong throttled", clientResponse.statusCode().value());
                                    sendKafkaMessage("Request throttled by Pong", clientResponse.statusCode().value());
                                }
                                return Mono.empty();
                            })
                            .bodyToMono(String.class);
                    response.subscribe(result -> {
                        logger.info("Request sent & Pong Respond: {}", result);
                        recordRequestInfo("Success", 200);
                        sendKafkaMessage("Request successful", 200);
                    }, error -> {
                        logger.error("Error sending request: {}", error.getMessage());
                        recordRequestInfo("Error", HttpStatus.INTERNAL_SERVER_ERROR.value());
                        sendKafkaMessage("Request error", HttpStatus.INTERNAL_SERVER_ERROR.value());
                    });
                } else {
                    requestCount.decrementAndGet();
                    logger.info("Request not send as being \"rate limited\".");
                    recordRequestInfo("Rate limited", HttpStatus.TOO_MANY_REQUESTS.value());
                    sendKafkaMessage("Request rate limited", HttpStatus.TOO_MANY_REQUESTS.value());
                }
                lock.release();
            }
        } catch (IOException e) {
            logger.error("Error in rate limiting or sending request: {}", e.getMessage());
            recordRequestInfo("System error", HttpStatus.INTERNAL_SERVER_ERROR.value());
            sendKafkaMessage("System error during request", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private void recordRequestInfo(String status, int code) {
        Map<String, Object> requestRecord = new HashMap<>();
        requestRecord.put("timestamp", new Date());
        requestRecord.put("status", status);
        requestRecord.put("httpCode", code);
        mongoTemplate.insert(requestRecord, "request_records");
    }

    private void sendKafkaMessage(String message, int code) {
        kafkaTemplate.send("ping_request_topic", message + " - Code: " + code);
    }
}
