package br.edu.remediot.dto;

import jakarta.validation.constraints.Positive;

public record UpdatePatientRequest(
        @Positive(message = "Idade deve ser positiva") Integer age,
        String address,
        Double latitude,
        Double longitude,
        String photoUrl
) {
}
