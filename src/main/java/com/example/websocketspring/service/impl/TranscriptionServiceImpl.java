package com.example.websocketspring.service.impl;

import com.example.websocketspring.service.interfaces.TranscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Service
public class TranscriptionServiceImpl implements TranscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptionServiceImpl.class);

    private final String transcriptionDirectory;

    public TranscriptionServiceImpl(@Value("${transcription.directory:transcriptions}") String transcriptionDirectory) {
        this.transcriptionDirectory = transcriptionDirectory;
    }

    /**
     * Saves the final transcription to a file.
     *
     * @param sessionId  The Twilio session ID.
     * @param transcript The transcribed text.
     */
    @Override
    public void saveTranscriptToFile(String sessionId, String transcript) {
        String fileName = "session_" + sessionId + "_transcription.txt";
        File transcriptionDir = new File(transcriptionDirectory);
        File transcriptionFile = new File(transcriptionDir, fileName);

        try {
            // Ensure the directory exists
            if (!transcriptionDir.exists()) {
                boolean dirCreated = transcriptionDir.mkdirs();
                if (dirCreated) {
                    logger.info("Created directory: {}", transcriptionDir.getAbsolutePath());
                } else {
                    logger.error("Failed to create directory: {}", transcriptionDir.getAbsolutePath());
                    return;
                }
            }

            // Write the transcription to the file
            try (FileWriter writer = new FileWriter(transcriptionFile, true)) {
                writer.write(transcript + System.lineSeparator());
                logger.info("Transcription saved to: {}", transcriptionFile.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Error writing transcription to file for session {}: {}", sessionId, e.getMessage(), e);
        }
    }
}