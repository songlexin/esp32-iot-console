package com.iotconsole.dto;

public record HealthDto(
        String status,
        String mqtt,
        long devices,
        long online
) {
}
