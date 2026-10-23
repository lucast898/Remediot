package br.edu.remediot.repository;

import br.edu.remediot.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, String> {

    Optional<Device> findByPatientId(Long patientId);

    /** Pareamento (D7): gavetas sem paciente, com o código no fim do deviceId, vistas há pouco. */
    List<Device> findByPatientIsNullAndDeviceIdEndingWithAndLastSeenAtAfter(String code, Instant seenAfter);

    List<Device> findByOnlineFalseAndOfflineReasonAndOfflineAlertSentFalseAndOfflineSinceBefore(
            String offlineReason, Instant offlineBefore);
}
