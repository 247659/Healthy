package healthmonitor.controller;

import healthmonitor.model.dto.PatientDto;
import healthmonitor.service.PatientService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patients")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('INTERNAL_SERVICE') or hasRole('PATIENT') " +
            "and @securityService.isOwner(#jwt, #id)")
    public ResponseEntity<PatientDto> getPatientById(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientDto> save(@Valid @RequestBody PatientDto request) {
        return ResponseEntity.ok(patientService.save(request));
    }

    @GetMapping("/allPatients")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<PatientDto>> getAllPatients() {
        return ResponseEntity.ok(patientService.getAllPatients());
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('PATIENT') and @securityService.isOwner(#jwt, #id)")
    public ResponseEntity<PatientDto> updatePatient(@PathVariable String id, @Valid @RequestBody PatientDto patientDto, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(patientService.updatePatient(id, patientDto));
    }

}
