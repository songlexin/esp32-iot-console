package com.iotconsole.mqtt;

import java.util.Optional;

public final class MqttTopics {

    public static final String STATUS_FILTER = "devices/+/status";
    public static final String TELEMETRY_FILTER = "devices/+/telemetry";

    private MqttTopics() {
    }

    public static String status(String deviceId) {
        return "devices/" + deviceId + "/status";
    }

    public static String telemetry(String deviceId) {
        return "devices/" + deviceId + "/telemetry";
    }

    public static String command(String deviceId) {
        return "devices/" + deviceId + "/command";
    }

    public static Optional<ParsedTopic> parse(String topic) {
        if (topic == null) {
            return Optional.empty();
        }
        String[] parts = topic.split("/");
        if (parts.length != 3 || !"devices".equals(parts[0]) || parts[1].isBlank()) {
            return Optional.empty();
        }
        String kind = parts[2];
        if (!"status".equals(kind) && !"telemetry".equals(kind) && !"command".equals(kind)) {
            return Optional.empty();
        }
        return Optional.of(new ParsedTopic(parts[1], kind));
    }

    public record ParsedTopic(String deviceId, String kind) {
        public boolean isStatus() {
            return "status".equals(kind);
        }

        public boolean isTelemetry() {
            return "telemetry".equals(kind);
        }
    }
}
