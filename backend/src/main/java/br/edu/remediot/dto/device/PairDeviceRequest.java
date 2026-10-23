package br.edu.remediot.dto.device;

import jakarta.validation.constraints.NotBlank;

/** Os 4 caracteres do nome da rede da gaveta (RemedIoT-C4D5 -> "C4D5"), D7. */
public record PairDeviceRequest(
        @NotBlank(message = "Digite os 4 caracteres do nome da rede da gaveta") String code
) {
}
