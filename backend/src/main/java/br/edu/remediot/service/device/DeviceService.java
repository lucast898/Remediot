package br.edu.remediot.service.device;

import br.edu.remediot.dto.device.DeviceEventResponse;
import br.edu.remediot.dto.device.DeviceEventType;
import br.edu.remediot.dto.device.DeviceResponse;
import br.edu.remediot.dto.device.DeviceStatusMessage;
import br.edu.remediot.entity.*;
import br.edu.remediot.exception.DeviceConflictException;
import br.edu.remediot.exception.ResourceNotFoundException;
import br.edu.remediot.repository.DeviceAgendaRepository;
import br.edu.remediot.repository.DeviceEventRepository;
import br.edu.remediot.repository.DeviceRepository;
import br.edu.remediot.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** Pareamento (D7), desvinculação e estado da gaveta de um paciente. */
@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    /** A gaveta tem de ter mandado status há pouco: acabou de ser configurada. */
    public static final Duration PAIRING_WINDOW = Duration.ofMinutes(15);

    private static final Pattern PAIRING_CODE = Pattern.compile("^[0-9A-F]{4}$");

    private final DeviceRepository deviceRepository;
    private final DeviceAgendaRepository agendaRepository;
    private final DeviceEventRepository eventRepository;
    private final PatientRepository patientRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final JsonMapper jsonMapper;

    public DeviceService(
            DeviceRepository deviceRepository,
            DeviceAgendaRepository agendaRepository,
            DeviceEventRepository eventRepository,
            PatientRepository patientRepository,
            ApplicationEventPublisher eventPublisher,
            JsonMapper jsonMapper
    ) {
        this.deviceRepository = deviceRepository;
        this.agendaRepository = agendaRepository;
        this.eventRepository = eventRepository;
        this.patientRepository = patientRepository;
        this.eventPublisher = eventPublisher;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(readOnly = true)
    public DeviceResponse getForPatient(Long patientId) {
        return deviceRepository.findByPatientId(patientId)
                .map(this::toResponse)
                .orElseGet(DeviceResponse::notPaired);
    }

    @Transactional
    public DeviceResponse pair(Long patientId, String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        if (code.startsWith("REMEDIOT-")) {
            code = code.substring("REMEDIOT-".length());
        }
        if (!PAIRING_CODE.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "Digite os 4 caracteres que aparecem no fim do nome da rede da gaveta (ex.: RemedIoT-C4D5 → C4D5)");
        }
        if (deviceRepository.findByPatientId(patientId).isPresent()) {
            throw new DeviceConflictException("Este paciente já tem uma gaveta. Desvincule a atual antes de conectar outra.");
        }
        List<Device> candidates = deviceRepository.findByPatientIsNullAndDeviceIdEndingWithAndLastSeenAtAfter(
                code, Instant.now().minus(PAIRING_WINDOW));
        if (candidates.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma gaveta com esse código está conectada agora. "
                    + "Confira os 4 caracteres e se a gaveta terminou de se conectar ao Wi-Fi da casa.");
        }
        if (candidates.size() > 1) {
            throw new DeviceConflictException("Mais de uma gaveta com esse código está aguardando conexão. "
                    + "Desligue as outras gavetas da tomada e tente de novo.");
        }
        Device device = candidates.get(0);
        Patient patient = patientRepository.getReferenceById(patientId);
        device.setPatient(patient);
        device.setPairedAt(Instant.now());
        deviceRepository.save(device);
        log.info("Gaveta {} pareada com o paciente {}", device.getDeviceId(), patientId);

        eventPublisher.publishEvent(new DeviceAgendaRequestedEvent(device.getDeviceId(), false));
        return toResponse(device);
    }

    @Transactional
    public void unpair(Long patientId) {
        Device device = requirePairedDevice(patientId);
        device.setPatient(null);
        device.setPairedAt(null);
        log.info("Gaveta {} desvinculada do paciente {}", device.getDeviceId(), patientId);
        // Agenda vazia: a gaveta para de acompanhar doses e de apitar
        eventPublisher.publishEvent(new DeviceAgendaRequestedEvent(device.getDeviceId(), true));
    }

    @Transactional(readOnly = true)
    public Device requirePairedDevice(Long patientId) {
        return deviceRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Este paciente não tem gaveta conectada"));
    }

    @Transactional(readOnly = true)
    public List<DeviceEventResponse> listEvents(Long patientId, int limit) {
        Device device = requirePairedDevice(patientId);
        int size = Math.max(1, Math.min(limit, 200));
        return eventRepository.findByDeviceDeviceIdAndReceivedAtGreaterThanEqualOrderByOccurredAtDesc(
                        device.getDeviceId(), historyStart(device), PageRequest.of(0, size)).stream()
                .map(this::toEventResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeviceEventResponse> listPendingAlerts(Long patientId) {
        Device device = requirePairedDevice(patientId);
        return pendingAlerts(device).stream().map(this::toEventResponse).toList();
    }

    private List<DeviceEvent> pendingAlerts(Device device) {
        return eventRepository.findByDeviceDeviceIdAndReceivedAtGreaterThanEqualAndTypeInAndAcknowledgedFalseOrderByOccurredAtDesc(
                device.getDeviceId(), historyStart(device), DeviceEventType.acknowledgeableTypeNames());
    }

    /** Eventos de antes do pareamento (de outro dono da gaveta) não aparecem. */
    private static Instant historyStart(Device device) {
        return device.getPairedAt() != null ? device.getPairedAt() : Instant.EPOCH;
    }

    DeviceEventResponse toEventResponse(DeviceEvent event) {
        return new DeviceEventResponse(
                event.getEventId(),
                event.getType(),
                event.getOccurredAt(),
                event.isTimeReliable(),
                DeviceEventDescriptions.describe(event.getType(), dataOf(event)),
                DeviceEventType.from(event.getType()).isAcknowledgeable(),
                event.isAcknowledged(),
                event.getClassification() == DeviceEventClassification.CAREGIVER);
    }

    private DeviceResponse toResponse(Device device) {
        DeviceStatusMessage status = lastStatus(device);
        DeviceAgenda agenda = agendaRepository.findFirstByDeviceDeviceIdOrderByVersionDesc(device.getDeviceId())
                .orElse(null);
        return new DeviceResponse(
                true,
                device.getDeviceId(),
                device.isOnline(),
                device.getOfflineReason(),
                device.getLastSeenAt(),
                device.getPairedAt(),
                device.getFirmwareVersion(),
                device.getTimeSource(),
                device.getOfflineQueueSize(),
                status != null ? status.rssi() : null,
                status != null ? status.gaveta() : null,
                status != null ? status.alarmeAtivo() : null,
                status != null ? status.modoReabastecer() : null,
                pendingAlerts(device).size(),
                agendaStatus(device, agenda),
                agenda != null ? agenda.getVersion() : null,
                device.getAppliedAgendaVersion(),
                agendaErrors(agenda));
    }

    private static String agendaStatus(Device device, DeviceAgenda agenda) {
        if (agenda == null) {
            return "SEM_AGENDA";
        }
        if (agenda.getStatus() == DeviceAgendaStatus.REJEITADA) {
            return "REJEITADA";
        }
        Long applied = device.getAppliedAgendaVersion();
        return applied != null && applied >= agenda.getVersion() ? "SINCRONIZADA" : "AGUARDANDO";
    }

    private List<String> agendaErrors(DeviceAgenda agenda) {
        if (agenda == null || agenda.getStatus() != DeviceAgendaStatus.REJEITADA || agenda.getErrors() == null) {
            return List.of();
        }
        try {
            List<Map<String, Object>> errors = jsonMapper.readValue(agenda.getErrors(), new TypeReference<>() { });
            return errors.stream()
                    .map(error -> Objects.toString(error.get("codigo"), null))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (JacksonException e) {
            return List.of();
        }
    }

    private DeviceStatusMessage lastStatus(Device device) {
        if (device.getLastStatusJson() == null) {
            return null;
        }
        try {
            return jsonMapper.readValue(device.getLastStatusJson(), DeviceStatusMessage.class);
        } catch (JacksonException e) {
            return null;
        }
    }

    private tools.jackson.databind.JsonNode dataOf(DeviceEvent event) {
        try {
            return jsonMapper.readTree(event.getPayload()).get("dados");
        } catch (JacksonException e) {
            return null;
        }
    }
}
