package org.example.moliyaapp.service;


import org.example.moliyaapp.entity.Device;
import org.springframework.stereotype.Component;

@Component
public interface DeviceService {

    boolean isDeviceTrusted(Long userId, String deviceId);

    void addTrustedDevice(Long userId, String deviceId);

    Device addTrustedDeviceByLogin(Long userId, String deviceId);
}
