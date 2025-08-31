package org.example.moliyaapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.entity.Device;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.repository.DevicesRepository;
import org.example.moliyaapp.repository.UserRepository;
import org.example.moliyaapp.service.DeviceService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DevicesServiceImpl implements DeviceService {
    private final DevicesRepository devicesRepository;
    private final UserRepository userRepository;

    @Override
    public boolean isDeviceTrusted(Long userId, String deviceId) {
        Device devices = devicesRepository.findByDeviceNameAndUserId(deviceId, userId)
                .orElse(null);
        if (devices == null) {
            return false;
        }
        if (devices.getExpiredAt() != null && devices.getExpiredAt().isBefore(LocalDateTime.now())) {
            devices.setUser(null);
            return false;
        }
        return true;
    }

    @Override
    public void addTrustedDevice(Long userId, String deviceId) {
        User user = this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER IS NOT FOUND"));
        Device devices = new Device();
        devices.setDeviceName(deviceId);
        devices.setAddedAt(LocalDateTime.now());
        devices.setUser(user);
        // 15 kundan keyin expire bo'lishi kerak
        devices.setExpiredAt(LocalDateTime.now().plusDays(15));

        devicesRepository.save(devices);

    }

    @Override
    public Device addTrustedDeviceByLogin(Long userId, String deviceId) {
        Device devices = new Device();
        devices.setAddedAt(LocalDateTime.now());
        // 15 kundan keyin expire bo'lishi kerak
        devices.setExpiredAt(LocalDateTime.now().plusDays(15));

        return devices;
    }
}
