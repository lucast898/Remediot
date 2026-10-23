package br.edu.remediot.repository;

import br.edu.remediot.entity.DeviceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceEventRepository extends JpaRepository<DeviceEvent, String> {

    List<DeviceEvent> findByDeviceDeviceIdOrderByOccurredAtDesc(String deviceId);
}
