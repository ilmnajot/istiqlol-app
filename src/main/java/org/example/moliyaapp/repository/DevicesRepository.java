package org.example.moliyaapp.repository;

import org.example.moliyaapp.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DevicesRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceNameAndUserId(String deviceName, Long userId);

    @Query(value = "select * from device as d where d.user_id=?1 and d.device_name=?2", nativeQuery = true)
    Optional<Device> findByUserIdAndDeviceName(Long userId, String deviceName);
}
