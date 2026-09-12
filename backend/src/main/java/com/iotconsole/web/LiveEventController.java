package com.iotconsole.web;

import com.iotconsole.service.LiveEventHub;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class LiveEventController {

    private final LiveEventHub events;

    public LiveEventController(LiveEventHub events) {
        this.events = events;
    }

    @GetMapping(path = "/api/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        return events.subscribe();
    }
}
