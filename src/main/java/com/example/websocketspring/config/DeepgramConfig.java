package com.example.websocketspring.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class DeepgramConfig {
    @Value("${deepgram.api.key}")
    private String deepgramApiKey;

    @Value("${deepgram.model}")
    private String deepgramModel;
}
