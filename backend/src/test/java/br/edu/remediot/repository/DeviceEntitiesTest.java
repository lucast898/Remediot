package br.edu.remediot.repository;

import br.edu.remediot.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** Mapeamento das tabelas da gaveta (device, device_event, device_command, device_agenda). */
@SpringBootTest
@Transactional
class DeviceEntitiesTest {

    @Autowired UserRepository userRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired DeviceRepository deviceRepository;
    @Autowired DeviceEventRepository deviceEventRepository;
    @Autowired DeviceCommandRepository deviceCommandRepository;
    @Autowired DeviceAgendaRepository deviceAgendaRepository;
    @Autowired EntityManager entityManager;

    private Patient patient;

    @BeforeEach
    void setUp() {
        User caregiver = userRepository.save(new User("Cuidador", "cuidador@example.com", "hash", Role.CUIDADOR));
        User patientUser = userRepository.save(new User("Paciente", "paciente@example.com", "hash", Role.PACIENTE));
        patient = patientRepository.save(new Patient(patientUser, caregiver, 70, "Rua A", 0.0, 0.0));
    }

    @Test
    void gavetaSemPacienteEDepoisPareada() {
        deviceRepository.save(new Device("A4CF12B3C4D5"));
        flushAndClear();

        Device device = deviceRepository.findById("A4CF12B3C4D5").orElseThrow();
        assertNull(device.getPatient());

        device.setPatient(patient);
        device.setPairedAt(Instant.now());
        flushAndClear();

        assertEquals("A4CF12B3C4D5", deviceRepository.findByPatientId(patient.getId()).orElseThrow().getDeviceId());
    }

    @Test
    void eventoGuardaPayloadEPodeSerReclassificado() {
        Device device = deviceRepository.save(new Device("A4CF12B3C4D5"));
        Instant ts = Instant.ofEpochSecond(1790291342);
        String payload = "{\"eventId\":\"A4CF12B3C4D5-00000007\",\"tipo\":\"ABERTURA_FORA_HORARIO\"}";
        deviceEventRepository.save(new DeviceEvent("A4CF12B3C4D5-00000007", device, "ABERTURA_FORA_HORARIO",
                ts, true, ts.plusSeconds(2), payload));
        flushAndClear();

        DeviceEvent event = deviceEventRepository.findById("A4CF12B3C4D5-00000007").orElseThrow();
        assertEquals(payload, event.getPayload());
        assertEquals(ts, event.getOccurredAt());
        assertNull(event.getClassification());
        assertFalse(event.isAcknowledged());

        event.setClassification(DeviceEventClassification.CAREGIVER);
        event.setAcknowledged(true);
        flushAndClear();

        DeviceEvent reloaded = deviceEventRepository.findById("A4CF12B3C4D5-00000007").orElseThrow();
        assertEquals(DeviceEventClassification.CAREGIVER, reloaded.getClassification());
        assertTrue(reloaded.isAcknowledged());
        assertEquals(1, deviceEventRepository.findByDeviceDeviceIdOrderByOccurredAtDesc("A4CF12B3C4D5").size());
    }

    @Test
    void comandoComecaPendente() {
        Device device = deviceRepository.save(new Device("A4CF12B3C4D5"));
        Instant sentAt = Instant.now().minusSeconds(60);
        deviceCommandRepository.save(new DeviceCommand("cmd-8a41c2d0", device, "LOCALIZAR", sentAt));
        flushAndClear();

        assertEquals(1, deviceCommandRepository
                .findByStatusAndSentAtBefore(DeviceCommandStatus.PENDENTE, Instant.now().minusSeconds(30)).size());
    }

    @Test
    void ultimaAgendaEhADeMaiorVersao() {
        Device device = deviceRepository.save(new Device("A4CF12B3C4D5"));
        deviceAgendaRepository.save(new DeviceAgenda(device, 1790244000L, "cfg-aaaaaaaa", "{\"doses\":[]}"));
        deviceAgendaRepository.save(new DeviceAgenda(device, 1790244100L, "cfg-bbbbbbbb", "{\"doses\":[]}"));
        flushAndClear();

        DeviceAgenda latest = deviceAgendaRepository.findFirstByDeviceDeviceIdOrderByVersionDesc("A4CF12B3C4D5").orElseThrow();
        assertEquals("cfg-bbbbbbbb", latest.getConfigId());
        assertEquals(DeviceAgendaStatus.ENVIADA, latest.getStatus());
        assertTrue(deviceAgendaRepository.findByConfigId("cfg-aaaaaaaa").isPresent());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
