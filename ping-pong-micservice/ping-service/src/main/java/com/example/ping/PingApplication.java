package com.example.ping;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
public class PingApplication {
    private final PingService pingService;

    public PingApplication(PingService pingService) {
        this.pingService = pingService;
    }

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(PingApplication.class, args);
        PingService pingService = context.getBean(PingService.class);

        // 使用线程池来模拟多个JVM进程的效果（实际可以通过启动多个实例来实现）
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);  // 启动5个模拟进程
        for (int i = 0; i < 5; i++) {
            executorService.scheduleAtFixedRate(() -> pingService.sendPingRequestPeriodically(), 0, 1, TimeUnit.SECONDS);
        }
    }
}
