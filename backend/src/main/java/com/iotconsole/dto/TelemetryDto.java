package com.iotconsole.dto;

import com.iotconsole.domain.TelemetrySample;
import com.iotconsole.domain.TelemetrySource;

import java.time.Instant;

public record TelemetryDto(
        double tempC,
        double humidity,
        boolean led,
        Integer rssi,
        TelemetrySource source,
        Instant ts
) {
    public static TelemetryDto from(TelemetrySample sample) {
        return new TelemetryDto(
                sample.getTempC(),
                sample.getHumidity(),
                sample.isLedOn(),
                sample.getRssi(),
                sample.getSource(),
                sample.getTs()
        );
    }
}
