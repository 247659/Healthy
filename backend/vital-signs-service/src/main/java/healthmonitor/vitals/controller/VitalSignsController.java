package healthmonitor.vitals.controller;

import healthmonitor.vitals.dto.VitalSignsDto;
import healthmonitor.vitals.service.VitalsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/vital-signs")
@RequiredArgsConstructor
public class VitalSignsController {

    private final VitalsService vitalsService;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<VitalSignsDto>> getHistory(@PathVariable String patientId,
                                                          @RequestParam(required = false) Instant from,
                                                          @RequestParam(required = false) Instant to) {
        if (to == null) to = Instant.now();
        if (from == null) from = to.minus(Duration.ofDays(1));
        List<VitalSignsDto> history = vitalsService.getPatientHistory(patientId, from, to);
        return ResponseEntity.ok(history);
    }

    @PostMapping
    public ResponseEntity<Void> receiveVitals(@Valid @RequestBody VitalSignsDto request) {
        vitalsService.processAndSaveVitals(request);
        return ResponseEntity.noContent().build();
    }
}
