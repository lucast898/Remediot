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
        if (currentUser == null || patient == null || currentUser.getRole() == null) {
            return false;
        }
        if (currentUser.getRole() == Role.CUIDADOR) {
            return patient.getCaregiver().getId().equals(currentUser.getId());
        }
        if (currentUser.getRole() == Role.PACIENTE) {
            return patient.getUser().getId().equals(currentUser.getId());
        }
        return false;
    }
}
