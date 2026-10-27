package healthmonitor.vitals.controller;

import healthmonitor.vitals.dto.VitalSignsDto;
import healthmonitor.vitals.dto.VitalThresholdDto;
import healthmonitor.vitals.service.VitalsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    @PreAuthorize("hasRole('DOCTOR') or hasRole('PATIENT') and #jwt.subject == #patientId")
    public ResponseEntity<List<VitalSignsDto>> getHistory(@PathVariable String patientId,
                                                          @RequestParam(required = false) Instant from,
                                                          @RequestParam(required = false) Instant to,
                                                          @AuthenticationPrincipal Jwt jwt) {
        if (to == null) to = Instant.now();
        if (from == null) from = to.minus(Duration.ofDays(1));
        List<VitalSignsDto> history = vitalsService.getPatientHistory(patientId, from, to);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/threshold/{patientId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('PATIENT') and #jwt.subject == #patientId")
    public ResponseEntity<VitalThresholdDto> getByPatientId(@PathVariable String patientId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(vitalsService.getByPatientId(patientId));
    }

    @PutMapping("/threshold/{patientId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('PATIENT') and #jwt.subject == #patientId")
    public ResponseEntity<VitalThresholdDto> update(@PathVariable String patientId, @Valid @RequestBody VitalThresholdDto request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(vitalsService.update(patientId, request));
    }
}
