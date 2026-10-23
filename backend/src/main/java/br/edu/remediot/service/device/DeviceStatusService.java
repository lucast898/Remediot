package br.edu.remediot.service.device;

import br.edu.remediot.dto.device.DeviceStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Status, heartbeat e LWT da gaveta (tópico remediot/v1/{deviceId}/status). */
@Service
public class DeviceStatusService {

    private static final Logger log = LoggerFactory.getLogger(DeviceStatusService.class);

    private final JsonMapper jsonMapper;

    public DeviceStatusService(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void handle(String topicDeviceId, String payload) {
        DeviceStatusMessage status;
        try {
            status = jsonMapper.readValue(payload, DeviceStatusMessage.class);
        } catch (JacksonException e) {
            log.warn("Status da gaveta {} com JSON inválido descartado: {}", topicDeviceId, e.getOriginalMessage());
            return;
        }
        log.info("Status da gaveta {}: online={} motivo={}", topicDeviceId, status.online(), status.motivo());
    }
}
