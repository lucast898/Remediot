package br.edu.remediot.service.device;

import br.edu.remediot.dto.CreateMedicationRequest;
import br.edu.remediot.entity.*;
import br.edu.remediot.mqtt.DeviceMessagePublisher;
import br.edu.remediot.repository.DeviceRepository;
import br.edu.remediot.repository.PatientRepository;
import br.edu.remediot.repository.UserRepository;
import br.edu.remediot.service.MedicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Caminho real, com commit: cadastrar um remédio publica a agenda da gaveta logo
 * depois do commit. Sem @Transactional, então limpa o banco no fim.
 */
@SpringBootTest
class AgendaAfterCommitTest {

    @Autowired MedicationService medicationService;
    @Autowired UserRepository userRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired DeviceRepository deviceRepository;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean DeviceMessagePublisher publisher;

    @AfterEach
    void cleanUp() {
        for (String table : List.of("dose_reminder", "opening_event", "dose", "medication_schedule", "medication",
                "device_agenda", "device_event", "device_command", "device", "patient", "app_user")) {
            jdbc.update("DELETE FROM " + table);
        }
    }

    @Test
    void cadastrarRemedioPublicaAAgendaDepoisDoCommit() {
        User caregiver = userRepository.save(new User("Cuidador", "cuidador@example.com", "hash", Role.CUIDADOR));
        User user = userRepository.save(new User("Paciente", "paciente@example.com", "hash", Role.PACIENTE));
        Patient patient = patientRepository.save(new Patient(user, caregiver, 70, "Rua A", 0.0, 0.0));
        Device device = new Device("A4CF12B3C4D5");
        device.setPatient(patient);
        deviceRepository.save(device);

        medicationService.create(patient.getId(),
                new CreateMedicationRequest("Losartana", "50mg", 2, List.of(LocalTime.of(8, 0))));

        verify(publisher).publish(eq("remediot/v1/A4CF12B3C4D5/config/agenda"),
                argThat(json -> json.contains("\"hora\":\"08:00\"") && json.contains("\"peso\":2")), eq(1), eq(true));
    }

    @Test
    void pacienteSemGavetaNaoPublicaNada() {
        User caregiver = userRepository.save(new User("Cuidador", "cuidador@example.com", "hash", Role.CUIDADOR));
        User user = userRepository.save(new User("Paciente", "paciente@example.com", "hash", Role.PACIENTE));
        Patient patient = patientRepository.save(new Patient(user, caregiver, 70, "Rua A", 0.0, 0.0));

        medicationService.create(patient.getId(),
                new CreateMedicationRequest("Losartana", "50mg", 2, List.of(LocalTime.of(8, 0))));

        verifyNoInteractions(publisher);
    }
}
