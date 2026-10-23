package br.edu.remediot.dto.device;

import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * Resposta da gaveta a um comando ou a uma agenda (CONTRATO-MQTT.md, seções 7 e 9).
 * Para a agenda, commandId = configId e tipo = "AGENDA".
 */
public record DeviceAckMessage(
        String commandId,
        String deviceId,
        String tipo,
        String status,
        String mensagem,
        Long versao,
        Integer doses,
        List<AgendaError> erros,
        JsonNode dados,
        Long ts
) {

    public static final String TYPE_AGENDA = "AGENDA";

    public record AgendaError(String codigo, String doseId, String detalhe) {
    }
}
