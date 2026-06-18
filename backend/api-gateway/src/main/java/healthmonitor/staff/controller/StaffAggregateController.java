package healthmonitor.staff.controller;

import healthmonitor.staff.service.StaffAggregateService;
import healthmonitor.payload.PatientClientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gateway")
@RequiredArgsConstructor
public class StaffAggregateController {

    private final StaffAggregateService staffAggregateService;

    @GetMapping("/dashboard/staff/{staffId}/patients")
    public Flux<PatientClientResponse> getAssignedPatients(@PathVariable UUID staffId) {
        return staffAggregateService.getAssignedPatient(staffId);
    }
}
