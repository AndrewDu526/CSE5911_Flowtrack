package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Spring Boot
 * - @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 * - @EnableConfigurationProperties make @ConfigurationProperties works
 */
@SpringBootApplication
public class FlowTrackServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlowTrackServerApplication.class, args);
    }
}