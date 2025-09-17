package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Spring Boot 启动类：
 * - @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 * - @EnableConfigurationProperties 让 @ConfigurationProperties 生效
 */
@SpringBootApplication
public class FlowTrackServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlowTrackServerApplication.class, args);
    }
}