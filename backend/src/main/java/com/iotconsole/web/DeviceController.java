package com.iotconsole.web;

import com.iotconsole.dto.DeviceDto;
import com.iotconsole.dto.LedCommandRequest;
import com.iotconsole.dto.TelemetryDto;
import com.iotconsole.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<DeviceDto> list() {
        return deviceService.listDevices();
    }

    @GetMapping("/{id}")
    public DeviceDto get(@PathVariable String id) {
        return deviceService.getDevice(id);
    }

    @PostMapping("/{id}/led")
    public DeviceDto setLed(@PathVariable String id, @Valid @RequestBody LedCommandRequest request) {
        return deviceService.setLed(id, request.led());
    }

    @GetMapping("/{id}/telemetry")
    public List<TelemetryDto> history(
            @PathVariable String id,
            @RequestParam(defaultValue = "60") int limit
    ) {
        return deviceService.history(id, limit);
    }
}
