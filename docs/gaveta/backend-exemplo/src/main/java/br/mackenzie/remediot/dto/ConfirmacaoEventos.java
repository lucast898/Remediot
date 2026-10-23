package br.mackenzie.remediot.dto;

import java.util.List;

/** Confirmação de recebimento (remediot/v1/{deviceId}/eventos/confirmacao). Pode vir em lote. */
public record ConfirmacaoEventos(List<String> eventIds) {
}
