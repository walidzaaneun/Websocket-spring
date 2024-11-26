package com.example.websocketspring.config;


import com.example.websocketspring.Handler.AudioWebSocketHandler;
import com.example.websocketspring.Handler.TwilioWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
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
//        registry.addHandler(myHandler(), "/audio-recorder")
//                .setAllowedOrigins("*"); // Add front-end origin
        registry.addHandler(twilioWebSocketHandler, "/audio")
                .setAllowedOrigins("*");

    }
//    @Bean
//    public WebSocketHandler myHandler() {
//        return new AudioWebSocketHandler();
//    }
}