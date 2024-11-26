package com.example.websocketspring.controller;

import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Say;
import com.twilio.twiml.voice.Start;
import com.twilio.twiml.voice.Stream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("twilio")
public class TwilioController {

    @PostMapping("/voice")
    public String handleVoice() {
        Stream stream = new Stream.Builder()
                .name("WebSocket for call")
                .url("wss://a8d9-160-177-220-83.ngrok-free.app/audio")
                .build();

        Start start = new Start.Builder().stream(stream).build();

        VoiceResponse response = new VoiceResponse.Builder()
                .say(new Say.Builder("Please start speaking ya rbk").build())
                .start(start)
                .build();

        return response.toXml();
    }
}
