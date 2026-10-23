package br.edu.remediot.controller;

import br.edu.remediot.dto.device.DeviceCommandResponse;
import br.edu.remediot.dto.device.RefillRequest;
import br.edu.remediot.security.UserPrincipal;
import br.edu.remediot.service.AccessControlService;
import br.edu.remediot.service.device.DeviceCommandService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Comandos para a gaveta. Cuidador e paciente usam reabastecer, "fui eu" e os
 * demais (D10); trocar Wi-Fi e reiniciar são só do cuidador. A resposta vem
 * PENDENTE: o app consulta GET /commands/{commandId} até a gaveta responder.
 */
@RestController
@RequestMapping("/api/patients/{patientId}/device")
public class DeviceCommandController {

    private final DeviceCommandService commandService;
    private final AccessControlService accessControlService;

    public DeviceCommandController(DeviceCommandService commandService, AccessControlService accessControlService) {
        this.commandService = commandService;
        this.accessControlService = accessControlService;
    }

    @PostMapping("/commands/refill")
    public DeviceCommandResponse startRefill(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long patientId,
            @Valid @RequestBody(required = false) RefillRequest request
    ) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return commandService.startRefill(patientId, request != null ? request.minutes() : null);
    }

    @PostMapping("/commands/refill/stop")
    public DeviceCommandResponse stopRefill(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long patientId) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return commandService.stopRefill(patientId);
    }

    @PostMapping("/commands/locate")
    public DeviceCommandResponse locate(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long patientId) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return commandService.locate(patientId);
    }

    @PostMapping("/commands/silence-alarm")
    public DeviceCommandResponse silenceAlarm(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long patientId) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return commandService.send(patientId, DeviceCommandService.SILENCE_ALARM);
    }

    @PostMapping("/commands/request-status")
    public DeviceCommandResponse requestStatus(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long patientId) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return commandService.send(patientId, DeviceCommandService.REQUEST_STATUS);
    }

    @PostMapping("/commands/resync-agenda")
    public DeviceCommandResponse resyncAgenda(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long patientId) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return commandService.send(patientId, DeviceCommandService.RESYNC_AGENDA);
    }

    @PostMapping("/commands/reset-wifi")
    @PreAuthorize("hasRole('CUIDADOR')")
    public DeviceCommandResponse resetWifi(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long patientId) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return commandService.send(patientId, DeviceCommandService.RESET_WIFI);
    }

    @PostMapping("/commands/restart")
    @PreAuthorize("hasRole('CUIDADOR')")
    public DeviceCommandResponse restart(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long patientId) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return commandService.send(patientId, DeviceCommandService.RESTART);
    }

    @GetMapping("/commands/{commandId}")
    public DeviceCommandResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long patientId,
            @PathVariable String commandId
    ) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return commandService.get(patientId, commandId);
    }

    /** "Fui eu" (abertura fora de horário) ou "Ciente" (demais alertas da gaveta). */
    @PostMapping("/alerts/{eventId}/acknowledge")
    public DeviceCommandResponse acknowledgeAlert(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long patientId,
            @PathVariable String eventId
    ) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return commandService.acknowledgeAlert(patientId, eventId);
    }
}
