package br.edu.remediot.dto.device;

import tools.jackson.databind.JsonNode;

/**
 * Envelope de evento da gaveta (CONTRATO-MQTT.md, seção 4.1). O "tipo" fica como
 * String: um tipo novo do firmware não pode quebrar a leitura (ver DeviceEventType).
 * "dados" depende do tipo e é lido depois (ex.: DoseEventData).
 */
public record DeviceEventMessage(
        String eventId,
        String deviceId,
        String tipo,
        Long ts,
        String fonteHora,
        Boolean tsEstimado,
        Long uptimeMs,
        Long boot,
        String fw,
        Boolean reenvio,
        JsonNode dados
) {
}
