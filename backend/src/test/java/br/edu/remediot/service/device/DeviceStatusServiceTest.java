package br.edu.remediot.service.device;

import br.edu.remediot.entity.*;
import br.edu.remediot.repository.DeviceRepository;
import br.edu.remediot.repository.PatientRepository;
import br.edu.remediot.repository.UserRepository;
import br.edu.remediot.service.AlertService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Status, heartbeat e LWT (CONTRATO-MQTT.md, seção 6). */
@SpringBootTest
@Transactional
class DeviceStatusServiceTest {

    private static final String DEVICE_ID = "A4CF12B3C4D5";
    private static final String HEARTBEAT = """
            {"online":true,"deviceId":"A4CF12B3C4D5","ts":1790244059,"uptimeSeg":63,"rssi":-61,
             "fw":"2.2.0-mvp","fonteHora":"NTP","ultimaSyncNtp":1790244000,"gaveta":"FECHADA",
             "alarmeAtivo":false,"alertasPendentes":1,"modoReabastecer":false,"agendaVersao":7,
             "filaOffline":2,"filaDescartados":0,"heapLivre":182344,"motivoReset":"POWERON"}""";
    private static final String LWT = "{\"online\":false,\"deviceId\":\"A4CF12B3C4D5\",\"motivo\":\"CONEXAO_PERDIDA\"}";

    @Autowired DeviceStatusService service;
    @Autowired DeviceRepository deviceRepository;
    @Autowired UserRepository userRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired EntityManager entityManager;

    @MockitoBean AlertService alertService;

    @Test
    void gavetaDesconhecidaFicaAguardandoPareamento() {
        service.handle(DEVICE_ID, HEARTBEAT);
        flushAndClear();

        Device device = deviceRepository.findById(DEVICE_ID).orElseThrow();
        assertNull(device.getPatient());
        assertTrue(device.isOnline());
        assertNotNull(device.getLastSeenAt());
        assertEquals("2.2.0-mvp", device.getFirmwareVersion());
        assertEquals("NTP", device.getTimeSource());
        assertEquals(2, device.getOfflineQueueSize());
        assertEquals(7L, device.getAppliedAgendaVersion());
        assertEquals(HEARTBEAT, device.getLastStatusJson());
    }

    @Test
    void lwtMarcaOfflineEGuardaOUltimoHeartbeat() {
        service.handle(DEVICE_ID, HEARTBEAT);
        service.handle(DEVICE_ID, LWT);
        flushAndClear();

        Device device = deviceRepository.findById(DEVICE_ID).orElseThrow();
        assertFalse(device.isOnline());
        assertEquals("CONEXAO_PERDIDA", device.getOfflineReason());
        assertNotNull(device.getOfflineSince());
        assertEquals(HEARTBEAT, device.getLastStatusJson());
    }

    @Test
    void lwtRetidoReentregueNaoReiniciaAContagem() {
        service.handle(DEVICE_ID, LWT);
        flushAndClear();
        Device device = deviceRepository.findById(DEVICE_ID).orElseThrow();
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1)).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        device.setOfflineSince(oneHourAgo);
        flushAndClear();

        service.handle(DEVICE_ID, LWT);
        flushAndClear();

        assertEquals(oneHourAgo, deviceRepository.findById(DEVICE_ID).orElseThrow().getOfflineSince());
    }

    @Test
    void offlineHaMaisDeDezMinutosAlertaOCuidadorUmaVez() {
        Device device = pairedDeviceOffline("CONEXAO_PERDIDA", Duration.ofMinutes(11));

        service.alertLongOfflineDevices();
        service.alertLongOfflineDevices();

        verify(alertService, times(1)).handleDeviceAlert(any(), eq("GAVETA_OFFLINE"), contains("sem conexão"));
        assertTrue(deviceRepository.findById(device.getDeviceId()).orElseThrow().isOfflineAlertSent());
    }

    @Test
    void offlineHaPoucoTempoNaoAlerta() {
        pairedDeviceOffline("CONEXAO_PERDIDA", Duration.ofMinutes(5));

        service.alertLongOfflineDevices();

        verifyNoInteractions(alertService);
    }

    @Test
    void reinicioETrocaDeWifiPedidosPeloAppNaoAlertam() {
        pairedDeviceOffline("TROCANDO_WIFI", Duration.ofHours(2));

        service.alertLongOfflineDevices();

        verifyNoInteractions(alertService);
    }

    @Test
    void heartbeatDepoisDaQuedaVoltaOnline() {
        pairedDeviceOffline("CONEXAO_PERDIDA", Duration.ofMinutes(30));

        service.handle(DEVICE_ID, HEARTBEAT);
        flushAndClear();

        Device device = deviceRepository.findById(DEVICE_ID).orElseThrow();
        assertTrue(device.isOnline());
        assertNull(device.getOfflineReason());
    }

    @Test
    void statusComDeviceIdDiferenteDoTopicoEhDescartado() {
        service.handle("0123456789AB", HEARTBEAT);

        assertTrue(deviceRepository.findById("0123456789AB").isEmpty());
        assertTrue(deviceRepository.findById(DEVICE_ID).isEmpty());
    }

    private Device pairedDeviceOffline(String reason, Duration offlineFor) {
        User caregiver = userRepository.save(new User("Cuidador", "cuidador@example.com", "hash", Role.CUIDADOR));
        User user = userRepository.save(new User("Paciente", "paciente@example.com", "hash", Role.PACIENTE));
        Patient patient = patientRepository.save(new Patient(user, caregiver, 70, "Rua A", 0.0, 0.0));
        Device device = new Device(DEVICE_ID);
        device.setPatient(patient);
        device.setOnline(false);
        device.setOfflineReason(reason);
        device.setOfflineSince(Instant.now().minus(offlineFor));
        deviceRepository.save(device);
        flushAndClear();
        return device;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
