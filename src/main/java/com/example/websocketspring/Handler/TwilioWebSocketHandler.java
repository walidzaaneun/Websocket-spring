package com.example.websocketspring.Handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import javax.sound.sampled.*;
import java.io.*;
import java.util.Base64;
import java.util.UUID;

@Component
public class TwilioWebSocketHandler extends BinaryWebSocketHandler {

    private AudioFormat ulawFormat;
    private AudioFormat pcmFormat;
    private ByteArrayOutputStream byteArrayOutputStream;
    private File wavFile;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("New WebSocket connection established: " + session.getId());
        String uniqueId = UUID.randomUUID().toString();
        String fileName = "caller_audio_" + uniqueId + ".wav";
        wavFile = new File("audio_files/" + fileName);
        wavFile.getParentFile().mkdirs();
        byteArrayOutputStream = new ByteArrayOutputStream();

        // Define source (μ-law) and target (PCM) audio formats
        ulawFormat = new AudioFormat(
                AudioFormat.Encoding.ULAW,
                8000.0f, // Sample Rate
                8,       // Sample Size in bits
                1,       // Channels
                1,       // Frame Size
                8000.0f, // Frame Rate
                false    // Big Endian (false for little-endian)
        );

        pcmFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                8000.0f, // Sample Rate
                16,      // Sample Size in bits
                1,       // Channels
                2,       // Frame Size
                8000.0f, // Frame Rate
                false    // Big Endian (false for little-endian)
        );
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(message.getPayload());
            String event = rootNode.path("event").asText();

            if ("media".equals(event)) {
                String payload = rootNode.path("media").path("payload").asText();
                if (!payload.isEmpty()) {
                    byte[] audioData = Base64.getDecoder().decode(payload);

                    // Log the size of the audio data received
                    System.out.println("Received audio data length: " + audioData.length);

                    // Write the raw μ-law audio data to the ByteArrayOutputStream
                    byteArrayOutputStream.write(audioData);
                }
            } else {
                System.out.println("Received non-media event: " + event);
            }
        } catch (Exception e) {
            System.err.println("Error processing WebSocket message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        System.out.println("WebSocket connection closed: " + session.getId());

        // Convert ByteArrayOutputStream to AudioInputStream in μ-law format
        byte[] ulawData = byteArrayOutputStream.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(ulawData);
        AudioInputStream ulawStream = new AudioInputStream(bais, ulawFormat, ulawData.length);

        // Convert μ-law AudioInputStream to PCM AudioInputStream
        AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcmFormat, ulawStream);

        // Write the PCM AudioInputStream to a .wav file
        try {
            AudioSystem.write(pcmStream, AudioFileFormat.Type.WAVE, wavFile);
            System.out.println("Audio saved to: " + wavFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving audio file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            ulawStream.close();
            pcmStream.close();
            bais.close();
            byteArrayOutputStream.close();
        }
    }
}
