package br.edu.remediot.service;

import br.edu.remediot.entity.Patient;
import br.edu.remediot.entity.Role;
import br.edu.remediot.entity.User;

/**
 * Regra de isolamento: cuidador só acessa pacientes da própria carteira;
 * paciente só acessa os próprios dados. Ver CLAUDE.md — "vínculo crítico".
 */
public final class PatientAccessGuard {

    private PatientAccessGuard() {
    }

    public static boolean canAccess(User currentUser, Patient patient) {
        if (currentUser == null) {
            return false;
        }
        return canAccess(currentUser.getId(), currentUser.getRole(), patient);
    }

    public static boolean canAccess(Long currentUserId, Role currentRole, Patient patient) {
        if (currentUserId == null || patient == null || currentRole == null) {
            return false;
        }
        if (currentRole == Role.CUIDADOR) {
            return patient.getCaregiver().getId().equals(currentUserId);
        }
        if (currentRole == Role.PACIENTE) {
            return patient.getUser().getId().equals(currentUserId);
        }
        return false;
    }
}
