package com.iotconsole.mqtt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqttTopicsTest {

    @Test
    void buildsCanonicalTopics() {
        assertThat(MqttTopics.status("esp32-sim-01")).isEqualTo("devices/esp32-sim-01/status");
        assertThat(MqttTopics.telemetry("esp32-sim-01")).isEqualTo("devices/esp32-sim-01/telemetry");
        assertThat(MqttTopics.command("esp32-sim-01")).isEqualTo("devices/esp32-sim-01/command");
    }

    @Test
    void parsesInboundTopics() {
        assertThat(MqttTopics.parse("devices/kitchen/status"))
                .hasValueSatisfying(parsed -> {
                    assertThat(parsed.deviceId()).isEqualTo("kitchen");
                    assertThat(parsed.isStatus()).isTrue();
                });
        assertThat(MqttTopics.parse("devices/kitchen/telemetry"))
                .hasValueSatisfying(parsed -> assertThat(parsed.isTelemetry()).isTrue());
        assertThat(MqttTopics.parse("sensors/kitchen/temp")).isEmpty();
        assertThat(MqttTopics.parse("devices/")).isEmpty();
    }
}
