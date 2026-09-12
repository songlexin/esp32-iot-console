package com.iotconsole.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 120)
    private String name;

    private boolean online;
    private boolean ledOn;

    private Instant lastSeen;
    private Instant createdAt;

    private Double lastTempC;
    private Double lastHumidity;
    private Integer lastRssi;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private TelemetrySource lastSource;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isLedOn() {
        return ledOn;
    }

    public void setLedOn(boolean ledOn) {
        this.ledOn = ledOn;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Double getLastTempC() {
        return lastTempC;
    }

    public void setLastTempC(Double lastTempC) {
        this.lastTempC = lastTempC;
    }

    public Double getLastHumidity() {
        return lastHumidity;
    }

    public void setLastHumidity(Double lastHumidity) {
        this.lastHumidity = lastHumidity;
    }

    public Integer getLastRssi() {
        return lastRssi;
    }

    public void setLastRssi(Integer lastRssi) {
        this.lastRssi = lastRssi;
    }

    public TelemetrySource getLastSource() {
        return lastSource;
    }

    public void setLastSource(TelemetrySource lastSource) {
        this.lastSource = lastSource;
    }
}
