package br.edu.remediot.dto;

import br.edu.remediot.entity.Medication;

import java.time.LocalTime;
import java.util.List;

public record MedicationResponse(
        Long id,
        Long patientId,
        String name,
        String dose,
        Integer weight,
        List<LocalTime> times
) {
    public static MedicationResponse from(Medication medication) {
        List<LocalTime> times = medication.getSchedules().stream()
                .map(s -> s.getTime())
                .sorted()
                .toList();
        return new MedicationResponse(
                medication.getId(),
                medication.getPatient().getId(),
                medication.getName(),
                medication.getDose(),
                medication.getWeight(),
                times
        );
    }
}
