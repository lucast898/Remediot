package br.edu.remediot.controller;

import br.edu.remediot.dto.DeviceEventRequest;
import br.edu.remediot.dto.DoseResponse;
import br.edu.remediot.entity.OpeningEventSource;
import br.edu.remediot.service.DoseEventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gancho reservado para a fase futura: o Node-RED chamará este endpoint a
 * partir do evento MQTT publicado pelo ESP32. Autenticado por chave de
 * dispositivo (não por JWT de usuário) — ver CLAUDE.md "Integrações futuras".
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceEventController {

    private static final String DEVICE_KEY_HEADER = "X-Device-Key";

    private final DoseEventService doseEventService;
    private final String deviceApiKey;

    public DeviceEventController(
            DoseEventService doseEventService,
            @Value("${remediot.device.api-key}") String deviceApiKey
    ) {
        this.doseEventService = doseEventService;
        this.deviceApiKey = deviceApiKey;
    }

    @PostMapping("/events")
    public DoseResponse receiveEvent(
            @RequestHeader(DEVICE_KEY_HEADER) String key,
            @Valid @RequestBody DeviceEventRequest request
    ) {
        if (!deviceApiKey.equals(key)) {
            throw new AccessDeniedException("Chave de dispositivo inválida");
        }
        return doseEventService.registerOpening(request.doseId(), OpeningEventSource.DEVICE);
    }
}
