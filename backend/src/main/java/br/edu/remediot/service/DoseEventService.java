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
 * Ponto único de entrada para eventos de abertura do compartimento: o simulador,
 * a ponte-app provisória (POST /api/devices/events) e a gaveta por MQTT
 * (DeviceEventIngestService).
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
        return registerOpening(dose, LocalDateTime.now(), source);
    }

    /**
     * Abertura num horário conhecido. A gaveta usa este caminho com o horário real
     * da abertura (pode chegar atrasado, se ela estava sem internet).
     */
    @Transactional
    public DoseResponse registerOpening(Dose dose, LocalDateTime openedAt, OpeningEventSource source) {
        openingEventRepository.save(new OpeningEvent(dose, openedAt, source));

        DoseStatus newStatus = doseStatusService.resolveStatusForOpening(dose.getScheduledAt(), openedAt);
        dose.setStatus(newStatus);
        dose.setRegisteredAt(openedAt);
        Dose saved = doseRepository.save(dose);

        if (newStatus == DoseStatus.ATRASADA) {
            alertService.handleLateOrMissedDose(saved);
        }
        return DoseResponse.from(saved);
    }
}
