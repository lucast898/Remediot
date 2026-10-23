package br.edu.remediot.service.device;

import br.edu.remediot.dto.device.DeviceStatusMessage;
import br.edu.remediot.entity.Device;
import br.edu.remediot.repository.DeviceRepository;
import br.edu.remediot.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;

/**
 * Status, heartbeat e LWT da gaveta (CONTRATO-MQTT.md, seção 6). Uma gaveta que
 * nunca foi vista é criada sem paciente, aguardando o pareamento.
 */
@Service
public class DeviceStatusService {

    private static final Logger log = LoggerFactory.getLogger(DeviceStatusService.class);

    public static final Duration OFFLINE_ALERT_AFTER = Duration.ofMinutes(10);
    public static final String OFFLINE_ALERT_TYPE = "GAVETA_OFFLINE";

    private final JsonMapper jsonMapper;
    private final DeviceRepository deviceRepository;
    private final AlertService alertService;
    private final TransactionTemplate transactionTemplate;

    public DeviceStatusService(
            JsonMapper jsonMapper,
            DeviceRepository deviceRepository,
            AlertService alertService,
            PlatformTransactionManager transactionManager
    ) {
        this.jsonMapper = jsonMapper;
        this.deviceRepository = deviceRepository;
        this.alertService = alertService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void handle(String topicDeviceId, String payload) {
        DeviceStatusMessage status;
        try {
            status = jsonMapper.readValue(payload, DeviceStatusMessage.class);
        } catch (JacksonException e) {
            log.warn("Status da gaveta {} com JSON inválido descartado: {}", topicDeviceId, e.getOriginalMessage());
            return;
        }
        if (status.deviceId() != null && !status.deviceId().equals(topicDeviceId)) {
            log.warn("deviceId do status ({}) diferente do tópico ({}): descartado", status.deviceId(), topicDeviceId);
            return;
        }
        transactionTemplate.executeWithoutResult(tx -> apply(topicDeviceId, status, payload));
    }

    private void apply(String deviceId, DeviceStatusMessage status, String payload) {
        Device device = deviceRepository.findById(deviceId).orElseGet(() -> {
            log.info("Gaveta nova {} aguardando pareamento", deviceId);
            return deviceRepository.save(new Device(deviceId));
        });
        Instant now = Instant.now();

        if (Boolean.TRUE.equals(status.online())) {
            device.setOnline(true);
            device.setLastSeenAt(now);
            device.setOfflineReason(null);
            device.setOfflineSince(null);
            device.setOfflineAlertSent(false);
            device.setFirmwareVersion(status.fw());
            device.setTimeSource(status.fonteHora());
            device.setOfflineQueueSize(status.filaOffline());
            if (status.agendaVersao() != null) {
                device.setAppliedAgendaVersion(status.agendaVersao());
            }
            device.setLastStatusJson(payload);
            return;
        }

        // LWT ou despedida. O último heartbeat (lastStatusJson) continua guardado.
        String reason = status.motivo() != null ? status.motivo() : DeviceStatusMessage.REASON_CONNECTION_LOST;
        if (!device.isOnline() && reason.equals(device.getOfflineReason())) {
            // Mesma mensagem retida reentregue (ex.: o backend reiniciou): não reinicia a contagem
            return;
        }
        device.setOnline(false);
        device.setOfflineReason(reason);
        device.setOfflineSince(now);
        device.setOfflineAlertSent(false);
        log.info("Gaveta {} offline: {}", deviceId, reason);
    }

    /**
     * Só a queda sem aviso (LWT, CONEXAO_PERDIDA) vira alerta, uma vez, depois de
     * 10 min. Reinício e troca de Wi-Fi foram pedidos pelo próprio app.
     */
    @Scheduled(fixedRateString = "${remediot.device.offline-check-interval-ms:60000}")
    @Transactional
    public void alertLongOfflineDevices() {
        Instant cutoff = Instant.now().minus(OFFLINE_ALERT_AFTER);
        for (Device device : deviceRepository.findByOnlineFalseAndOfflineReasonAndOfflineAlertSentFalseAndOfflineSinceBefore(
                DeviceStatusMessage.REASON_CONNECTION_LOST, cutoff)) {
            device.setOfflineAlertSent(true);
            if (device.getPatient() != null) {
                alertService.handleDeviceAlert(device.getPatient(), OFFLINE_ALERT_TYPE,
                        "A gaveta está sem conexão há mais de 10 minutos. Confira se ela está ligada e com Wi-Fi.");
            }
        }
    }
}
