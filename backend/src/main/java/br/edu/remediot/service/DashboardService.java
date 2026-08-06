package br.edu.remediot.service;

import br.edu.remediot.dto.PatientAdherenceResponse;
import br.edu.remediot.entity.Dose;
import br.edu.remediot.entity.DoseStatus;
import br.edu.remediot.entity.Patient;
import br.edu.remediot.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private final PatientRepository patientRepository;
    private final DoseGenerationService doseGenerationService;

    public DashboardService(PatientRepository patientRepository, DoseGenerationService doseGenerationService) {
        this.patientRepository = patientRepository;
        this.doseGenerationService = doseGenerationService;
    }

    @Transactional
    public List<PatientAdherenceResponse> getAdherenceForCaregiver(Long caregiverId) {
        LocalDate today = LocalDate.now();
        return patientRepository.findByCaregiverId(caregiverId).stream()
                .map(patient -> buildSummary(patient, today))
                .toList();
    }

    private PatientAdherenceResponse buildSummary(Patient patient, LocalDate today) {
        List<Dose> doses = doseGenerationService.ensureDosesForPatientAndDate(patient.getId(), today);

        int takenOnTime = 0;
        int takenLate = 0;
        int missed = 0;
        boolean weightThreeLateAlert = false;

        for (Dose dose : doses) {
            int weight = dose.getSchedule().getMedication().getWeight();
            switch (dose.getStatus()) {
                case TOMADA -> takenOnTime++;
                case ATRASADA -> {
                    takenLate++;
                    if (weight == 3) {
                        weightThreeLateAlert = true;
                    }
                }
                case NAO_RETIRADA -> {
                    missed++;
                    if (weight == 3) {
                        weightThreeLateAlert = true;
                    }
                }
                case AGENDADA -> {
                    // ainda dentro da janela, não conta nem a favor nem contra a adesão
                }
            }
        }

        int scheduled = doses.size();
        int resolved = takenOnTime + takenLate + missed;
        double adherencePercentage = resolved == 0 ? 100.0 : (takenOnTime * 100.0) / resolved;

        return new PatientAdherenceResponse(
                patient.getId(),
                patient.getUser().getFullName(),
                patient.getPhotoUrl(),
                scheduled,
                takenOnTime,
                takenLate,
                missed,
                Math.round(adherencePercentage * 10) / 10.0,
                weightThreeLateAlert
        );
    }
}
