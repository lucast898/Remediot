package br.edu.remediot.controller;

import br.edu.remediot.dto.CreateMedicationRequest;
import br.edu.remediot.dto.MedicationResponse;
import br.edu.remediot.dto.UpdateMedicationRequest;
import br.edu.remediot.security.UserPrincipal;
import br.edu.remediot.service.AccessControlService;
import br.edu.remediot.service.MedicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients/{patientId}/medications")
public class MedicationController {

    private final MedicationService medicationService;
    private final AccessControlService accessControlService;

    public MedicationController(MedicationService medicationService, AccessControlService accessControlService) {
        this.medicationService = medicationService;
        this.accessControlService = accessControlService;
    }

    @GetMapping
    public List<MedicationResponse> list(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long patientId) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return medicationService.listByPatient(patientId);
    }

    @PostMapping
    public ResponseEntity<MedicationResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long patientId,
            @Valid @RequestBody CreateMedicationRequest request
    ) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(medicationService.create(patientId, request));
    }

    @PutMapping("/{medicationId}")
    public MedicationResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long patientId,
            @PathVariable Long medicationId,
            @Valid @RequestBody UpdateMedicationRequest request
    ) {
        accessControlService.getAccessiblePatient(patientId, principal);
        return medicationService.update(medicationId, patientId, request);
    }

    @DeleteMapping("/{medicationId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long patientId,
            @PathVariable Long medicationId
    ) {
        accessControlService.getAccessiblePatient(patientId, principal);
        medicationService.delete(medicationId, patientId);
        return ResponseEntity.noContent().build();
    }
}
