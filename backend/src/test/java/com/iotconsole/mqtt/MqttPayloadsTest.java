package com.iotconsole.mqtt;

import com.iotconsole.domain.TelemetrySource;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MqttPayloadsTest {

    @Test
    void parsesPlainOnlineStatus() {
        assertThat(MqttPayloads.parseStatus("online").online()).isTrue();
        assertThat(MqttPayloads.parseStatus("offline").online()).isFalse();
        assertThat(MqttPayloads.parseStatus("").online()).isFalse();
    }

    @Test
    void parsesJsonStatusWithName() {
        MqttPayloads.StatusPayload status = MqttPayloads.parseStatus(
                "{\"state\":\"online\",\"name\":\"客厅开发板\"}"
        );
        assertThat(status.online()).isTrue();
        assertThat(status.name()).isEqualTo("客厅开发板");
    }

    @Test
    void parsesTelemetryFromFirmwareShape() {
        MqttPayloads.TelemetryPayload payload = MqttPayloads.parseTelemetry(
                "{\"tempC\":24.5,\"humidity\":61.2,\"led\":true,\"rssi\":-52,\"ts\":1700000000,\"source\":\"SENSOR\"}"
        );
        assertThat(payload.tempC()).isEqualTo(24.5);
        assertThat(payload.humidity()).isEqualTo(61.2);
        assertThat(payload.led()).isTrue();
        assertThat(payload.rssi()).isEqualTo(-52);
        assertThat(payload.source()).isEqualTo(TelemetrySource.SENSOR);
        assertThat(payload.ts()).isEqualTo(Instant.ofEpochSecond(1_700_000_000L));
    }

    @Test
    void treatsUnknownSourceAsSimulated() {
        MqttPayloads.TelemetryPayload payload = MqttPayloads.parseTelemetry(
                "{\"tempC\":21,\"humidity\":40,\"led\":false}"
        );
        assertThat(payload.source()).isEqualTo(TelemetrySource.SIMULATED);
        assertThat(payload.ts()).isNotNull();
    }

    @Test
    void commandJsonMatchesContract() {
        assertThat(MqttPayloads.commandJson(true)).isEqualTo("{\"led\":true}");
        assertThat(MqttPayloads.parseLedCommand("{\"led\":false}")).isFalse();
    }

    @Test
    void rejectsEmptyTelemetry() {
        assertThatThrownBy(() -> MqttPayloads.parseTelemetry(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
