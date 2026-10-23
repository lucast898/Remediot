package br.edu.remediot.dto.device;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Agenda publicada para a gaveta (CONTRATO-MQTT.md, seção 7). Sem diasSemana
 * (vale todos os dias, como no app, D2) e sem descricao (LGPD: nada de nome de remédio).
 */
public record AgendaMessage(long versao, String configId, List<Entry> doses) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Entry(String doseId, String hora, Integer peso) {
    }
}
