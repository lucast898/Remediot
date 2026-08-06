package br.edu.remediot.controller;

import br.edu.remediot.dto.PatientAdherenceResponse;
import br.edu.remediot.security.UserPrincipal;
import br.edu.remediot.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CUIDADOR')")
    public List<PatientAdherenceResponse> getAdherence(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.getAdherenceForCaregiver(principal.getId());
    }
}
