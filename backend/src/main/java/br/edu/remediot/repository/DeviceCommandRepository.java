package br.edu.remediot.repository;

import br.edu.remediot.entity.DeviceCommand;
import br.edu.remediot.entity.DeviceCommandStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, String> {

    List<DeviceCommand> findByStatusAndSentAtBefore(DeviceCommandStatus status, Instant sentBefore);
}
