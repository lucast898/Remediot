package br.edu.remediot.dto.device;

import java.util.Map;

/** Comando para a gaveta (CONTRATO-MQTT.md, seção 8). Nunca vai retained. */
public record DeviceCommandMessage(
        String commandId,
        String tipo,
        long emitidoEm,
        Long validoAte,
        Map<String, Object> parametros
) {
}
