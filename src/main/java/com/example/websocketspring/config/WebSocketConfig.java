package com.example.websocketspring.config;


import com.example.websocketspring.handler.TwilioWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    final TwilioWebSocketHandler twilioWebSocketHandler;

    public WebSocketConfig(TwilioWebSocketHandler twilioWebSocketHandler) {
        this.twilioWebSocketHandler = twilioWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry.addHandler(twilioWebSocketHandler, "/audio")
                .setAllowedOrigins("*");

    }

}