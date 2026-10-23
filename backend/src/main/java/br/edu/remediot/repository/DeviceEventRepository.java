package br.edu.remediot.repository;

import br.edu.remediot.entity.DeviceEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface DeviceEventRepository extends JpaRepository<DeviceEvent, String> {

    List<DeviceEvent> findByDeviceDeviceIdOrderByOccurredAtDesc(String deviceId);

    /** Histórico só desde o pareamento: eventos de um dono anterior da gaveta não aparecem. */
    List<DeviceEvent> findByDeviceDeviceIdAndReceivedAtGreaterThanEqualOrderByOccurredAtDesc(
            String deviceId, Instant since, Pageable pageable);

    List<DeviceEvent> findByDeviceDeviceIdAndReceivedAtGreaterThanEqualAndTypeInAndAcknowledgedFalseOrderByOccurredAtDesc(
            String deviceId, Instant since, Collection<String> types);
}
