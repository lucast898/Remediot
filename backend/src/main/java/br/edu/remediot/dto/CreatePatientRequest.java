package br.edu.remediot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePatientRequest(
        @NotBlank(message = "Nome é obrigatório") String fullName,
        @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") String email,
        @NotBlank(message = "Senha é obrigatória") @Size(min = 6, message = "Senha deve ter ao menos 6 caracteres") String password,
        @Positive(message = "Idade deve ser positiva") Integer age,
        String address,
        Double latitude,
        Double longitude,
        String photoUrl
) {
}
