package com.example.pong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

@Service
public class PongService {
    private static final Logger logger = LoggerFactory.getLogger(PongService.class);
    private final ConcurrentHashMap<Integer, AtomicInteger> requestCountPerSecond = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();
    private final MongoTemplate mongoTemplate;

    public PongService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Mono<ServerResponse> handlePingRequest(ServerRequest request) {
        int currentSecond = (int) Instant.now(clock).getEpochSecond();
        AtomicInteger count = requestCountPerSecond.computeIfAbsent(currentSecond, k -> new AtomicInteger(0));
        if (count.getAndIncrement() < 1) {
            recordRequestProcessInfo("Accepted", 200);
            return ServerResponse.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(BodyInserters.fromValue("World"));
        } else {
            logger.info("Throttling request as limit reached for this second.");
            recordRequestProcessInfo("Throttled", HttpStatus.TOO_MANY_REQUESTS.value());
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
    }

    private void recordRequestProcessInfo(String status, int code) {
        Query query = query(where("timestamp").is(Instant.now(clock).toEpochMilli()));
        Update update = update("requestsProcessed", status).set("httpCode", code);
        mongoTemplate.upsert(query, update, "pong_request_records");
    }
}