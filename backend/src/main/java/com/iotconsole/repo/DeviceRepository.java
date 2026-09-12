package com.iotconsole.repo;

import com.iotconsole.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, String> {

    List<Device> findAllByOrderByNameAsc();

    List<Device> findByOnlineTrueAndLastSeenBefore(Instant cutoff);

    long countByOnlineTrue();
}
