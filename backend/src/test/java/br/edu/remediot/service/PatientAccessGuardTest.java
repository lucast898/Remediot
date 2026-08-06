package br.edu.remediot.service;

import br.edu.remediot.entity.Patient;
import br.edu.remediot.entity.Role;
import br.edu.remediot.entity.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientAccessGuardTest {

    @Test
    void cuidadorDonoDaCarteiraPodeAcessarPaciente() throws Exception {
        User caregiver = userWithId(1L, Role.CUIDADOR);
        User patientUser = userWithId(2L, Role.PACIENTE);
        Patient patient = new Patient(patientUser, caregiver, 70, "Rua A", 0.0, 0.0);

        assertTrue(PatientAccessGuard.canAccess(caregiver, patient));
    }

    @Test
    void cuidadorDeOutraCarteiraNaoPodeAcessarPaciente() throws Exception {
        User caregiver = userWithId(1L, Role.CUIDADOR);
        User outroCuidador = userWithId(99L, Role.CUIDADOR);
        User patientUser = userWithId(2L, Role.PACIENTE);
        Patient patient = new Patient(patientUser, caregiver, 70, "Rua A", 0.0, 0.0);

        assertFalse(PatientAccessGuard.canAccess(outroCuidador, patient));
    }

    @Test
    void pacienteAcessaOsProipriosDados() throws Exception {
        User caregiver = userWithId(1L, Role.CUIDADOR);
        User patientUser = userWithId(2L, Role.PACIENTE);
        Patient patient = new Patient(patientUser, caregiver, 70, "Rua A", 0.0, 0.0);

        assertTrue(PatientAccessGuard.canAccess(patientUser, patient));
    }

    @Test
    void pacienteNaoAcessaDadosDeOutroPaciente() throws Exception {
        User caregiver = userWithId(1L, Role.CUIDADOR);
        User patientUser = userWithId(2L, Role.PACIENTE);
        User outroPaciente = userWithId(3L, Role.PACIENTE);
        Patient patient = new Patient(patientUser, caregiver, 70, "Rua A", 0.0, 0.0);

        assertFalse(PatientAccessGuard.canAccess(outroPaciente, patient));
    }

    private static User userWithId(Long id, Role role) throws Exception {
        User user = new User("Nome " + id, "user" + id + "@example.com", "hash", role);
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }
}
