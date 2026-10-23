package br.edu.remediot.service.device;

import br.edu.remediot.dto.device.DeviceAckMessage;
import br.edu.remediot.dto.device.DeviceCommandMessage;
import br.edu.remediot.dto.device.DeviceCommandResponse;
import br.edu.remediot.dto.device.DeviceEventResponse;
import br.edu.remediot.dto.device.DeviceEventType;
import br.edu.remediot.entity.*;
import br.edu.remediot.exception.ResourceNotFoundException;
import br.edu.remediot.mqtt.DeviceMessagePublisher;
import br.edu.remediot.mqtt.MqttTopics;
import br.edu.remediot.repository.DeviceCommandRepository;
import br.edu.remediot.repository.DeviceEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

/**
 * Comandos para a gaveta (CONTRATO-MQTT.md, seções 8 e 9): QoS 1, NUNCA retained
 * (um REINICIAR retido reiniciaria a gaveta a cada reconexão), válidos por 120 s.
 * Sem ack em 30 s, o comando vira SEM_RESPOSTA.
 */
@Service
public class DeviceCommandService {

    private static final Logger log = LoggerFactory.getLogger(DeviceCommandService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    public static final Duration VALIDITY = Duration.ofSeconds(120);
    public static final Duration ACK_TIMEOUT = Duration.ofSeconds(30);

    public static final String REFILL = "MODO_REABASTECER";
    public static final String ACKNOWLEDGE_ALERT = "RECONHECER_ALERTA";
    public static final String LOCATE = "LOCALIZAR";
    public static final String SILENCE_ALARM = "SILENCIAR_ALARME";
    public static final String REQUEST_STATUS = "PEDIR_STATUS";
    public static final String RESYNC_AGENDA = "RESSINCRONIZAR_AGENDA";
    public static final String RESTART = "REINICIAR";
    public static final String RESET_WIFI = "RESETAR_WIFI";

    private final DeviceCommandRepository commandRepository;
    private final DeviceEventRepository eventRepository;
    private final DeviceService deviceService;
    private final DeviceMessagePublisher publisher;
    private final JsonMapper jsonMapper;
    private final TransactionTemplate transaction;

    public DeviceCommandService(
            DeviceCommandRepository commandRepository,
            DeviceEventRepository eventRepository,
            DeviceService deviceService,
            DeviceMessagePublisher publisher,
            JsonMapper jsonMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.commandRepository = commandRepository;
        this.eventRepository = eventRepository;
        this.deviceService = deviceService;
        this.publisher = publisher;
        this.jsonMapper = jsonMapper;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public DeviceCommandResponse startRefill(Long patientId, Integer minutes) {
        return send(patientId, REFILL, Map.of("acao", "INICIAR", "duracaoMin", minutes != null ? minutes : 10));
    }

    public DeviceCommandResponse stopRefill(Long patientId) {
        return send(patientId, REFILL, Map.of("acao", "ENCERRAR"));
    }

    public DeviceCommandResponse locate(Long patientId) {
        return send(patientId, LOCATE, Map.of("duracaoSeg", 10));
    }

    public DeviceCommandResponse send(Long patientId, String type) {
        return send(patientId, type, Map.of());
    }

    /**
     * "Fui eu" (abertura fora de horário feita pelo cuidador) ou "Ciente" (demais
     * alertas): marca o evento e pede para a gaveta tirar o LED vermelho (seção 10).
     */
    public DeviceCommandResponse acknowledgeAlert(Long patientId, String eventId) {
        transaction.executeWithoutResult(status -> {
            DeviceEventResponse visible = deviceService.listPendingAlerts(patientId).stream()
                    .filter(alert -> alert.eventId().equals(eventId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Alerta não encontrado ou já resolvido"));
            // listPendingAlerts já filtra pela gaveta do paciente, desde o pareamento
            DeviceEvent event = eventRepository.findById(visible.eventId()).orElseThrow();
            event.setAcknowledged(true);
            if (DeviceEventType.from(event.getType()) == DeviceEventType.ABERTURA_FORA_HORARIO) {
                event.setClassification(DeviceEventClassification.CAREGIVER);
            }
        });
        return send(patientId, ACKNOWLEDGE_ALERT, Map.of("eventId", eventId));
    }

    private DeviceCommandResponse send(Long patientId, String type, Map<String, Object> parameters) {
        Instant now = Instant.now();
        String commandId = "cmd-" + HexFormat.of().formatHex(randomBytes(4));
        DeviceCommandMessage message = new DeviceCommandMessage(
                commandId, type, now.getEpochSecond(), now.plus(VALIDITY).getEpochSecond(), parameters);

        Sent sent = transaction.execute(status -> {
            Device device = deviceService.requirePairedDevice(patientId);
            DeviceCommand command = commandRepository.save(new DeviceCommand(commandId, device, type, now));
            return new Sent(device.getDeviceId(), DeviceCommandResponse.from(command));
        });
        // Depois de gravar: o ack procura o comando pelo commandId
        publisher.publish(MqttTopics.command(sent.deviceId()), jsonMapper.writeValueAsString(message), 1, false);
        log.info("Comando {} {} enviado para a gaveta {}", type, commandId, sent.deviceId());
        return sent.response();
    }

    @Transactional(readOnly = true)
    public DeviceCommandResponse get(Long patientId, String commandId) {
        Device device = deviceService.requirePairedDevice(patientId);
        return commandRepository.findById(commandId)
                .filter(command -> command.getDevice().getDeviceId().equals(device.getDeviceId()))
                .map(DeviceCommandResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Comando não encontrado"));
    }

    public void handleAck(String deviceId, DeviceAckMessage ack) {
        if (ack.commandId() == null) {
            log.warn("Gaveta {} não conseguiu ler um comando: {}", deviceId, ack.mensagem());
            return;
        }
        transaction.executeWithoutResult(status -> commandRepository.findById(ack.commandId())
                .filter(command -> command.getDevice().getDeviceId().equals(deviceId))
                .ifPresentOrElse(command -> {
                    command.setStatus(statusOf(ack.status()));
                    command.setMessage(ack.mensagem());
                    command.setAnsweredAt(Instant.now());
                    log.info("Gaveta {}: comando {} {} -> {} {}", deviceId, command.getType(),
                            command.getCommandId(), command.getStatus(), ack.mensagem());
                }, () -> log.warn("Ack de comando desconhecido {} (gaveta {})", ack.commandId(), deviceId)));
    }

    /** Os acks não passam pela fila offline da gaveta: sem resposta em 30 s, desiste. */
    @Scheduled(fixedRateString = "${remediot.device.command-timeout-check-ms:10000}")
    @Transactional
    public void markUnanswered() {
        for (DeviceCommand command : commandRepository.findByStatusAndSentAtBefore(
                DeviceCommandStatus.PENDENTE, Instant.now().minus(ACK_TIMEOUT))) {
            command.setStatus(DeviceCommandStatus.SEM_RESPOSTA);
        }
    }

    private static DeviceCommandStatus statusOf(String ackStatus) {
        if (ackStatus == null) {
            return DeviceCommandStatus.ERRO;
        }
        return switch (ackStatus) {
            case "OK" -> DeviceCommandStatus.OK;
            case "IGNORADO" -> DeviceCommandStatus.IGNORADO;
            case "DUPLICADO" -> DeviceCommandStatus.DUPLICADO;
            case "EXPIRADO" -> DeviceCommandStatus.EXPIRADO;
            default -> DeviceCommandStatus.ERRO;
        };
    }

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private record Sent(String deviceId, DeviceCommandResponse response) {
    }
}
