package com.example.websocketspring.handler;

import com.example.websocketspring.client.DeepgramWebSocketClient;
import com.example.websocketspring.config.DeepgramConfig;
import com.example.websocketspring.service.impl.TranscriptionServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TwilioWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(TwilioWebSocketHandler.class);

    private final DeepgramConfig deepgramConfig;
    private final TranscriptionServiceImpl transcriptionService;

    // Mapping between Twilio session IDs and Deepgram clients
    private final ConcurrentHashMap<String, DeepgramWebSocketClient> deepgramClients = new ConcurrentHashMap<>();

    // ObjectMapper instance for JSON processing
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public TwilioWebSocketHandler(DeepgramConfig deepgramConfig, TranscriptionServiceImpl transcriptionService) {
        this.deepgramConfig = deepgramConfig;
        this.transcriptionService = transcriptionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = UUID.randomUUID().toString(); // Generate a unique session ID
        session.getAttributes().put("sessionId", sessionId); // Store sessionId as an attribute
        logger.info("New Twilio WebSocket connection established: {}", sessionId);

        // Initialize Deepgram WebSocket client for this session
        DeepgramWebSocketClient deepgramClient = initializeDeepgramClient(sessionId);
        if (deepgramClient != null) {
            deepgramClients.put(sessionId, deepgramClient);
        } else {
            logger.error("Failed to initialize Deepgram client for session: {}", sessionId);
        }
    }

    /**
     * Initializes a Deepgram WebSocket client and connects to Deepgram's API.
     *
     * @param sessionId The unique identifier for the Twilio session.
     * @return An instance of DeepgramWebSocketClient if connection is successful, null otherwise.
     */
    private DeepgramWebSocketClient initializeDeepgramClient(String sessionId) {
        String deepgramUrl = "wss://api.deepgram.com/v1/listen?encoding=mulaw&sample_rate=8000&channels=1&model="+deepgramConfig.getDeepgramModel();

        try {
            DeepgramWebSocketClient client = new DeepgramWebSocketClient(new URI(deepgramUrl), sessionId, transcriptionService);
            client.addHeader("Authorization", "Token " + deepgramConfig.getDeepgramApiKey());
            client.connectBlocking();
            return client;
        } catch (URISyntaxException e) {
            logger.error("Invalid Deepgram URI: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error("Deepgram client connection interrupted for session {}: {}", sessionId, e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Failed to connect to Deepgram for session {}: {}", sessionId, e.getMessage(), e);
        }

        return null;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode rootNode = objectMapper.readTree(message.getPayload());
            String event = rootNode.path("event").asText();

            if ("media".equals(event)) {
                String payload = rootNode.path("media").path("payload").asText();
                if (!payload.isEmpty()) {
                    byte[] audioData = Base64.getDecoder().decode(payload);

                    // Retrieve the associated Deepgram client
                    String sessionId = getSessionId(session);
                    if (sessionId == null) {
                        logger.error("Session ID is null for session: {}", session);
                        return;
                    }

                    DeepgramWebSocketClient deepgramClient = deepgramClients.get(sessionId);
                    if (deepgramClient != null && deepgramClient.isOpen()) {
                        deepgramClient.sendAudio(audioData);
                    } else {
                        logger.error("Deepgram client is not available or not open for session: {}", sessionId);
                    }
                }
            } else {
                logger.info("Received non-media event: {}", event);
            }
        } catch (Exception e) {
            logger.error("Error processing Twilio WebSocket message: {}", e.getMessage(), e);
        }
    }

    /**
     * Retrieves the session ID associated with the WebSocketSession.
     *
     * @param session The WebSocketSession instance.
     * @return The session ID as a String, or null if not found.
     */
    private String getSessionId(WebSocketSession session) {
        Object sessionIdObj = session.getAttributes().get("sessionId");
        if (sessionIdObj instanceof String) {
            return (String) sessionIdObj;
        } else {
            logger.error("Session ID is missing or invalid for session: {}", session);
            return null;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = getSessionId(session);
        if (sessionId != null) {
            logger.info("Twilio WebSocket connection closed: {}", sessionId);

            // Close the associated Deepgram WebSocket connection
            DeepgramWebSocketClient deepgramClient = deepgramClients.remove(sessionId);
            if (deepgramClient != null && deepgramClient.isOpen()) {
                deepgramClient.close();
                logger.info("Closed Deepgram WebSocket connection for session: {}", sessionId);
            }
        } else {
            logger.warn("Twilio WebSocket connection closed without a valid session ID.");
        }
    }
}