package com.iotconsole.dto;

import jakarta.validation.constraints.NotNull;

public record LedCommandRequest(@NotNull Boolean led) {
}
