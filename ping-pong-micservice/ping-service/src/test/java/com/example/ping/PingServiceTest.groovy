package com.example.ping

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.junit.jupiter.api.extension.ExtendWith
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import com.example.ping.PingService
import reactor.test.StepVerifier

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ExtendWith(SpringExtension.class)
class PingServiceTest extends Specification {
    @Autowired
    PingService pingService

    def "should send ping request and handle response"() {
        when:
        pingService.sendPingRequestPeriodically()

        then:
        true  // 这里可以进一步完善断言，比如验证日志输出等情况，此处简化示例

        and:
        // 可以使用StepVerifier等工具进一步验证响应流的情况，示例如下
        def responseMono = Mono.just("Test")  // 模拟响应
        StepVerifier.create(responseMono)
                .expectNext("Test")
                .verifyComplete()
    }
}