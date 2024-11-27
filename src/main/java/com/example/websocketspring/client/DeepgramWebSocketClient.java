package com.example.websocketspring.client;

import com.example.websocketspring.service.impl.TranscriptionServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeepgramWebSocketClient extends WebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(DeepgramWebSocketClient.class);

    private final String sessionId;
    private final TranscriptionServiceImpl transcriptionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    public DeepgramWebSocketClient(URI serverUri, String sessionId, TranscriptionServiceImpl transcriptionService) {
        super(serverUri);
        this.sessionId = sessionId;
        this.transcriptionService = transcriptionService;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        isConnected.set(true);
        logger.info("Connected to Deepgram for session: {}", sessionId);
    }

    @Override
    public void onMessage(String message) {
        handleTranscriptionResult(message);
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        logger.warn("Received binary message from Deepgram for session: {}", sessionId);
        // Handle binary messages if Deepgram sends any
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        isConnected.set(false);
        logger.info("Deepgram WebSocket connection closed for session {}: {}", sessionId, reason);
    }

    @Override
    public void onError(Exception ex) {
        logger.error("Error in Deepgram WebSocket connection for session {}: {}", sessionId, ex.getMessage(), ex);
    }

    /**
     * Sends audio data to Deepgram if the connection is open.
     *
     * @param audioData The raw audio data bytes.
     */
    public void sendAudio(byte[] audioData) {
        if (isConnected.get()) {
            this.send(audioData);
            logger.debug("Sent audio data to Deepgram for session: {}", sessionId);
        } else {
            logger.error("Cannot send audio data. Deepgram client is not connected for session: {}", sessionId);
        }
    }

    /**
     * Handles transcription results received from Deepgram.
     *
     * @param message The JSON message received from Deepgram.
     */
    private void handleTranscriptionResult(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            boolean isFinal = rootNode.path("is_final").asBoolean(false);
            JsonNode transcriptNode = rootNode.path("channel").path("alternatives").get(0).path("transcript");

            if (!transcriptNode.isMissingNode()) {
                String transcript = transcriptNode.asText().trim();
                if (!transcript.isEmpty()) {
                    if (isFinal) {
                        logger.info("Final Transcription (Session {}): {}", sessionId, transcript);
                        // Save final transcript to file
                        transcriptionService.saveTranscriptToFile(sessionId, transcript);
                    } else {
                        logger.debug("Partial Transcription (Session {}): {}", sessionId, transcript);
                        // Optionally handle partial transcripts
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error handling transcription result for session {}: {}", sessionId, e.getMessage(), e);
        }
    }
}