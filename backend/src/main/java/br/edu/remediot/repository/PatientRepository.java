package br.edu.remediot.repository;

import br.edu.remediot.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    List<Patient> findByCaregiverId(Long caregiverId);

    Optional<Patient> findByUserId(Long userId);
}
