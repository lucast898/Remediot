package br.edu.remediot.dto;

public record PatientAdherenceResponse(
        Long patientId,
        String fullName,
        String photoUrl,
        int scheduled,
        int takenOnTime,
        int takenLate,
        int missed,
        double adherencePercentage,
        boolean weightThreeLateAlert
) {
}
