package br.edu.remediot.service.device;

import br.edu.remediot.dto.device.DeviceAckMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Respostas da gaveta a comandos e agendas (tópico remediot/v1/{deviceId}/ack). */
@Service
public class DeviceAckService {

    private static final Logger log = LoggerFactory.getLogger(DeviceAckService.class);

    private final JsonMapper jsonMapper;
    private final DeviceAgendaService agendaService;

    public DeviceAckService(JsonMapper jsonMapper, DeviceAgendaService agendaService) {
        this.jsonMapper = jsonMapper;
        this.agendaService = agendaService;
    }

    public void handle(String topicDeviceId, String payload) {
        DeviceAckMessage ack;
        try {
            ack = jsonMapper.readValue(payload, DeviceAckMessage.class);
        } catch (JacksonException e) {
            log.warn("Ack da gaveta {} com JSON inválido descartado: {}", topicDeviceId, e.getOriginalMessage());
            return;
        }
        if (ack.deviceId() != null && !ack.deviceId().equals(topicDeviceId)) {
            log.warn("deviceId do ack ({}) diferente do tópico ({}): descartado", ack.deviceId(), topicDeviceId);
            return;
        }
        if (DeviceAckMessage.TYPE_AGENDA.equals(ack.tipo())) {
            agendaService.handleAck(topicDeviceId, ack);
        } else {
            log.info("Ack da gaveta {}: {} {} {}", topicDeviceId, ack.tipo(), ack.commandId(), ack.status());
        }
    }
}
