package br.edu.remediot.repository;

import br.edu.remediot.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationRepository extends JpaRepository<Medication, Long> {

    List<Medication> findByPatientId(Long patientId);
}
