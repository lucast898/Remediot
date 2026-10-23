package br.edu.remediot.service.device;

import br.edu.remediot.dto.device.DeviceEventResponse;
import br.edu.remediot.dto.device.DeviceResponse;
import br.edu.remediot.entity.*;
import br.edu.remediot.exception.DeviceConflictException;
import br.edu.remediot.exception.ResourceNotFoundException;
import br.edu.remediot.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pareamento pelos 4 caracteres (D7), desvinculação e estado da gaveta. */
@SpringBootTest
@Transactional
@RecordApplicationEvents
class DeviceServiceTest {

    @Autowired DeviceService service;
    @Autowired UserRepository userRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired DeviceRepository deviceRepository;
    @Autowired DeviceAgendaRepository agendaRepository;
    @Autowired DeviceEventRepository eventRepository;
    @Autowired EntityManager entityManager;
    @Autowired ApplicationEvents events;

    private Patient patient;

    @BeforeEach
    void setUp() {
        patient = newPatient("paciente@example.com");
    }

    @Test
    void pareiaPelosQuatroCaracteresEPedeAAgenda() {
        waitingDevice("A4CF12B3C4D5", Duration.ofMinutes(1));

        DeviceResponse response = service.pair(patient.getId(), "c4d5");

        assertTrue(response.paired());
        assertEquals("A4CF12B3C4D5", response.deviceId());
        assertEquals(patient.getId(), deviceRepository.findById("A4CF12B3C4D5").orElseThrow().getPatient().getId());
        assertEquals(List.of(new DeviceAgendaRequestedEvent("A4CF12B3C4D5", false)),
                events.stream(DeviceAgendaRequestedEvent.class).toList());
    }

    @Test
    void aceitaONomeInteiroDaRede() {
        waitingDevice("A4CF12B3C4D5", Duration.ofMinutes(1));

        assertTrue(service.pair(patient.getId(), " RemedIoT-C4D5 ").paired());
    }

    @Test
    void codigoInvalidoEhRecusado() {
        assertThrows(IllegalArgumentException.class, () -> service.pair(patient.getId(), "C4D"));
        assertThrows(IllegalArgumentException.class, () -> service.pair(patient.getId(), "C4DZ"));
    }

    @Test
    void semGavetaComEsseCodigo() {
        waitingDevice("A4CF12B3C4D5", Duration.ofMinutes(1));

        assertThrows(ResourceNotFoundException.class, () -> service.pair(patient.getId(), "FFFF"));
    }

    @Test
    void gavetaVistaHaMaisDeQuinzeMinutosNaoPareia() {
        waitingDevice("A4CF12B3C4D5", Duration.ofMinutes(20));

        assertThrows(ResourceNotFoundException.class, () -> service.pair(patient.getId(), "C4D5"));
    }

    @Test
    void duasGavetasComOMesmoCodigoNaoPareia() {
        waitingDevice("A4CF12B3C4D5", Duration.ofMinutes(1));
        waitingDevice("0000AAAAC4D5", Duration.ofMinutes(1));

        assertThrows(DeviceConflictException.class, () -> service.pair(patient.getId(), "C4D5"));
    }

    @Test
    void gavetaDeOutroPacienteNaoEntraNaBusca() {
        Device taken = waitingDevice("A4CF12B3C4D5", Duration.ofMinutes(1));
        taken.setPatient(newPatient("outro@example.com"));
        flushAndClear();

        assertThrows(ResourceNotFoundException.class, () -> service.pair(patient.getId(), "C4D5"));
    }

    @Test
    void pacienteComGavetaNaoPareiaOutra() {
        waitingDevice("A4CF12B3C4D5", Duration.ofMinutes(1));
        service.pair(patient.getId(), "C4D5");
        waitingDevice("0000AAAABBBB", Duration.ofMinutes(1));

        assertThrows(DeviceConflictException.class, () -> service.pair(patient.getId(), "BBBB"));
    }

    @Test
    void desvincularPedeAgendaVazia() {
        waitingDevice("A4CF12B3C4D5", Duration.ofMinutes(1));
        service.pair(patient.getId(), "C4D5");
        flushAndClear();

        service.unpair(patient.getId());
        flushAndClear();

        assertNull(deviceRepository.findById("A4CF12B3C4D5").orElseThrow().getPatient());
        assertFalse(service.getForPatient(patient.getId()).paired());
        assertTrue(events.stream(DeviceAgendaRequestedEvent.class).anyMatch(DeviceAgendaRequestedEvent::empty));
    }

    @Test
    void estadoDaAgendaSincronizadaAguardandoERejeitada() {
        Device device = waitingDevice("A4CF12B3C4D5", Duration.ofMinutes(1));
        service.pair(patient.getId(), "C4D5");
        assertEquals("SEM_AGENDA", service.getForPatient(patient.getId()).agendaStatus());

        agendaRepository.save(new DeviceAgenda(device, 100L, "cfg-00000001", "{}"));
        flushAndClear();
        assertEquals("AGUARDANDO", service.getForPatient(patient.getId()).agendaStatus());

        deviceRepository.findById("A4CF12B3C4D5").orElseThrow().setAppliedAgendaVersion(100L);
        flushAndClear();
        assertEquals("SINCRONIZADA", service.getForPatient(patient.getId()).agendaStatus());

        DeviceAgenda rejected = new DeviceAgenda(deviceRepository.getReferenceById("A4CF12B3C4D5"), 101L, "cfg-00000002", "{}");
        rejected.setStatus(DeviceAgendaStatus.REJEITADA);
        rejected.setErrors("[{\"codigo\":\"DOSES_DEMAIS\"}]");
        agendaRepository.save(rejected);
        flushAndClear();
        DeviceResponse response = service.getForPatient(patient.getId());
        assertEquals("REJEITADA", response.agendaStatus());
        assertEquals(List.of("DOSES_DEMAIS"), response.agendaErrors());
    }

    @Test
    void historicoEAlertasSoDesdeOPareamento() {
        Device device = waitingDevice("A4CF12B3C4D5", Duration.ofMinutes(1));
        Instant beforePairing = Instant.now().minus(Duration.ofHours(1));
        eventRepository.save(new DeviceEvent("A4CF12B3C4D5-00000001", device, "ABERTURA_FORA_HORARIO",
                beforePairing, true, beforePairing, "{\"dados\":{}}"));
        service.pair(patient.getId(), "C4D5");
        Instant now = Instant.now().plusSeconds(1);
        eventRepository.save(new DeviceEvent("A4CF12B3C4D5-00000002", device, "ABERTURA_FORA_HORARIO",
                now, true, now, "{\"dados\":{\"proximaDose\":null}}"));
        eventRepository.save(new DeviceEvent("A4CF12B3C4D5-00000003", device, "DOSE_CONFIRMADA",
                now, true, now, "{\"dados\":{\"horaPrevista\":\"08:00\"}}"));
        flushAndClear();

        List<DeviceEventResponse> history = service.listEvents(patient.getId(), 50);
        assertEquals(2, history.size());
        assertTrue(history.stream().noneMatch(e -> e.eventId().endsWith("00000001")));
        assertTrue(history.stream().allMatch(e -> !e.description().contains("tomad")));

        List<DeviceEventResponse> alerts = service.listPendingAlerts(patient.getId());
        assertEquals(List.of("A4CF12B3C4D5-00000002"), alerts.stream().map(DeviceEventResponse::eventId).toList());
        assertEquals(1, service.getForPatient(patient.getId()).pendingAlerts());
    }

    private Device waitingDevice(String deviceId, Duration seenAgo) {
        Device device = new Device(deviceId);
        device.setOnline(true);
        device.setLastSeenAt(Instant.now().minus(seenAgo));
        Device saved = deviceRepository.save(device);
        entityManager.flush();
        return saved;
    }

    private Patient newPatient(String email) {
        User caregiver = userRepository.save(new User("Cuidador", "c-" + email, "hash", Role.CUIDADOR));
        User user = userRepository.save(new User("Paciente", email, "hash", Role.PACIENTE));
        return patientRepository.save(new Patient(user, caregiver, 70, "Rua A", 0.0, 0.0));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
