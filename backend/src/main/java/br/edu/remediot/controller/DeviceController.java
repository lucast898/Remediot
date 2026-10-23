package br.edu.remediot.controller;

import br.edu.remediot.dto.device.DeviceEventResponse;
import br.edu.remediot.dto.device.DeviceResponse;
import br.edu.remediot.dto.device.PairDeviceRequest;
import br.edu.remediot.security.UserPrincipal;
import br.edu.remediot.service.AccessControlService;
import br.edu.remediot.service.device.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gaveta do paciente: estado, pareamento (D7) e histórico de eventos. */
@RestController
@RequestMapping("/api/patients/{patientId}/device")
public class DeviceController {

    private final DeviceService deviceService;
    private final AccessControlService accessControlService;

    public DeviceController(DeviceService deviceService, AccessControlService accessControlService) {
        this.deviceService = deviceService;
        this.accessControlService = accessControlService;
    }

    @GetMapping
    public DeviceResponse get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long patientId) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return deviceService.getForPatient(patientId);
    }

    @PostMapping("/pair")
    @PreAuthorize("hasRole('CUIDADOR')")
    public DeviceResponse pair(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long patientId,
            @Valid @RequestBody PairDeviceRequest request
    ) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return deviceService.pair(patientId, request.code());
    }

    @DeleteMapping
    @PreAuthorize("hasRole('CUIDADOR')")
    public ResponseEntity<Void> unpair(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long patientId) {
        accessControlService.getAccessiblePatient(patientId, principal);
        deviceService.unpair(patientId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/events")
    public List<DeviceEventResponse> events(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return deviceService.listEvents(patientId, limit);
    }

    @GetMapping("/alerts")
    public List<DeviceEventResponse> pendingAlerts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long patientId
    ) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return deviceService.listPendingAlerts(patientId);
    }
}
