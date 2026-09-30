package br.edu.remediot.service;

import br.edu.remediot.dto.UpdateMedicationRequest;
import br.edu.remediot.entity.*;
import br.edu.remediot.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Excluir/editar medicamento que já tem doses, eventos de abertura e lembretes
 * (as chaves estrangeiras são verificadas pelo banco no flush).
 */
@SpringBootTest
@Transactional
class MedicationServiceTest {

    @Autowired MedicationService medicationService;
    @Autowired UserRepository userRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired MedicationRepository medicationRepository;
    @Autowired DoseRepository doseRepository;
    @Autowired OpeningEventRepository openingEventRepository;
    @Autowired DoseReminderRepository doseReminderRepository;
    @Autowired EntityManager entityManager;

    private Patient patient;
    private Medication medication;
    private MedicationSchedule morning;
    private MedicationSchedule evening;
    private Dose morningDose;

    @BeforeEach
    void setUp() {
        User caregiver = userRepository.save(new User("Cuidador", "cuidador@example.com", "hash", Role.CUIDADOR));
        User patientUser = userRepository.save(new User("Paciente", "paciente@example.com", "hash", Role.PACIENTE));
        patient = patientRepository.save(new Patient(patientUser, caregiver, 70, "Rua A", 0.0, 0.0));

        medication = new Medication(patient, "Losartana", "50mg", 2);
        morning = new MedicationSchedule(medication, LocalTime.of(8, 0));
        evening = new MedicationSchedule(medication, LocalTime.of(20, 0));
        medication.getSchedules().addAll(List.of(morning, evening));
        medication = medicationRepository.save(medication);

        LocalDate today = LocalDate.now();
        morningDose = doseRepository.save(new Dose(morning, today, LocalDateTime.of(today, morning.getTime())));
        Dose eveningDose = doseRepository.save(new Dose(evening, today, LocalDateTime.of(today, evening.getTime())));
        openingEventRepository.save(new OpeningEvent(morningDose, LocalDateTime.now(), OpeningEventSource.SIMULATOR));
        doseReminderRepository.save(new DoseReminder(eveningDose, 300));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void excluirMedicamentoComHistoricoApagaDosesEventosELembretes() {
        medicationService.delete(medication.getId(), patient.getId());
        entityManager.flush();
        entityManager.clear();

        assertFalse(medicationRepository.existsById(medication.getId()));
        assertEquals(0, doseRepository.count());
        assertEquals(0, openingEventRepository.count());
        assertEquals(0, doseReminderRepository.count());
    }

    @Test
    void editarSemMudarHorariosPreservaDosesEHistorico() {
        medicationService.update(medication.getId(), patient.getId(),
                new UpdateMedicationRequest("Losartana", "50 miligramas", 3,
                        List.of(LocalTime.of(8, 0), LocalTime.of(20, 0))));
        entityManager.flush();
        entityManager.clear();

        assertEquals(2, doseRepository.count());
        assertEquals(1, openingEventRepository.count());
        assertEquals("50 miligramas", medicationRepository.findById(medication.getId()).orElseThrow().getDose());
    }

    @Test
    void removerUmHorarioApagaSoAsDosesDele() {
        medicationService.update(medication.getId(), patient.getId(),
                new UpdateMedicationRequest("Losartana", "50mg", 2,
                        List.of(LocalTime.of(8, 0), LocalTime.of(12, 0))));
        entityManager.flush();
        entityManager.clear();

        assertTrue(doseRepository.findById(morningDose.getId()).isPresent());
        assertEquals(1, doseRepository.count());
        assertEquals(1, openingEventRepository.count());
        assertEquals(0, doseReminderRepository.count());
        List<LocalTime> times = medicationRepository.findById(medication.getId()).orElseThrow()
                .getSchedules().stream().map(MedicationSchedule::getTime).sorted().toList();
        assertEquals(List.of(LocalTime.of(8, 0), LocalTime.of(12, 0)), times);
    }
}
