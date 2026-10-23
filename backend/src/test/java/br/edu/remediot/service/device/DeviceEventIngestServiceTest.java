package br.edu.remediot.service.device;

import br.edu.remediot.entity.*;
import br.edu.remediot.mqtt.DeviceMessagePublisher;
import br.edu.remediot.repository.*;
import br.edu.remediot.service.AlertService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import static br.edu.remediot.service.device.DeviceEventIngestService.DEVICE_ZONE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Eventos da gaveta -> doses (CONTRATO-MQTT.md, seções 3 a 5; decisões D1 e D4). */
@SpringBootTest
@Transactional
class DeviceEventIngestServiceTest {

    private static final String DEVICE_ID = "A4CF12B3C4D5";
    private static final LocalDate DAY = LocalDate.of(2026, 9, 24);
    private static final String CONFIRMATION_TOPIC = "remediot/v1/A4CF12B3C4D5/eventos/confirmacao";

    @Autowired DeviceEventIngestService service;
    @Autowired UserRepository userRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired MedicationRepository medicationRepository;
    @Autowired DoseRepository doseRepository;
    @Autowired OpeningEventRepository openingEventRepository;
    @Autowired DeviceRepository deviceRepository;
    @Autowired DeviceEventRepository deviceEventRepository;
    @Autowired EntityManager entityManager;

    @MockitoBean DeviceMessagePublisher publisher;
    @MockitoBean AlertService alertService;

    private Patient patient;
    private MedicationSchedule morning;

    @BeforeEach
    void setUp() {
        patient = newPatient("paciente@example.com");
        morning = newSchedule(patient, 2, LocalTime.of(8, 0));
        Device device = new Device(DEVICE_ID);
        device.setPatient(patient);
        deviceRepository.save(device);
        flushAndClear();
    }

    @Test
    void doseConfirmadaRegistraNoHorarioRealDaAbertura() {
        service.handle(DEVICE_ID, doseEvent("A4CF12B3C4D5-00000001", "DOSE_CONFIRMADA", at(8, 5), morning));
        flushAndClear();

        Dose dose = doseOf(morning);
        assertEquals(DoseStatus.TOMADA, dose.getStatus());
        assertEquals(LocalDateTime.of(DAY, LocalTime.of(8, 5)), dose.getRegisteredAt());
        List<OpeningEvent> openings = openingEventRepository.findByDoseId(dose.getId());
        assertEquals(1, openings.size());
        assertEquals(OpeningEventSource.DEVICE, openings.get(0).getSource());
        verify(publisher).publish(CONFIRMATION_TOPIC, "{\"eventIds\":[\"A4CF12B3C4D5-00000001\"]}", 1, false);
        verify(alertService, never()).handleLateOrMissedDose(any());
    }

    @Test
    void doseAntecipadaViraTomada() {
        service.handle(DEVICE_ID, doseEvent("A4CF12B3C4D5-00000002", "DOSE_ANTECIPADA", at(7, 40), morning));
        flushAndClear();

        assertEquals(DoseStatus.TOMADA, doseOf(morning).getStatus());
    }

    @Test
    void doseAtrasadaViraAtrasadaEAlerta() {
        service.handle(DEVICE_ID, doseEvent("A4CF12B3C4D5-00000003", "DOSE_ATRASADA", at(9, 30), morning));
        flushAndClear();

        assertEquals(DoseStatus.ATRASADA, doseOf(morning).getStatus());
        verify(alertService).handleLateOrMissedDose(any());
    }

    @Test
    void reenvioNaoDuplicaRegistroNemAlertaMasEhConfirmado() {
        String json = doseEvent("A4CF12B3C4D5-00000004", "DOSE_ATRASADA", at(9, 30), morning);
        service.handle(DEVICE_ID, json);
        service.handle(DEVICE_ID, json.replace("\"reenvio\":false", "\"reenvio\":true"));
        flushAndClear();

        assertEquals(1, openingEventRepository.findByDoseId(doseOf(morning).getId()).size());
        assertEquals(1, deviceEventRepository.count());
        verify(alertService, times(1)).handleLateOrMissedDose(any());
        verify(publisher, times(2)).publish(eq(CONFIRMATION_TOPIC), anyString(), eq(1), eq(false));
    }

    @Test
    void deviceIdDiferenteDoTopicoEhDescartadoSemConfirmar() {
        service.handle("FFFFFFFFFFFF", doseEvent("A4CF12B3C4D5-00000005", "DOSE_CONFIRMADA", at(8, 5), morning));
        flushAndClear();

        assertEquals(0, deviceEventRepository.count());
        assertTrue(doseRepository.findByScheduleIdAndScheduledDate(morning.getId(), DAY).isEmpty());
        verifyNoInteractions(publisher);
    }

    @Test
    void tsNuloUsaHorarioDeChegadaEMarcaHoraNaoConfiavel() {
        service.handle(DEVICE_ID, envelope("A4CF12B3C4D5-00000006", "ABERTURA_NAO_CLASSIFICADA", null,
                "{\"motivo\":\"SEM_HORA\"}"));
        flushAndClear();

        DeviceEvent event = deviceEventRepository.findById("A4CF12B3C4D5-00000006").orElseThrow();
        assertFalse(event.isTimeReliable());
        assertEquals(event.getReceivedAt(), event.getOccurredAt());
        verify(publisher).publish(eq(CONFIRMATION_TOPIC), anyString(), eq(1), eq(false));
    }

    @Test
    void doseAtrasadaQueChegaDepoisDaNaoRetiradaValeOEvento() {
        service.handle(DEVICE_ID, doseEvent("A4CF12B3C4D5-00000007", "DOSE_PERDIDA", at(11, 0), morning));
        flushAndClear();
        assertEquals(DoseStatus.NAO_RETIRADA, doseOf(morning).getStatus());

        // A gaveta estava sem internet: a abertura das 09:30 chega depois
        service.handle(DEVICE_ID, doseEvent("A4CF12B3C4D5-00000008", "DOSE_ATRASADA", at(9, 30), morning));
        flushAndClear();

        assertEquals(DoseStatus.ATRASADA, doseOf(morning).getStatus());
    }

    @Test
    void tresRemediosNoMesmoHorarioViramTresDoses() {
        MedicationSchedule second = newSchedule(patient, 1, LocalTime.of(8, 0));
        MedicationSchedule third = newSchedule(patient, 3, LocalTime.of(8, 0));
        flushAndClear();

        service.handle(DEVICE_ID, doseEvent("A4CF12B3C4D5-00000009", "DOSE_CONFIRMADA", at(8, 5), morning));
        service.handle(DEVICE_ID, doseEvent("A4CF12B3C4D5-00000010", "DOSE_CONFIRMADA", at(8, 5), second));
        service.handle(DEVICE_ID, doseEvent("A4CF12B3C4D5-00000011", "DOSE_CONFIRMADA", at(8, 5), third));
        flushAndClear();

        for (MedicationSchedule schedule : List.of(morning, second, third)) {
            assertEquals(DoseStatus.TOMADA, doseOf(schedule).getStatus());
        }
    }

    @Test
    void dosePerdidaMarcaNaoRetiradaEAlertaUmaVez() {
        service.handle(DEVICE_ID, doseEvent("A4CF12B3C4D5-00000012", "DOSE_PERDIDA", at(11, 0), morning));
        flushAndClear();

        assertEquals(DoseStatus.NAO_RETIRADA, doseOf(morning).getStatus());
        verify(alertService, times(1)).handleLateOrMissedDose(any());
    }

    @Test
    void dosePerdidaNaoAlertaDeNovoSeOAppJaMarcouNaoRetirada() {
        Dose dose = doseRepository.save(new Dose(morning, DAY, LocalDateTime.of(DAY, morning.getTime())));
        dose.setStatus(DoseStatus.NAO_RETIRADA);
        flushAndClear();

        service.handle(DEVICE_ID, doseEvent("A4CF12B3C4D5-00000013", "DOSE_PERDIDA", at(11, 0), morning));

        verify(alertService, never()).handleLateOrMissedDose(any());
    }

    @Test
    void dosePerdidaComGavetaIndisponivelAvisaQueNaoDaParaSaber() {
        String json = envelope("A4CF12B3C4D5-00000014", "DOSE_PERDIDA", at(11, 0),
                doseData(morning, "\"motivo\":\"DISPOSITIVO_INDISPONIVEL\""));
        service.handle(DEVICE_ID, json);

        verify(alertService).handleLateOrMissedDose(any());
        verify(alertService).handleDeviceAlert(any(), any(), contains("desligada ou sem hora"));
    }

    @Test
    void doseEmAtrasoSoVaiParaOHistorico() {
        service.handle(DEVICE_ID, doseEvent("A4CF12B3C4D5-00000015", "DOSE_EM_ATRASO", at(8, 30), morning));
        flushAndClear();

        assertTrue(doseRepository.findByScheduleIdAndScheduledDate(morning.getId(), DAY).isEmpty());
        assertTrue(deviceEventRepository.existsById("A4CF12B3C4D5-00000015"));
        verifyNoInteractions(alertService);
    }

    @Test
    void aberturaForaDeHorarioAlertaOCuidador() {
        service.handle(DEVICE_ID, envelope("A4CF12B3C4D5-00000016", "ABERTURA_FORA_HORARIO", at(14, 0),
                "{\"proximaDose\":null}"));

        verify(alertService).handleDeviceAlert(any(), any(), eq("Gaveta aberta fora do horário de uma dose"));
    }

    @Test
    void gavetaNaoPareadaGravaEConfirmaSemMexerEmDoses() {
        service.handle("0123456789AB", doseEvent("0123456789AB-00000001", "DOSE_CONFIRMADA", at(8, 5), morning)
                .replace(DEVICE_ID, "0123456789AB"));
        flushAndClear();

        assertTrue(deviceRepository.findById("0123456789AB").orElseThrow().getPatient() == null);
        assertTrue(deviceEventRepository.existsById("0123456789AB-00000001"));
        assertTrue(doseRepository.findByScheduleIdAndScheduledDate(morning.getId(), DAY).isEmpty());
        verify(publisher).publish(eq("remediot/v1/0123456789AB/eventos/confirmacao"), anyString(), eq(1), eq(false));
    }

    @Test
    void doseIdDeOutroPacienteEhIgnorado() {
        Patient other = newPatient("outro@example.com");
        MedicationSchedule otherSchedule = newSchedule(other, 1, LocalTime.of(8, 0));
        flushAndClear();

        service.handle(DEVICE_ID, doseEvent("A4CF12B3C4D5-00000017", "DOSE_CONFIRMADA", at(8, 5), otherSchedule));
        flushAndClear();

        assertTrue(doseRepository.findByScheduleIdAndScheduledDate(otherSchedule.getId(), DAY).isEmpty());
    }

    @Test
    void tipoDesconhecidoEhGuardadoEConfirmado() {
        service.handle(DEVICE_ID, envelope("A4CF12B3C4D5-00000018", "TIPO_DO_FUTURO", at(8, 0), "{\"x\":1}"));
        flushAndClear();

        assertEquals("TIPO_DO_FUTURO", deviceEventRepository.findById("A4CF12B3C4D5-00000018").orElseThrow().getType());
        verify(publisher).publish(eq(CONFIRMATION_TOPIC), anyString(), eq(1), eq(false));
    }

    @Test
    void jsonInvalidoNaoQuebraNemConfirma() {
        assertDoesNotThrow(() -> service.handle(DEVICE_ID, "{isso não é json"));
        verifyNoInteractions(publisher);
    }

    // ---------------- auxiliares ----------------

    private Patient newPatient(String email) {
        User caregiver = userRepository.save(new User("Cuidador", "c-" + email, "hash", Role.CUIDADOR));
        User user = userRepository.save(new User("Paciente", email, "hash", Role.PACIENTE));
        return patientRepository.save(new Patient(user, caregiver, 70, "Rua A", 0.0, 0.0));
    }

    private MedicationSchedule newSchedule(Patient owner, int weight, LocalTime time) {
        Medication medication = new Medication(owner, "Remédio", "1 comp", weight);
        MedicationSchedule schedule = new MedicationSchedule(medication, time);
        medication.getSchedules().add(schedule);
        medicationRepository.save(medication);
        entityManager.flush();
        return schedule;
    }

    private Dose doseOf(MedicationSchedule schedule) {
        return doseRepository.findByScheduleIdAndScheduledDate(schedule.getId(), DAY).orElseThrow();
    }

    private static long at(int hour, int minute) {
        return ZonedDateTime.of(DAY, LocalTime.of(hour, minute), DEVICE_ZONE).toEpochSecond();
    }

    private static String doseEvent(String eventId, String type, long ts, MedicationSchedule schedule) {
        return envelope(eventId, type, ts, doseData(schedule, "\"peso\":2"));
    }

    private static String doseData(MedicationSchedule schedule, String extra) {
        long scheduled = ZonedDateTime.of(DAY, schedule.getTime(), DEVICE_ZONE).toEpochSecond();
        return "{\"doseId\":\"s" + schedule.getId() + "\",\"dataReferencia\":\"" + DAY + "\","
                + "\"horaPrevista\":\"" + schedule.getTime() + "\",\"horarioPrevisto\":" + scheduled + "," + extra + "}";
    }

    private static String envelope(String eventId, String type, Long ts, String data) {
        return "{\"eventId\":\"" + eventId + "\",\"deviceId\":\"" + DEVICE_ID + "\",\"tipo\":\"" + type + "\","
                + "\"ts\":" + ts + ",\"fonteHora\":\"NTP\",\"uptimeMs\":120360,\"boot\":1,\"fw\":\"2.2.0-mvp\","
                + "\"reenvio\":false,\"dados\":" + data + "}";
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
