package ru.brombin.incident_retrieval_service.config;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RosbertaClientConfig {

    @Value("${huggingface.token}")
    String hfToken;

    @Value("${huggingface.rosberta.url}")
    String rosbertaUrl;

    @Bean
    public RestClient ruEnHuggingFaceRestClient() {
        if (hfToken == null || hfToken.isBlank()) {
            throw new IllegalStateException("huggingface token is not set");
        }

        return RestClient.builder()
                .baseUrl(rosbertaUrl)
                .defaultHeader("Authorization", "Bearer " + hfToken)
                .build();
    }
}
