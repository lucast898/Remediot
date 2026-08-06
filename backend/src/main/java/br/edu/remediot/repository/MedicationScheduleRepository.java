package br.edu.remediot.repository;

import br.edu.remediot.entity.MedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {

    List<MedicationSchedule> findByMedicationId(Long medicationId);

    List<MedicationSchedule> findByMedicationPatientId(Long patientId);
}
