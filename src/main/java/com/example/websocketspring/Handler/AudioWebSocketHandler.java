package com.example.websocketspring.Handler;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class AudioWebSocketHandler extends BinaryWebSocketHandler {

    private static final int CHUNK_DURATION_SECONDS = 2;
    private static final AtomicInteger fileCounter = new AtomicInteger();
    private ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
    private long startTime;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        startTime = System.currentTimeMillis();
        System.out.println("WebSocket connection established: " + session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException, UnsupportedAudioFileException {
        ByteBuffer payload = message.getPayload();
        byte[] bytes = new byte[payload.remaining()];
        System.out.println(bytes.length);
        payload.get(bytes);
        audioBuffer.write(bytes);

        long elapsedMillis = System.currentTimeMillis() - startTime;

        if (elapsedMillis >= CHUNK_DURATION_SECONDS * 1000) {
            byte[] audioData = audioBuffer.toByteArray();
            saveAsWav(audioData);

            audioBuffer.reset();
            startTime = System.currentTimeMillis();
        }
    }

    private void saveAsWav(byte[] audioData) throws UnsupportedAudioFileException, IOException {
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
             AudioInputStream audioInputStream = new AudioInputStream(bais, format, -1)) {
            String fileName = "audio_chunk_" + fileCounter.incrementAndGet() + ".wav";
            File wavFile = new File(fileName);
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavFile);

            System.out.println("Saved WAV file: " + fileName);

        } catch (IOException e) {
            System.err.println("Error saving WAV file: " + e.getMessage());
            e.printStackTrace();
        }

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("WebSocket connection closed: " + session.getId());
    }
}