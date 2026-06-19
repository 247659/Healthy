package healthmonitor.staff.controller;

import healthmonitor.payload.PatientClientResponse;
import healthmonitor.staff.service.StaffAggregateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/gateway")
@RequiredArgsConstructor
public class StaffAggregateController {

    private final StaffAggregateService staffAggregateService;

    @GetMapping("/dashboard/staff/{staffId}/patients/assigned")
    public Flux<PatientClientResponse> getAssignedPatients(@PathVariable String staffId) {
        return staffAggregateService.getAssignedPatients(staffId);
    }

    @GetMapping("/dashboard/staff/{staffId}/patients/unassigned")
    public Flux<PatientClientResponse> getUnassignedPatients(@PathVariable String staffId) {
        return staffAggregateService.getUnassignedPatients(staffId);
    }
}
