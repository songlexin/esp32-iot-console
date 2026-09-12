package com.iotconsole.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "telemetry_samples", indexes = {
        @Index(name = "idx_telemetry_device_ts", columnList = "deviceId,ts")
})
public class TelemetrySample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String deviceId;

    private double tempC;
    private double humidity;
    private boolean ledOn;
    private Integer rssi;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private TelemetrySource source;

    @Column(nullable = false)
    private Instant ts;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public double getTempC() {
        return tempC;
    }

    public void setTempC(double tempC) {
        this.tempC = tempC;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public boolean isLedOn() {
        return ledOn;
    }

    public void setLedOn(boolean ledOn) {
        this.ledOn = ledOn;
    }

    public Integer getRssi() {
        return rssi;
    }

    public void setRssi(Integer rssi) {
        this.rssi = rssi;
    }

    public TelemetrySource getSource() {
        return source;
    }

    public void setSource(TelemetrySource source) {
        this.source = source;
    }

    public Instant getTs() {
        return ts;
    }

    public void setTs(Instant ts) {
        this.ts = ts;
    }
}
