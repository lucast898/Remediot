package br.edu.remediot.dto.device;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Modo reabastecer: abrir a gaveta sem contar como dose. Padrão 10 min. */
public record RefillRequest(
        @Min(value = 1, message = "Mínimo de 1 minuto") @Max(value = 60, message = "Máximo de 60 minutos") Integer minutes
) {
}
