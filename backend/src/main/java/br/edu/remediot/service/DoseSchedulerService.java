package br.edu.remediot.service;

import br.edu.remediot.entity.Dose;
import br.edu.remediot.entity.DoseStatus;
import br.edu.remediot.repository.DoseRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DoseSchedulerService {

    private final DoseGenerationService doseGenerationService;
    private final DoseRepository doseRepository;
    private final AlertService alertService;

    public DoseSchedulerService(
            DoseGenerationService doseGenerationService,
            DoseRepository doseRepository,
            AlertService alertService
    ) {
        this.doseGenerationService = doseGenerationService;
        this.doseRepository = doseRepository;
        this.alertService = alertService;
    }

    @Scheduled(fixedRateString = "${remediot.scheduler.check-interval-ms:300000}")
    @Transactional
    public void checkMissedDoses() {
        doseGenerationService.ensureDosesForToday();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minus(DoseStatusService.MISSED_AFTER);
        List<Dose> missed = doseRepository.findByStatusAndScheduledAtBefore(DoseStatus.AGENDADA, cutoff);

        for (Dose dose : missed) {
            dose.setStatus(DoseStatus.NAO_RETIRADA);
            doseRepository.save(dose);
            alertService.handleLateOrMissedDose(dose);
        }
    }
}
