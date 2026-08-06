package br.edu.remediot.controller;

import br.edu.remediot.dto.CreatePatientRequest;
import br.edu.remediot.dto.PatientResponse;
import br.edu.remediot.dto.UpdatePatientRequest;
import br.edu.remediot.security.UserPrincipal;
import br.edu.remediot.service.AccessControlService;
import br.edu.remediot.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final AccessControlService accessControlService;

    public PatientController(PatientService patientService, AccessControlService accessControlService) {
        this.patientService = patientService;
        this.accessControlService = accessControlService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CUIDADOR')")
    public List<PatientResponse> listMyPatients(@AuthenticationPrincipal UserPrincipal principal) {
        return patientService.listByCaregiver(principal.getId());
    }

    @PostMapping
    @PreAuthorize("hasRole('CUIDADOR')")
    public ResponseEntity<PatientResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePatientRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.create(principal.getId(), request));
    }

    @GetMapping("/{id}")
    public PatientResponse get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        accessControlService.getAccessiblePatient(id, principal);
        return patientService.getResponse(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUIDADOR')")
    public PatientResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePatientRequest request
    ) {
        accessControlService.getAccessiblePatient(id, principal);
        return patientService.update(id, request);
    }
}
