package br.edu.remediot.dto.device;

import java.time.Instant;

/**
 * Evento da gaveta no histórico do paciente. alert = pede "Fui eu"/"Ciente";
 * description já vem em pt-BR, sem afirmar ingestão.
 */
public record DeviceEventResponse(
        String eventId,
        String type,
        Instant occurredAt,
        boolean timeReliable,
        String description,
        boolean alert,
        boolean acknowledged,
        boolean caregiverOpening
) {
}
