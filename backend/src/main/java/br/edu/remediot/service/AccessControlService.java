package br.edu.remediot.service;

import br.edu.remediot.entity.Dose;
import br.edu.remediot.entity.Patient;
import br.edu.remediot.exception.ResourceNotFoundException;
import br.edu.remediot.repository.DoseRepository;
import br.edu.remediot.repository.PatientRepository;
import br.edu.remediot.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AccessControlService {

    private final PatientRepository patientRepository;
    private final DoseRepository doseRepository;

    public AccessControlService(PatientRepository patientRepository, DoseRepository doseRepository) {
        this.patientRepository = patientRepository;
        this.doseRepository = doseRepository;
    }

    public Patient getAccessiblePatient(Long patientId, UserPrincipal principal) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
        requireAccess(principal, patient);
        return patient;
    }

    public Dose getAccessibleDose(Long doseId, UserPrincipal principal) {
        Dose dose = doseRepository.findById(doseId)
                .orElseThrow(() -> new ResourceNotFoundException("Dose não encontrada"));
        Patient patient = dose.getSchedule().getMedication().getPatient();
        requireAccess(principal, patient);
        return dose;
    }

    private void requireAccess(UserPrincipal principal, Patient patient) {
        if (!PatientAccessGuard.canAccess(principal.getId(), principal.getRole(), patient)) {
            throw new AccessDeniedException("Você não tem acesso a este paciente");
        }
    }
}
