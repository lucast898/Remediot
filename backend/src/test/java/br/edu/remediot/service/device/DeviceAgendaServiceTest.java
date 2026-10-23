package br.edu.remediot.service.device;

import br.edu.remediot.dto.CreateMedicationRequest;
import br.edu.remediot.dto.device.AgendaMessage;
import br.edu.remediot.entity.*;
import br.edu.remediot.mqtt.DeviceMessagePublisher;
import br.edu.remediot.repository.*;
import br.edu.remediot.service.MedicationService;
import br.edu.remediot.service.MedicationsChangedEvent;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Agenda do app para a gaveta (CONTRATO-MQTT.md, seção 7; D2 e D3). */
@SpringBootTest
@Transactional
@RecordApplicationEvents
class DeviceAgendaServiceTest {

    private static final String DEVICE_ID = "A4CF12B3C4D5";
    private static final String AGENDA_TOPIC = "remediot/v1/A4CF12B3C4D5/config/agenda";

    @Autowired DeviceAgendaService service;
    @Autowired DeviceAckService ackService;
    @Autowired MedicationService medicationService;
    @Autowired UserRepository userRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired MedicationRepository medicationRepository;
    @Autowired DeviceRepository deviceRepository;
    @Autowired DeviceAgendaRepository agendaRepository;
    @Autowired JsonMapper jsonMapper;
    @Autowired EntityManager entityManager;
    @Autowired ApplicationEvents events;

    @MockitoBean DeviceMessagePublisher publisher;

    private Patient patient;

    @BeforeEach
    void setUp() {
        User caregiver = userRepository.save(new User("Cuidador", "cuidador@example.com", "hash", Role.CUIDADOR));
        User user = userRepository.save(new User("Paciente", "paciente@example.com", "hash", Role.PACIENTE));
        patient = patientRepository.save(new Patient(user, caregiver, 70, "Rua A", 0.0, 0.0));
        Device device = new Device(DEVICE_ID);
        device.setPatient(patient);
        deviceRepository.save(device);
        flushAndClear();
    }

    @Test
    void umaEntradaPorHorarioComPesoESemNomeDeRemedio() {
        Medication losartana = newMedication("Losartana", 3, LocalTime.of(8, 0), LocalTime.of(20, 0));
        Medication vitamina = newMedication("Vitamina D", 1, LocalTime.of(8, 0));
        flushAndClear();

        service.publishAgenda(DEVICE_ID);

        String json = publishedAgendaJson();
        assertFalse(json.contains("Losartana") || json.contains("Vitamina"), "a agenda não pode levar nome (LGPD)");
        AgendaMessage agenda = jsonMapper.readValue(json, AgendaMessage.class);
        assertTrue(agenda.configId().matches("cfg-[0-9a-f]{8}"));
        assertEquals(List.of(
                new AgendaMessage.Entry("s" + scheduleId(losartana, 8), "08:00", 3),
                new AgendaMessage.Entry("s" + scheduleId(vitamina, 8), "08:00", 1),
                new AgendaMessage.Entry("s" + scheduleId(losartana, 20), "20:00", 3)), agenda.doses());
        assertEquals(DeviceAgendaStatus.ENVIADA, agendaRepository.findByConfigId(agenda.configId()).orElseThrow().getStatus());
    }

    @Test
    void versaoCresceACadaPublicacao() {
        DeviceAgenda first = service.publishAgenda(DEVICE_ID);
        DeviceAgenda second = service.publishAgenda(DEVICE_ID);

        assertTrue(first.getVersion() >= System.currentTimeMillis() / 1000 - 5, "versão em segundos (epoch)");
        assertTrue(second.getVersion() > first.getVersion());
    }

    @Test
    void agendaVaziaAoDesvincular() {
        newMedication("Losartana", 2, LocalTime.of(8, 0));
        flushAndClear();

        service.publishEmptyAgenda(DEVICE_ID);

        assertTrue(jsonMapper.readValue(publishedAgendaJson(), AgendaMessage.class).doses().isEmpty());
    }

    @Test
    void maisDeQuarentaHorariosNaoPublicaEFicaRejeitada() {
        LocalTime[] times = new LocalTime[41];
        for (int i = 0; i < times.length; i++) {
            times[i] = LocalTime.of(i / 2, (i % 2) * 30);
        }
        newMedication("Muitos horários", 1, times);
        flushAndClear();

        DeviceAgenda agenda = service.publishAgenda(DEVICE_ID);

        assertEquals(DeviceAgendaStatus.REJEITADA, agenda.getStatus());
        assertTrue(agenda.getErrors().contains("DOSES_DEMAIS"));
        verifyNoInteractions(publisher);
    }

    @Test
    void ackAplicadaAtualizaAVersaoDaGaveta() {
        DeviceAgenda agenda = service.publishAgenda(DEVICE_ID);

        ackService.handle(DEVICE_ID, ack(agenda, "APLICADA", "[]"));
        flushAndClear();

        assertEquals(DeviceAgendaStatus.APLICADA, agendaRepository.findByConfigId(agenda.getConfigId()).orElseThrow().getStatus());
        assertEquals(agenda.getVersion(), deviceRepository.findById(DEVICE_ID).orElseThrow().getAppliedAgendaVersion());
    }

    @Test
    void ackRejeitadaGuardaOsErros() {
        DeviceAgenda agenda = service.publishAgenda(DEVICE_ID);

        ackService.handle(DEVICE_ID, ack(agenda, "REJEITADA", "[{\"codigo\":\"PESO_INVALIDO\",\"doseId\":\"s17\"}]"));
        flushAndClear();

        DeviceAgenda saved = agendaRepository.findByConfigId(agenda.getConfigId()).orElseThrow();
        assertEquals(DeviceAgendaStatus.REJEITADA, saved.getStatus());
        assertTrue(saved.getErrors().contains("PESO_INVALIDO"));
        assertNull(deviceRepository.findById(DEVICE_ID).orElseThrow().getAppliedAgendaVersion());
    }

    @Test
    void ackDeVersaoAntigaNaoVoltaAVersaoDaGaveta() {
        DeviceAgenda old = service.publishAgenda(DEVICE_ID);
        DeviceAgenda latest = service.publishAgenda(DEVICE_ID);
        ackService.handle(DEVICE_ID, ack(latest, "APLICADA", "[]"));
        ackService.handle(DEVICE_ID, ack(old, "APLICADA", "[]"));
        flushAndClear();

        assertEquals(latest.getVersion(), deviceRepository.findById(DEVICE_ID).orElseThrow().getAppliedAgendaVersion());
    }

    @Test
    void ackDeOutraGavetaEhIgnorado() {
        DeviceAgenda agenda = service.publishAgenda(DEVICE_ID);

        ackService.handle("0123456789AB", ack(agenda, "APLICADA", "[]").replace(DEVICE_ID, "0123456789AB"));
        flushAndClear();

        assertEquals(DeviceAgendaStatus.ENVIADA, agendaRepository.findByConfigId(agenda.getConfigId()).orElseThrow().getStatus());
    }

    @Test
    void alterarRemedioAvisaQueAAgendaMudou() {
        medicationService.create(patient.getId(),
                new CreateMedicationRequest("Losartana", "50mg", 2, List.of(LocalTime.of(8, 0))));

        assertEquals(1, events.stream(MedicationsChangedEvent.class)
                .filter(e -> e.patientId().equals(patient.getId()))
                .count());
    }

    // ---------------- auxiliares ----------------

    private Medication newMedication(String name, int weight, LocalTime... times) {
        Medication medication = new Medication(patient, name, "1 comp", weight);
        for (LocalTime time : times) {
            medication.getSchedules().add(new MedicationSchedule(medication, time));
        }
        Medication saved = medicationRepository.save(medication);
        entityManager.flush();
        return saved;
    }

    private Long scheduleId(Medication medication, int hour) {
        return medication.getSchedules().stream()
                .filter(s -> s.getTime().getHour() == hour)
                .findFirst().orElseThrow().getId();
    }

    private String publishedAgendaJson() {
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(publisher).publish(eq(AGENDA_TOPIC), json.capture(), eq(1), eq(true));
        return json.getValue();
    }

    private static String ack(DeviceAgenda agenda, String status, String errors) {
        return "{\"commandId\":\"" + agenda.getConfigId() + "\",\"deviceId\":\"" + DEVICE_ID + "\",\"tipo\":\"AGENDA\","
                + "\"status\":\"" + status + "\",\"versao\":" + agenda.getVersion() + ",\"doses\":0,"
                + "\"erros\":" + errors + ",\"ts\":1790244002}";
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
