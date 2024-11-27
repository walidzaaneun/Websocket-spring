package com.example.websocketspring.service.interfaces;


public interface TranscriptionService {

    public void saveTranscriptToFile(String sessionId, String transcript);
}
