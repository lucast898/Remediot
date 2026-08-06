package br.edu.remediot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public record UpdateMedicationRequest(
        @NotBlank(message = "Nome do medicamento é obrigatório") String name,
        @NotBlank(message = "Dose é obrigatória") String dose,
        @NotNull(message = "Peso é obrigatório") @Min(1) @Max(3) Integer weight,
        @NotEmpty(message = "Informe ao menos um horário") List<LocalTime> times
) {
}
