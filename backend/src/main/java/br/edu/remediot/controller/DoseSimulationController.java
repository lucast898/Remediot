package br.edu.remediot.controller;

import br.edu.remediot.dto.DoseResponse;
import br.edu.remediot.entity.OpeningEventSource;
import br.edu.remediot.security.UserPrincipal;
import br.edu.remediot.service.AccessControlService;
import br.edu.remediot.service.DoseEventService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simulador de hardware (ver CLAUDE.md): sem o ESP32, este botão/endpoint
 * demonstra o fluxo completo de registro de dose na banca.
 */
@RestController
@RequestMapping("/api/doses")
public class DoseSimulationController {

    private final DoseEventService doseEventService;
    private final AccessControlService accessControlService;

    public DoseSimulationController(DoseEventService doseEventService, AccessControlService accessControlService) {
        this.doseEventService = doseEventService;
        this.accessControlService = accessControlService;
    }

    @PostMapping("/{doseId}/simulate-opening")
    public DoseResponse simulateOpening(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long doseId) {
        accessControlService.getAccessibleDose(doseId, principal);
        return doseEventService.registerOpening(doseId, OpeningEventSource.SIMULATOR);
    }
}
