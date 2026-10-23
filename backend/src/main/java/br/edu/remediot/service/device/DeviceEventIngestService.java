package br.edu.remediot.service.device;

import br.edu.remediot.dto.device.DeviceEventMessage;
import br.edu.remediot.dto.device.DeviceEventType;
import br.edu.remediot.dto.device.DoseEventData;
import br.edu.remediot.dto.device.EventConfirmationMessage;
import br.edu.remediot.entity.*;
import br.edu.remediot.mqtt.DeviceMessagePublisher;
import br.edu.remediot.mqtt.MqttTopics;
import br.edu.remediot.repository.DeviceEventRepository;
import br.edu.remediot.repository.DeviceRepository;
import br.edu.remediot.repository.DoseRepository;
import br.edu.remediot.repository.MedicationScheduleRepository;
import br.edu.remediot.service.AlertService;
import br.edu.remediot.service.DoseEventService;
import br.edu.remediot.service.DoseGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Eventos da gaveta (CONTRATO-MQTT.md, seções 3 a 5).
 *
 * Fluxo: grava -> confirma. A confirmação só sai depois do commit; se o banco
 * falhar, não confirmamos e a gaveta reenvia sozinha. Reenvios (mesmo eventId)
 * não são processados de novo, mas são confirmados, senão a gaveta reenvia para
 * sempre.
 *
 * As regras de dose do app não mudam: a abertura entra pelo DoseEventService com
 * o horário real dela, e o status sai do DoseStatusService como no simulador.
 */
@Service
public class DeviceEventIngestService {

    private static final Logger log = LoggerFactory.getLogger(DeviceEventIngestService.class);

    /** O ts da gaveta é UTC; as doses do app estão no horário de São Paulo. */
    public static final ZoneId DEVICE_ZONE = ZoneId.of("America/Sao_Paulo");

    private static final Pattern SCHEDULE_DOSE_ID = Pattern.compile("^s(\\d{1,18})$");

    private final JsonMapper jsonMapper;
    private final DeviceRepository deviceRepository;
    private final DeviceEventRepository deviceEventRepository;
    private final MedicationScheduleRepository scheduleRepository;
    private final DoseRepository doseRepository;
    private final DoseGenerationService doseGenerationService;
    private final DoseEventService doseEventService;
    private final AlertService alertService;
    private final DeviceMessagePublisher publisher;
    private final TransactionTemplate transactionTemplate;

    public DeviceEventIngestService(
            JsonMapper jsonMapper,
            DeviceRepository deviceRepository,
            DeviceEventRepository deviceEventRepository,
            MedicationScheduleRepository scheduleRepository,
            DoseRepository doseRepository,
            DoseGenerationService doseGenerationService,
            DoseEventService doseEventService,
            AlertService alertService,
            DeviceMessagePublisher publisher,
            PlatformTransactionManager transactionManager
    ) {
        this.jsonMapper = jsonMapper;
        this.deviceRepository = deviceRepository;
        this.deviceEventRepository = deviceEventRepository;
        this.scheduleRepository = scheduleRepository;
        this.doseRepository = doseRepository;
        this.doseGenerationService = doseGenerationService;
        this.doseEventService = doseEventService;
        this.alertService = alertService;
        this.publisher = publisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void handle(String topicDeviceId, String payload) {
        DeviceEventMessage event;
        try {
            event = jsonMapper.readValue(payload, DeviceEventMessage.class);
        } catch (JacksonException e) {
            log.warn("Evento da gaveta {} com JSON inválido descartado: {}", topicDeviceId, e.getOriginalMessage());
            return;
        }
        if (event.eventId() == null || event.deviceId() == null || event.tipo() == null) {
            log.warn("Evento da gaveta {} sem eventId/deviceId/tipo descartado: {}", topicDeviceId, payload);
            return;
        }
        // Uma gaveta só publica eventos em nome dela mesma (CONTRATO-MQTT.md, seção 11)
        if (!event.deviceId().equals(topicDeviceId)) {
            log.warn("deviceId do evento ({}) diferente do tópico ({}): descartado", event.deviceId(), topicDeviceId);
            return;
        }

        try {
            transactionTemplate.executeWithoutResult(status -> store(event, payload));
        } catch (RuntimeException e) {
            log.error("Falha ao gravar o evento {}; sem confirmação, a gaveta vai reenviar", event.eventId(), e);
            return;
        }
        confirm(event.deviceId(), event.eventId());
    }

    private void store(DeviceEventMessage event, String payload) {
        if (deviceEventRepository.existsById(event.eventId())) {
            log.info("Evento {} repetido (reenvio={}): só confirmado", event.eventId(), event.reenvio());
            return;
        }
        Instant receivedAt = Instant.now();
        Device device = deviceRepository.findById(event.deviceId())
                .orElseGet(() -> deviceRepository.save(new Device(event.deviceId())));
        device.setLastSeenAt(receivedAt);

        boolean timeReliable = event.ts() != null;
        Instant occurredAt = timeReliable ? Instant.ofEpochSecond(event.ts()) : receivedAt;
        DeviceEvent saved = deviceEventRepository.save(new DeviceEvent(
                event.eventId(), device, event.tipo(), occurredAt, timeReliable, receivedAt, payload));
        log.info("Evento {} {} da gaveta {} gravado", event.tipo(), event.eventId(), event.deviceId());

        Patient patient = device.getPatient();
        if (patient == null) {
            log.info("Gaveta {} ainda não pareada: evento {} só fica no histórico", event.deviceId(), event.eventId());
            return;
        }
        apply(patient, saved, event, occurredAt);
    }

    private void apply(Patient patient, DeviceEvent saved, DeviceEventMessage event, Instant occurredAt) {
        DeviceEventType type = DeviceEventType.from(event.tipo());

        if (type.isDoseOpening()) {
            findDose(patient, event).ifPresent(dose -> doseEventService.registerOpening(
                    dose, LocalDateTime.ofInstant(occurredAt, DEVICE_ZONE), OpeningEventSource.DEVICE));
        } else if (type == DeviceEventType.DOSE_PERDIDA) {
            findDose(patient, event).ifPresent(dose -> markMissed(patient, saved, dose, event));
        } else if (type.isCaregiverAlert()) {
            alertService.handleDeviceAlert(patient, saved, DeviceEventDescriptions.describe(event.tipo(), event.dados()));
        }
        // DOSE_EM_ATRASO: a dose continua AGENDADA, só histórico. É o momento em que
        // a gaveta começa a apitar (peso 2 e 3); se o grupo quiser, dá para disparar
        // aqui o escalonamento "atrasado" mais cedo do que o DoseSchedulerService.
        // ALARME_*, REABASTECIMENTO, MODO_*, RELOGIO_RESTABELECIDO etc.: só histórico.
    }

    /**
     * 3 h sem abertura. O DoseSchedulerService do app pode já ter marcado a dose
     * como NAO_RETIRADA (e alertado): nesse caso não alertamos de novo. Se a dose já
     * tem abertura registrada, ela vale.
     */
    private void markMissed(Patient patient, DeviceEvent saved, Dose dose, DeviceEventMessage event) {
        if (dose.getStatus() != DoseStatus.AGENDADA) {
            log.info("DOSE_PERDIDA {} para dose {} já {}: nada muda", event.eventId(), dose.getId(), dose.getStatus());
            return;
        }
        dose.setStatus(DoseStatus.NAO_RETIRADA);
        doseRepository.save(dose);
        alertService.handleLateOrMissedDose(dose);

        DoseEventData data = readDoseData(event);
        if (data != null && "DISPOSITIVO_INDISPONIVEL".equals(data.motivo())) {
            alertService.handleDeviceAlert(patient, saved, DeviceEventDescriptions.describe(event.tipo(), event.dados()));
        }
    }

    /** A dose do par (MedicationSchedule, data de referência); criada sob demanda. */
    private Optional<Dose> findDose(Patient patient, DeviceEventMessage event) {
        DoseEventData data = readDoseData(event);
        if (data == null || data.doseId() == null) {
            log.warn("Evento {} sem dados de dose: ignorado", event.eventId());
            return Optional.empty();
        }
        Matcher matcher = SCHEDULE_DOSE_ID.matcher(data.doseId());
        if (!matcher.matches()) {
            log.warn("Evento {} com doseId {} fora do formato s<id>: ignorado", event.eventId(), data.doseId());
            return Optional.empty();
        }
        Optional<MedicationSchedule> schedule = scheduleRepository.findById(Long.parseLong(matcher.group(1)))
                .filter(s -> s.getMedication().getPatient().getId().equals(patient.getId()));
        if (schedule.isEmpty()) {
            // Remédio excluído depois do evento, ou doseId de outro paciente
            log.warn("Evento {}: horário {} não existe para o paciente {}", event.eventId(), data.doseId(), patient.getId());
            return Optional.empty();
        }
        LocalDate date = data.dataReferencia() != null
                ? data.dataReferencia()
                : LocalDate.ofInstant(Instant.now(), DEVICE_ZONE);
        return Optional.of(doseGenerationService.ensureDoseForDate(schedule.get(), date));
    }

    private DoseEventData readDoseData(DeviceEventMessage event) {
        if (event.dados() == null || event.dados().isNull()) {
            return null;
        }
        try {
            return jsonMapper.treeToValue(event.dados(), DoseEventData.class);
        } catch (JacksonException e) {
            log.warn("Evento {} com dados de dose inválidos: {}", event.eventId(), e.getOriginalMessage());
            return null;
        }
    }

    private void confirm(String deviceId, String eventId) {
        String json = jsonMapper.writeValueAsString(new EventConfirmationMessage(List.of(eventId)));
        publisher.publish(MqttTopics.eventConfirmation(deviceId), json, 1, false);
    }
}
