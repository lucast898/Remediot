package br.edu.remediot.service.device;

import br.edu.remediot.dto.device.AgendaMessage;
import br.edu.remediot.dto.device.DeviceAckMessage;
import br.edu.remediot.entity.Device;
import br.edu.remediot.entity.DeviceAgenda;
import br.edu.remediot.entity.DeviceAgendaStatus;
import br.edu.remediot.mqtt.DeviceMessagePublisher;
import br.edu.remediot.mqtt.MqttTopics;
import br.edu.remediot.repository.DeviceAgendaRepository;
import br.edu.remediot.repository.DeviceRepository;
import br.edu.remediot.repository.MedicationScheduleRepository;
import br.edu.remediot.service.MedicationsChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Agenda do app para a gaveta (CONTRATO-MQTT.md, seção 7): uma entrada por
 * MedicationSchedule, retained, versão sempre crescente, no máximo 40 entradas e
 * 7.500 bytes. Republicada depois do commit sempre que os remédios mudam.
 */
@Service
public class DeviceAgendaService {

    private static final Logger log = LoggerFactory.getLogger(DeviceAgendaService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DeviceRepository deviceRepository;
    private final DeviceAgendaRepository agendaRepository;
    private final MedicationScheduleRepository scheduleRepository;
    private final DeviceMessagePublisher publisher;
    private final JsonMapper jsonMapper;
    private final TransactionTemplate transaction;
    /** Depois do commit (listener), a transação antiga já terminou: precisa de uma nova. */
    private final TransactionTemplate newTransaction;

    public DeviceAgendaService(
            DeviceRepository deviceRepository,
            DeviceAgendaRepository agendaRepository,
            MedicationScheduleRepository scheduleRepository,
            DeviceMessagePublisher publisher,
            JsonMapper jsonMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.deviceRepository = deviceRepository;
        this.agendaRepository = agendaRepository;
        this.scheduleRepository = scheduleRepository;
        this.publisher = publisher;
        this.jsonMapper = jsonMapper;
        this.transaction = new TransactionTemplate(transactionManager);
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Remédio criado, editado ou excluído: republica a agenda, se o paciente tem gaveta. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMedicationsChanged(MedicationsChangedEvent event) {
        try {
            String deviceId = newTransaction.execute(status -> deviceRepository.findByPatientId(event.patientId())
                    .map(Device::getDeviceId)
                    .orElse(null));
            if (deviceId != null) {
                publishAgenda(deviceId, newTransaction);
            }
        } catch (RuntimeException e) {
            // A alteração do remédio já foi salva; a agenda pode ser reenviada depois
            // pelo botão "ressincronizar" ou na próxima alteração
            log.error("Falha ao publicar a agenda do paciente {}", event.patientId(), e);
        }
    }

    /**
     * Monta a agenda com os horários do paciente da gaveta (vazia se a gaveta não
     * está pareada), grava e publica. Se não passar na validação, grava como
     * REJEITADA com os erros e NÃO publica.
     */
    public DeviceAgenda publishAgenda(String deviceId) {
        return publishAgenda(deviceId, transaction);
    }

    /** Agenda republicada depois do commit de outra operação (ex.: pareamento). */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendaRequested(DeviceAgendaRequestedEvent event) {
        try {
            if (event.empty()) {
                publishEmptyAgenda(event.deviceId(), newTransaction);
            } else {
                publishAgenda(event.deviceId(), newTransaction);
            }
        } catch (RuntimeException e) {
            log.error("Falha ao publicar a agenda da gaveta {}", event.deviceId(), e);
        }
    }

    private DeviceAgenda publishAgenda(String deviceId, TransactionTemplate tx) {
        PreparedAgenda prepared = tx.execute(status -> {
            Device device = deviceRepository.findById(deviceId).orElseThrow();
            List<AgendaMessage.Entry> entries = device.getPatient() == null
                    ? List.of()
                    : AgendaRules.entriesFor(scheduleRepository.findByMedicationPatientId(device.getPatient().getId()));
            return prepare(device, entries);
        });
        if (prepared.publish()) {
            publisher.publish(MqttTopics.agenda(deviceId), prepared.agenda().getJson(), 1, true);
            log.info("Agenda {} (versão {}) publicada para a gaveta {}",
                    prepared.agenda().getConfigId(), prepared.agenda().getVersion(), deviceId);
        }
        return prepared.agenda();
    }

    /** Ao desvincular: agenda vazia, para a gaveta parar de acompanhar e de apitar. */
    public DeviceAgenda publishEmptyAgenda(String deviceId) {
        return publishEmptyAgenda(deviceId, transaction);
    }

    private DeviceAgenda publishEmptyAgenda(String deviceId, TransactionTemplate tx) {
        PreparedAgenda prepared = tx.execute(status ->
                prepare(deviceRepository.findById(deviceId).orElseThrow(), List.of()));
        publisher.publish(MqttTopics.agenda(deviceId), prepared.agenda().getJson(), 1, true);
        return prepared.agenda();
    }

    private PreparedAgenda prepare(Device device, List<AgendaMessage.Entry> entries) {
        Long previous = agendaRepository.findFirstByDeviceDeviceIdOrderByVersionDesc(device.getDeviceId())
                .map(DeviceAgenda::getVersion)
                .orElse(null);
        long version = AgendaRules.nextVersion(Instant.now().getEpochSecond(), previous);
        String configId = "cfg-" + HexFormat.of().formatHex(randomBytes(4));
        AgendaMessage message = new AgendaMessage(version, configId, entries);
        String json = jsonMapper.writeValueAsString(message);

        DeviceAgenda agenda = new DeviceAgenda(device, version, configId, json);
        List<String> errors = AgendaRules.validate(message, json);
        if (!errors.isEmpty()) {
            agenda.setStatus(DeviceAgendaStatus.REJEITADA);
            agenda.setErrors(jsonMapper.writeValueAsString(errors.stream()
                    .map(code -> Map.of("codigo", code))
                    .toList()));
            agenda.setAnsweredAt(Instant.now());
            log.warn("Agenda da gaveta {} não publicada: {}", device.getDeviceId(), errors);
        }
        return new PreparedAgenda(agendaRepository.save(agenda), errors.isEmpty());
    }

    /** Ack de agenda (commandId = configId). Ack de versão antiga só atualiza o histórico. */
    public void handleAck(String deviceId, DeviceAckMessage ack) {
        if (ack.commandId() == null) {
            log.warn("Gaveta {} não conseguiu ler uma agenda (JSON_INVALIDO): {}", deviceId, ack.erros());
            return;
        }
        transaction.executeWithoutResult(status -> agendaRepository.findByConfigId(ack.commandId())
                .filter(agenda -> agenda.getDevice().getDeviceId().equals(deviceId))
                .ifPresentOrElse(agenda -> applyAck(agenda, ack),
                        () -> log.warn("Ack da agenda {} desconhecida (gaveta {})", ack.commandId(), deviceId)));
    }

    private void applyAck(DeviceAgenda agenda, DeviceAckMessage ack) {
        boolean applied = "APLICADA".equals(ack.status());
        agenda.setStatus(applied ? DeviceAgendaStatus.APLICADA : DeviceAgendaStatus.REJEITADA);
        agenda.setAnsweredAt(Instant.now());
        if (ack.erros() != null && !ack.erros().isEmpty()) {
            agenda.setErrors(jsonMapper.writeValueAsString(ack.erros()));
        }
        Device device = agenda.getDevice();
        if (applied && (device.getAppliedAgendaVersion() == null || agenda.getVersion() > device.getAppliedAgendaVersion())) {
            device.setAppliedAgendaVersion(agenda.getVersion());
        }
        log.info("Gaveta {}: agenda {} (versão {}) {}", device.getDeviceId(), agenda.getConfigId(),
                agenda.getVersion(), agenda.getStatus());
    }

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private record PreparedAgenda(DeviceAgenda agenda, boolean publish) {
    }
}
