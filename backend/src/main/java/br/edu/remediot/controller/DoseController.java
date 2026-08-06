package br.edu.remediot.controller;

import br.edu.remediot.dto.DoseResponse;
import br.edu.remediot.security.UserPrincipal;
import br.edu.remediot.service.AccessControlService;
import br.edu.remediot.service.DoseGenerationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/patients/{patientId}/doses")
public class DoseController {

    private final DoseGenerationService doseGenerationService;
    private final AccessControlService accessControlService;

    public DoseController(DoseGenerationService doseGenerationService, AccessControlService accessControlService) {
        this.doseGenerationService = doseGenerationService;
        this.accessControlService = accessControlService;
    }

    @GetMapping
    public List<DoseResponse> listForDate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        accessControlService.getAccessiblePatient(patientId, principal);
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return doseGenerationService.listDosesForDate(patientId, targetDate);
    }
}
