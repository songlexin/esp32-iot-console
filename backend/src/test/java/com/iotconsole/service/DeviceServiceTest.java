package com.iotconsole.service;

import com.iotconsole.domain.TelemetrySource;
import com.iotconsole.dto.DeviceDto;
import com.iotconsole.mqtt.MqttPayloads;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.mqtt.auto-connect=false",
        "spring.datasource.url=jdbc:h2:mem:iotsvc;DB_CLOSE_DELAY=-1",
        "app.telemetry.history-limit=3"
})
class DeviceServiceTest {

    @Autowired
    private DeviceService deviceService;

    @Test
    void autoRegistersAndPrunesHistory() {
        for (int i = 0; i < 5; i++) {
            deviceService.onTelemetry("prune-me", new MqttPayloads.TelemetryPayload(
                    20 + i,
                    40,
                    i % 2 == 0,
                    -60,
                    Instant.parse("2026-02-01T00:00:0" + i + "Z"),
                    TelemetrySource.SENSOR
            ));
        }

        assertThat(deviceService.history("prune-me", 20)).hasSize(3);
        DeviceDto device = deviceService.getDevice("prune-me");
        assertThat(device.online()).isTrue();
        assertThat(device.temperatureC()).isEqualTo(24.0);
        assertThat(device.telemetrySource()).isEqualTo(TelemetrySource.SENSOR);
        assertThat(device.name()).isEqualTo("设备 prune-me");
    }

    @Test
    void statusCanRenameDevice() {
        deviceService.onStatus("named", new MqttPayloads.StatusPayload(true, "阳台"));
        assertThat(deviceService.getDevice("named").name()).isEqualTo("阳台");
    }
}
