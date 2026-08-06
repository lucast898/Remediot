package br.edu.remediot.dto;

import br.edu.remediot.entity.Patient;

public record PatientResponse(
        Long id,
        String fullName,
        String email,
        Integer age,
        String address,
        Double latitude,
        Double longitude,
        String photoUrl,
        Long caregiverId,
        String caregiverFullName,
        String caregiverPhone
) {
    public static PatientResponse from(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getUser().getFullName(),
                patient.getUser().getEmail(),
                patient.getAge(),
                patient.getAddress(),
                patient.getLatitude(),
                patient.getLongitude(),
                patient.getPhotoUrl(),
                patient.getCaregiver().getId(),
                patient.getCaregiver().getFullName(),
                patient.getCaregiver().getPhone()
        );
    }
}
