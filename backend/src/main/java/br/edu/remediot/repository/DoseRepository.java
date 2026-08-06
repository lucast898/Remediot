package br.edu.remediot.repository;

import br.edu.remediot.entity.Dose;
import br.edu.remediot.entity.DoseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DoseRepository extends JpaRepository<Dose, Long> {

    List<Dose> findByScheduleMedicationPatientIdAndScheduledDate(Long patientId, LocalDate scheduledDate);

    List<Dose> findByScheduleMedicationPatientIdAndScheduledDateBetween(Long patientId, LocalDate start, LocalDate end);

    List<Dose> findByStatusAndScheduledAtBefore(DoseStatus status, LocalDateTime cutoff);

    Optional<Dose> findByScheduleIdAndScheduledDate(Long scheduleId, LocalDate scheduledDate);
}
