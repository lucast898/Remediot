package br.edu.remediot.repository;

import br.edu.remediot.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, String> {

    Optional<Device> findByPatientId(Long patientId);
}
