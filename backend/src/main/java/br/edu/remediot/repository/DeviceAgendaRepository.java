package br.edu.remediot.repository;

import br.edu.remediot.entity.DeviceAgenda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceAgendaRepository extends JpaRepository<DeviceAgenda, Long> {

    Optional<DeviceAgenda> findFirstByDeviceDeviceIdOrderByVersionDesc(String deviceId);

    Optional<DeviceAgenda> findByConfigId(String configId);
}
