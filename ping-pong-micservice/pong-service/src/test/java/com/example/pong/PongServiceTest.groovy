package com.example.pong

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.junit.jupiter.api.extension.ExtendWith
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import com.example.pong.PongService
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ExtendWith(SpringExtension.class)
class PongServiceTest extends Specification {

    @Autowired
    PongService pongService

    def "should handle ping request successfully within rate limit"() {
        given:
        ServerRequest serverRequest = mock(ServerRequest.class)
        when(serverRequest.method()).thenReturn(Mono.just("POST"))
        when(serverRequest.uri()).thenReturn(Mono.just(new URI("/pong")))

        when:
        def responseMono = pongService.handlePingRequest(serverRequest)

        then:
        StepVerifier.create(responseMono)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().is2xxSuccessful())
                .verifyComplete()
    }

    def "should throttle ping request when rate limit is reached"() {
        given:
        ServerRequest serverRequest = mock(ServerRequest.class)
        when(serverRequest.method()).thenReturn(Mono.just("POST"))
        when(serverRequest.uri()).thenReturn(Mono.just(new URI("/pong")))

        // 模拟达到限流条件（这里通过修改内部计数逻辑来模拟，实际可能需要更复杂的手段）
        def currentSecond = (int) (System.currentTimeMillis() / 1000)
        pongService.requestCountPerSecond.computeIfAbsent(currentSecond, { k -> new AtomicInteger(1) })

        when:
        def responseMono = pongService.handlePingRequest(serverRequest)

        then:
        StepVerifier.create(responseMono)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 429)
                .verifyComplete()
    }
}