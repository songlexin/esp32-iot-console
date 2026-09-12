package com.iotconsole.repo;

import com.iotconsole.domain.TelemetrySample;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelemetryRepository extends JpaRepository<TelemetrySample, Long> {

    List<TelemetrySample> findByDeviceIdOrderByTsDesc(String deviceId, Pageable pageable);

    List<TelemetrySample> findByDeviceIdOrderByTsAsc(String deviceId);

    long countByDeviceId(String deviceId);
}
