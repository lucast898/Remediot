package br.edu.remediot.mqtt;

import br.edu.remediot.service.device.DeviceAckService;
import br.edu.remediot.service.device.DeviceEventIngestService;
import br.edu.remediot.service.device.DeviceStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Entrega cada mensagem recebida ao serviço do tipo dela. Nenhuma exceção pode
 * sair daqui: no Paho, uma exceção no callback derruba a conexão.
 */
@Component
public class DeviceMessageRouter {

    private static final Logger log = LoggerFactory.getLogger(DeviceMessageRouter.class);

    private final DeviceEventIngestService eventIngestService;
    private final DeviceStatusService statusService;
    private final DeviceAckService ackService;

    public DeviceMessageRouter(
            DeviceEventIngestService eventIngestService,
            DeviceStatusService statusService,
            DeviceAckService ackService
    ) {
        this.eventIngestService = eventIngestService;
        this.statusService = statusService;
        this.ackService = ackService;
    }

    public void route(String topic, String payload) {
        try {
            MqttTopics.parseInbound(topic).ifPresentOrElse(
                    inbound -> {
                        switch (inbound.kind()) {
                            case EVENTS -> eventIngestService.handle(inbound.deviceId(), payload);
                            case STATUS -> statusService.handle(inbound.deviceId(), payload);
                            case ACK -> ackService.handle(inbound.deviceId(), payload);
                        }
                    },
                    () -> log.warn("Mensagem MQTT em tópico inesperado descartada: {}", topic));
        } catch (Exception e) {
            log.error("Falha ao processar mensagem MQTT de {}: {}", topic, e.getMessage(), e);
        }
    }
}
