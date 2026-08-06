package br.edu.remediot.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Formato provisório do evento entregue pelo Node-RED (fase futura). O
 * mapeamento real dose <-> compartimento físico será definido junto com o
 * hardware; por enquanto o próprio doseId identifica a dose.
 */
public record DeviceEventRequest(
        @NotNull(message = "doseId é obrigatório") Long doseId
) {
}
