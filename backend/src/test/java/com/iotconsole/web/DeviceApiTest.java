package com.iotconsole.web;

import com.iotconsole.domain.TelemetrySource;
import com.iotconsole.mqtt.MqttPayloads;
import com.iotconsole.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.mqtt.auto-connect=false",
        "spring.datasource.url=jdbc:h2:mem:iotapi;DB_CLOSE_DELAY=-1",
        "app.telemetry.history-limit=10"
})
@AutoConfigureMockMvc
class DeviceApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceService deviceService;

    @Test
    void healthIsUpWithoutBroker() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.mqtt").value("DISCONNECTED"));
    }

    @Test
    void unknownDeviceReturns404() throws Exception {
        mockMvc.perform(get("/api/devices/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Device not found: missing"));
    }

    @Test
    void telemetryRegistersDeviceAndHistory() throws Exception {
        deviceService.onTelemetry("esp32-test-01", new MqttPayloads.TelemetryPayload(
                23.4,
                55.0,
                false,
                -48,
                Instant.parse("2026-01-01T00:00:00Z"),
                TelemetrySource.SIMULATED
        ));
        deviceService.onStatus("esp32-test-01", new MqttPayloads.StatusPayload(true, "测试板"));

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("esp32-test-01"))
                .andExpect(jsonPath("$[0].name").value("测试板"))
                .andExpect(jsonPath("$[0].online").value(true))
                .andExpect(jsonPath("$[0].temperatureC", closeTo(23.4, 0.01)));

        mockMvc.perform(get("/api/devices/esp32-test-01/telemetry?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tempC", closeTo(23.4, 0.01)))
                .andExpect(jsonPath("$[0].source").value("SIMULATED"));

        mockMvc.perform(post("/api/devices/esp32-test-01/led")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"led\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ledOn").value(true));
    }
}
