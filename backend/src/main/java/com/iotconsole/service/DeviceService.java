package com.iotconsole.service;

import com.iotconsole.domain.Device;
import com.iotconsole.domain.TelemetrySample;
import com.iotconsole.domain.TelemetrySource;
import com.iotconsole.dto.DeviceDto;
import com.iotconsole.dto.TelemetryDto;
import com.iotconsole.mqtt.CommandPublisher;
import com.iotconsole.mqtt.MqttPayloads;
import com.iotconsole.repo.DeviceRepository;
import com.iotconsole.repo.TelemetryRepository;
import com.iotconsole.web.DeviceNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceRepository devices;
    private final TelemetryRepository telemetry;
    private final CommandPublisher commandPublisher;
    private final LiveEventHub events;
    private final int historyLimit;
    private final int staleAfterSeconds;

    public DeviceService(
            DeviceRepository devices,
            TelemetryRepository telemetry,
            @Lazy CommandPublisher commandPublisher,
            LiveEventHub events,
            @Value("${app.telemetry.history-limit:200}") int historyLimit,
            @Value("${app.telemetry.stale-after-seconds:30}") int staleAfterSeconds
    ) {
        this.devices = devices;
        this.telemetry = telemetry;
        this.commandPublisher = commandPublisher;
        this.events = events;
        this.historyLimit = historyLimit;
        this.staleAfterSeconds = staleAfterSeconds;
    }

    @Transactional(readOnly = true)
    public List<DeviceDto> listDevices() {
        return devices.findAllByOrderByNameAsc().stream().map(DeviceDto::from).toList();
    }

    @Transactional(readOnly = true)
    public DeviceDto getDevice(String id) {
        return DeviceDto.from(requireDevice(id));
    }

    @Transactional(readOnly = true)
    public List<TelemetryDto> history(String id, int limit) {
        requireDevice(id);
        int capped = Math.max(1, Math.min(limit, historyLimit));
        List<TelemetrySample> newestFirst = telemetry.findByDeviceIdOrderByTsDesc(id, PageRequest.of(0, capped));
        List<TelemetryDto> chronological = new ArrayList<>(newestFirst.size());
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            chronological.add(TelemetryDto.from(newestFirst.get(i)));
        }
        return chronological;
    }

    @Transactional
    public DeviceDto setLed(String id, boolean led) {
        Device device = requireDevice(id);
        commandPublisher.publishLed(id, led);
        device.setLedOn(led);
        DeviceDto dto = DeviceDto.from(devices.save(device));
        events.deviceUpdated(dto);
        return dto;
    }

    @Transactional
    public DeviceDto onStatus(String deviceId, MqttPayloads.StatusPayload status) {
        Device device = getOrCreate(deviceId, status.name());
        if (status.name() != null && !status.name().isBlank()) {
            device.setName(status.name().trim());
        }
        device.setOnline(status.online());
        device.setLastSeen(Instant.now());
        DeviceDto dto = DeviceDto.from(devices.save(device));
        events.deviceUpdated(dto);
        log.info("Device {} status -> {}", deviceId, status.online() ? "online" : "offline");
        return dto;
    }

    @Transactional
    public DeviceDto onTelemetry(String deviceId, MqttPayloads.TelemetryPayload payload) {
        Device device = getOrCreate(deviceId, null);
        Instant ts = payload.ts() == null ? Instant.now() : payload.ts();
        device.setOnline(true);
        device.setLastSeen(Instant.now());
        device.setLastTempC(payload.tempC());
        device.setLastHumidity(payload.humidity());
        device.setLedOn(payload.led());
        device.setLastRssi(payload.rssi());
        device.setLastSource(payload.source() == null ? TelemetrySource.SIMULATED : payload.source());

        TelemetrySample sample = new TelemetrySample();
        sample.setDeviceId(deviceId);
        sample.setTempC(payload.tempC());
        sample.setHumidity(payload.humidity());
        sample.setLedOn(payload.led());
        sample.setRssi(payload.rssi());
        sample.setSource(device.getLastSource());
        sample.setTs(ts);
        telemetry.save(sample);
        pruneHistory(deviceId);

        DeviceDto dto = DeviceDto.from(devices.save(device));
        events.telemetryUpdated(deviceId, TelemetryDto.from(sample), dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public long countDevices() {
        return devices.count();
    }

    @Transactional(readOnly = true)
    public long countOnline() {
        return devices.countByOnlineTrue();
    }

    @Scheduled(fixedRate = 5_000)
    @Transactional
    public void markStaleDevicesOffline() {
        Instant cutoff = Instant.now().minusSeconds(staleAfterSeconds);
        List<Device> stale = devices.findByOnlineTrueAndLastSeenBefore(cutoff);
        for (Device device : stale) {
            device.setOnline(false);
            devices.save(device);
            events.deviceUpdated(DeviceDto.from(device));
            log.info("Device {} marked offline after {}s without heartbeat", device.getId(), staleAfterSeconds);
        }
    }

    private Device requireDevice(String id) {
        return devices.findById(id).orElseThrow(() -> new DeviceNotFoundException(id));
    }

    private Device getOrCreate(String deviceId, String maybeName) {
        return devices.findById(deviceId).orElseGet(() -> {
            Device created = new Device();
            created.setId(deviceId);
            created.setName(maybeName == null || maybeName.isBlank() ? defaultName(deviceId) : maybeName.trim());
            created.setCreatedAt(Instant.now());
            created.setOnline(false);
            created.setLedOn(false);
            return devices.save(created);
        });
    }

    private void pruneHistory(String deviceId) {
        List<TelemetrySample> samples = telemetry.findByDeviceIdOrderByTsAsc(deviceId);
        if (samples.size() > historyLimit) {
            telemetry.deleteAll(samples.subList(0, samples.size() - historyLimit));
        }
    }

    private static String defaultName(String deviceId) {
        return "设备 " + deviceId;
    }
}
