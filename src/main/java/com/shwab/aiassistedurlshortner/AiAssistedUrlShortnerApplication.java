package com.shwab.aiassistedurlshortner;

import com.shwab.aiassistedurlshortner.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(AppProperties.class)
public class AiAssistedUrlShortnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAssistedUrlShortnerApplication.class, args);
    }
}
