package com.iotconsole.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class LiveEventHub {

    private static final Logger log = LoggerFactory.getLogger(LiveEventHub.class);

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public LiveEventHub(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("ready").data("{\"ok\":true}", MediaType.APPLICATION_JSON));
        } catch (IOException ignored) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    public void emit(String eventName, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("Failed to serialize live event {}", eventName, ex);
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(json, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                emitter.complete();
                emitters.remove(emitter);
            }
        }
    }

    public void deviceUpdated(Object device) {
        emit("device", Map.of("device", device));
    }

    public void telemetryUpdated(String deviceId, Object sample, Object device) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deviceId", deviceId);
        body.put("sample", sample);
        body.put("device", device);
        emit("telemetry", body);
    }

    public int subscriberCount() {
        return emitters.size();
    }

    @Scheduled(fixedRate = 15_000)
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception ex) {
                emitter.complete();
                emitters.remove(emitter);
            }
        }
    }
}
