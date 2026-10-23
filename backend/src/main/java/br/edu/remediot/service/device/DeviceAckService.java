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

    public DeviceAckService(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void handle(String topicDeviceId, String payload) {
        DeviceAckMessage ack;
        try {
            ack = jsonMapper.readValue(payload, DeviceAckMessage.class);
        } catch (JacksonException e) {
            log.warn("Ack da gaveta {} com JSON inválido descartado: {}", topicDeviceId, e.getOriginalMessage());
            return;
        }
        log.info("Ack da gaveta {}: {} {} {}", topicDeviceId, ack.tipo(), ack.commandId(), ack.status());
    }
}
