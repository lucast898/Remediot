package br.edu.remediot.service;

import br.edu.remediot.dto.DoseResponse;
import br.edu.remediot.entity.Dose;
import br.edu.remediot.entity.DoseStatus;
import br.edu.remediot.entity.OpeningEvent;
import br.edu.remediot.entity.OpeningEventSource;
import br.edu.remediot.exception.ResourceNotFoundException;
import br.edu.remediot.repository.DoseRepository;
import br.edu.remediot.repository.OpeningEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Ponto único de entrada para eventos de abertura do compartimento — hoje vindo
 * do simulador, amanhã do endpoint que o Node-RED chama a partir do MQTT do ESP32.
 */
@Service
public class DoseEventService {

    private final DoseRepository doseRepository;
    private final OpeningEventRepository openingEventRepository;
    private final DoseStatusService doseStatusService;
    private final AlertService alertService;

    public DoseEventService(
            DoseRepository doseRepository,
            OpeningEventRepository openingEventRepository,
            DoseStatusService doseStatusService,
            AlertService alertService
    ) {
        this.doseRepository = doseRepository;
        this.openingEventRepository = openingEventRepository;
        this.doseStatusService = doseStatusService;
        this.alertService = alertService;
    }

    @Transactional
    public DoseResponse registerOpening(Long doseId, OpeningEventSource source) {
        Dose dose = doseRepository.findById(doseId)
                .orElseThrow(() -> new ResourceNotFoundException("Dose não encontrada"));

        LocalDateTime now = LocalDateTime.now();
        openingEventRepository.save(new OpeningEvent(dose, now, source));

        DoseStatus newStatus = doseStatusService.resolveStatusForOpening(dose.getScheduledAt(), now);
        dose.setStatus(newStatus);
        dose.setRegisteredAt(now);
        Dose saved = doseRepository.save(dose);

        if (newStatus == DoseStatus.ATRASADA) {
            alertService.handleLateOrMissedDose(saved);
        }
        return DoseResponse.from(saved);
    }
}
