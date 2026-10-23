package br.mackenzie.remediot.dto;

import java.util.List;

/**
 * Agenda publicada em remediot/v1/{deviceId}/config/agenda (retained, QoS 1).
 * A versão só cresce: o ESP ignora versão igual e rejeita versão menor.
 * Usamos o instante da alteração em segundos (epoch) como versão, para ela
 * continuar crescendo mesmo se o banco for apagado. Cabe em 32 bits sem sinal
 * (o ESP guarda uint32): segundos, nunca milissegundos.
 */
public record Agenda(long versao, String configId, List<DoseAgenda> doses) {
}
