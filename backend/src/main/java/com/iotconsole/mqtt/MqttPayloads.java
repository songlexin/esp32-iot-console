package com.iotconsole.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iotconsole.domain.TelemetrySource;

import java.time.Instant;
import java.util.Locale;

public final class MqttPayloads {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MqttPayloads() {
    }

    public record StatusPayload(boolean online, String name) {
    }

    public record TelemetryPayload(
            double tempC,
            double humidity,
            boolean led,
            Integer rssi,
            Instant ts,
            TelemetrySource source
    ) {
    }

    public static StatusPayload parseStatus(String payload) {
        String raw = payload == null ? "" : payload.trim();
        if (raw.isEmpty()) {
            return new StatusPayload(false, null);
        }
        if (looksLikeJson(raw)) {
            try {
                JsonNode node = MAPPER.readTree(raw);
                String state = text(node, "state", "status", "online");
                boolean online = parseOnline(state, node);
                String name = text(node, "name");
                return new StatusPayload(online, blankToNull(name));
            } catch (Exception ignored) {
                // Fall through to plain-text handling.
            }
        }
        return new StatusPayload(parseOnline(raw, null), null);
    }

    public static TelemetryPayload parseTelemetry(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Empty telemetry payload");
        }
        try {
            JsonNode node = MAPPER.readTree(payload);
            double tempC = number(node, 0, "tempC", "temperatureC", "temperature");
            double humidity = number(node, 0, "humidity", "hum", "rh");
            boolean led = bool(node, false, "led", "ledOn");
            Integer rssi = optionalInt(node, "rssi");
            Instant ts = timestamp(node);
            TelemetrySource source = source(node);
            return new TelemetryPayload(tempC, humidity, led, rssi, ts, source);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid telemetry JSON: " + payload, ex);
        }
    }

    public static String commandJson(boolean led) {
        return "{\"led\":" + led + "}";
    }

    public static Boolean parseLedCommand(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(payload);
            if (node.hasNonNull("led")) {
                return node.get("led").asBoolean();
            }
            if (node.hasNonNull("ledOn")) {
                return node.get("ledOn").asBoolean();
            }
        } catch (Exception ignored) {
            // ignore malformed command
        }
        return null;
    }

    private static boolean looksLikeJson(String raw) {
        return raw.startsWith("{") || raw.startsWith("\"");
    }

    private static boolean parseOnline(String state, JsonNode node) {
        if (node != null && node.has("online") && node.get("online").isBoolean()) {
            return node.get("online").asBoolean();
        }
        if (state == null) {
            return false;
        }
        String normalized = state.trim().toLowerCase(Locale.ROOT);
        return "online".equals(normalized) || "true".equals(normalized) || "1".equals(normalized);
    }

    private static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            if (node.hasNonNull(field) && node.get(field).isTextual()) {
                return node.get(field).asText();
            }
            if (node.hasNonNull(field) && node.get(field).isBoolean() && "online".equals(field)) {
                return node.get(field).asBoolean() ? "online" : "offline";
            }
        }
        return null;
    }

    private static double number(JsonNode node, double fallback, String... fields) {
        for (String field : fields) {
            if (node.hasNonNull(field) && node.get(field).isNumber()) {
                return node.get(field).asDouble();
            }
        }
        return fallback;
    }

    private static boolean bool(JsonNode node, boolean fallback, String... fields) {
        for (String field : fields) {
            if (node.hasNonNull(field)) {
                JsonNode value = node.get(field);
                if (value.isBoolean() || value.isNumber() || value.isTextual()) {
                    return value.asBoolean();
                }
            }
        }
        return fallback;
    }

    private static Integer optionalInt(JsonNode node, String field) {
        if (node.hasNonNull(field) && node.get(field).isNumber()) {
            return node.get(field).asInt();
        }
        return null;
    }

    private static Instant timestamp(JsonNode node) {
        if (node.hasNonNull("ts")) {
            JsonNode ts = node.get("ts");
            if (ts.isNumber()) {
                long value = ts.asLong();
                // Accept either epoch seconds or milliseconds.
                if (value < 1_000_000_000_000L) {
                    return Instant.ofEpochSecond(value);
                }
                return Instant.ofEpochMilli(value);
            }
            if (ts.isTextual()) {
                try {
                    return Instant.parse(ts.asText());
                } catch (Exception ignored) {
                    // use now
                }
            }
        }
        return Instant.now();
    }

    private static TelemetrySource source(JsonNode node) {
        if (node.hasNonNull("source") && node.get("source").isTextual()) {
            String raw = node.get("source").asText().trim().toUpperCase(Locale.ROOT);
            if ("SENSOR".equals(raw)) {
                return TelemetrySource.SENSOR;
            }
            if ("SIMULATED".equals(raw) || "SIM".equals(raw) || "FALLBACK".equals(raw)) {
                return TelemetrySource.SIMULATED;
            }
        }
        return TelemetrySource.SIMULATED;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
