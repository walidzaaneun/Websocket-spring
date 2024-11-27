package com.example.websocketspring.Handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TwilioWebSocketHandler extends TextWebSocketHandler {

    // Replace with your actual Deepgram API Key
    private final String deepgramApiKey = "d250412c80dd33f93ad519c5ae7c41ef6e7579a5";

    // Mapping between Twilio session IDs and Deepgram clients
    private ConcurrentHashMap<String, DeepgramWebSocketClient> deepgramClients = new ConcurrentHashMap<>();

    // ObjectMapper instance for JSON processing
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("New Twilio WebSocket connection established: " + session.getId());

        // Initialize Deepgram WebSocket client for this session
        DeepgramWebSocketClient deepgramClient = initializeDeepgramClient(session.getId());
        if (deepgramClient != null) {
            deepgramClients.put(session.getId(), deepgramClient);
        }
    }

    private DeepgramWebSocketClient initializeDeepgramClient(String sessionId) {
        String deepgramUrl = "wss://api.deepgram.com/v1/listen?encoding=mulaw&sample_rate=8000&channels=1&model=nova-2";

        try {
            DeepgramWebSocketClient client = new DeepgramWebSocketClient(new URI(deepgramUrl), sessionId);
            client.addHeader("Authorization", "Token " + deepgramApiKey);
            client.connectBlocking();
            return client;
        } catch (Exception e) {
            System.err.println("Failed to connect to Deepgram for session " + sessionId + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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
                    DeepgramWebSocketClient deepgramClient = deepgramClients.get(session.getId());
                    if (deepgramClient != null && deepgramClient.isOpen()) {
                        deepgramClient.send(audioData);
                    } else {
                        System.err.println("Deepgram client is not available or not open for session: " + session.getId());
                    }
                }
            } else {
                System.out.println("Received non-media event: " + event);
            }
        } catch (Exception e) {
            System.err.println("Error processing Twilio WebSocket message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        System.out.println("Twilio WebSocket connection closed: " + session.getId());

        // Close the associated Deepgram WebSocket connection
        DeepgramWebSocketClient deepgramClient = deepgramClients.remove(session.getId());
        if (deepgramClient != null && deepgramClient.isOpen()) {
            deepgramClient.close();
        }
    }

    private class DeepgramWebSocketClient extends WebSocketClient {

        private final String sessionId;

        public DeepgramWebSocketClient(URI serverUri, String sessionId) {
            super(serverUri);
            this.sessionId = sessionId;
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            System.out.println("Connected to Deepgram for session: " + sessionId);
        }

        @Override
        public void onMessage(String message) {
            handleTranscriptionResult(sessionId, message);
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            System.out.println("Received binary message from Deepgram for session: " + sessionId);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("Deepgram WebSocket connection closed for session " + sessionId + ": " + reason);
        }

        @Override
        public void onError(Exception ex) {
            System.err.println("Error in Deepgram WebSocket connection for session " + sessionId + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void handleTranscriptionResult(String sessionId, String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            boolean isFinal = rootNode.path("is_final").asBoolean(false);
            JsonNode transcriptNode = rootNode.path("channel").path("alternatives").get(0).path("transcript");

            if (!transcriptNode.isMissingNode()) {
                String transcript = transcriptNode.asText().trim();
                if (!transcript.isEmpty()) {
                    if (isFinal) {
                        System.out.println("Final Transcription (Session " + sessionId + "): " + transcript);
                        // Save final transcript to file
                        saveTranscriptToFile(sessionId, transcript);
                    } else {
                        System.out.println("Partial Transcription (Session " + sessionId + "): " + transcript);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling transcription result for session " + sessionId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveTranscriptToFile(String sessionId, String transcript) {
        String directoryPath = "transcriptions";
        String fileName = directoryPath + File.separator + "session_" + sessionId + "_transcription.txt";

        try {
            // Ensure the directory exists
            File transcriptionDir = new File(directoryPath);
            if (!transcriptionDir.exists()) {
                boolean dirCreated = transcriptionDir.mkdirs();
                if (dirCreated) {
                    System.out.println("Created directory: " + transcriptionDir.getAbsolutePath());
                } else {
                    System.err.println("Failed to create directory: " + transcriptionDir.getAbsolutePath());
                    return;
                }
            }

            // Write the transcription to the file
            try (FileWriter writer = new FileWriter(fileName, true)) {
                writer.write(transcript + System.lineSeparator());
                System.out.println("Transcription saved to: " + fileName);
            }
        } catch (IOException e) {
            System.err.println("Error writing transcription to file for session " + sessionId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
