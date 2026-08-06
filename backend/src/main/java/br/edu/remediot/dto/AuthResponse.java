package br.edu.remediot.dto;

import br.edu.remediot.entity.Role;

public record AuthResponse(
        String token,
        Long userId,
        String fullName,
        Role role,
        Long patientId
) {
}
