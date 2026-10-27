package healthmonitor.staff.controller;

import healthmonitor.payload.PatientClientResponse;
import healthmonitor.staff.service.StaffAggregateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/gateway")
@RequiredArgsConstructor
public class StaffAggregateController {

    private final StaffAggregateService staffAggregateService;

    @PreAuthorize("hasRole('DOCTOR') and #jwt.subject == #staffId")
    @GetMapping("/dashboard/staff/{staffId}/patients/assigned")
    public Flux<PatientClientResponse> getAssignedPatients(@PathVariable String staffId, @AuthenticationPrincipal Jwt jwt) {
        return staffAggregateService.getAssignedPatients(staffId);
    }

    @PreAuthorize("hasRole('DOCTOR') and #jwt.subject == #staffId")
    @GetMapping("/dashboard/staff/{staffId}/patients/unassigned")
    public Flux<PatientClientResponse> getUnassignedPatients(@PathVariable String staffId, @AuthenticationPrincipal Jwt jwt) {
        return staffAggregateService.getUnassignedPatients(staffId);
    }
}
