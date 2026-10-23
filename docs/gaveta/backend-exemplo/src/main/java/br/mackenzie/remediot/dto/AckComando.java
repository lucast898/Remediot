package br.mackenzie.remediot.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Resposta do ESP a um comando ou a uma agenda (tópico remediot/v1/{deviceId}/ack).
 * Para agenda: commandId = configId, tipo = "AGENDA", e vêm versao/erros/doses.
 */
public record AckComando(
        String commandId,
        String deviceId,
        String tipo,
        StatusAck status,
        String mensagem,
        Long ts,
        JsonNode dados,
        Long versao,
        List<ErroAgenda> erros,
        Integer doses) {

    public record ErroAgenda(String codigo, String doseId, String detalhe) {
    }
}
