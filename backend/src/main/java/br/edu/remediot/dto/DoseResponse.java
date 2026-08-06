package br.edu.remediot.dto;

import br.edu.remediot.entity.Dose;
import br.edu.remediot.entity.DoseStatus;

import java.time.LocalDateTime;

public record DoseResponse(
        Long id,
        Long medicationId,
        String medicationName,
        String medicationDose,
        Integer medicationWeight,
        LocalDateTime scheduledAt,
        DoseStatus status,
        LocalDateTime registeredAt
) {
    public static DoseResponse from(Dose dose) {
        var medication = dose.getSchedule().getMedication();
        return new DoseResponse(
                dose.getId(),
                medication.getId(),
                medication.getName(),
                medication.getDose(),
                medication.getWeight(),
                dose.getScheduledAt(),
                dose.getStatus(),
                dose.getRegisteredAt()
        );
    }
}
