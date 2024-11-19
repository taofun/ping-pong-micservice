package com.example.pong.router;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import com.example.pong.PongService;

@Configuration
public class PongRouter {
    private final PongService pongService;

    public PongRouter(PongService pongService) {
        this.pongService = pongService;
    }

    @Bean
    public RouterFunction<ServerResponse> route() {
        return RouterFunctions.route(RequestPredicates.POST("/pong")
                        .and(RequestPredicates.accept(MediaType.TEXT_PLAIN)),
                pongService::handlePingRequest);
    }
}