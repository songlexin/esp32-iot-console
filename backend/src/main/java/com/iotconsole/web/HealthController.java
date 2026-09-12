package com.iotconsole.web;

import com.iotconsole.dto.HealthDto;
import com.iotconsole.mqtt.MqttBrokerClient;
import com.iotconsole.service.DeviceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final MqttBrokerClient mqtt;
    private final DeviceService devices;

    public HealthController(MqttBrokerClient mqtt, DeviceService devices) {
        this.mqtt = mqtt;
        this.devices = devices;
    }

    @GetMapping("/api/health")
    public HealthDto health() {
        return new HealthDto(
                "UP",
                mqtt.isConnected() ? "CONNECTED" : "DISCONNECTED",
                devices.countDevices(),
                devices.countOnline()
        );
    }
}
