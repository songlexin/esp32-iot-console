package com.iotconsole.dto;

import com.iotconsole.domain.Device;
import com.iotconsole.domain.TelemetrySource;

import java.time.Duration;
import java.time.Instant;

public record DeviceDto(
        String id,
        String name,
        boolean online,
        Instant lastSeen,
        boolean ledOn,
        Double temperatureC,
        Double humidity,
        Integer rssi,
        TelemetrySource telemetrySource,
        Long secondsAgo
) {
    public static DeviceDto from(Device device) {
        Instant lastSeen = device.getLastSeen();
        Long secondsAgo = lastSeen == null
                ? null
                : Math.max(0, Duration.between(lastSeen, Instant.now()).getSeconds());
        return new DeviceDto(
                device.getId(),
                device.getName(),
                device.isOnline(),
                lastSeen,
                device.isLedOn(),
                device.getLastTempC(),
                device.getLastHumidity(),
                device.getLastRssi(),
                device.getLastSource(),
                secondsAgo
        );
    }
}
