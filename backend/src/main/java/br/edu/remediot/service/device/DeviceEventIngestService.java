package br.edu.remediot.service.device;

import br.edu.remediot.dto.device.DeviceEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Eventos da gaveta (tópico remediot/v1/{deviceId}/eventos). */
@Service
public class DeviceEventIngestService {

    private static final Logger log = LoggerFactory.getLogger(DeviceEventIngestService.class);

    private final JsonMapper jsonMapper;

    public DeviceEventIngestService(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void handle(String topicDeviceId, String payload) {
        DeviceEventMessage event;
        try {
            event = jsonMapper.readValue(payload, DeviceEventMessage.class);
        } catch (JacksonException e) {
            log.warn("Evento da gaveta {} com JSON inválido descartado: {}", topicDeviceId, e.getOriginalMessage());
            return;
        }
        log.info("Evento {} {} recebido da gaveta {}", event.tipo(), event.eventId(), topicDeviceId);
    }
}
