package healthmonitor.medicalStaff.controller;

import healthmonitor.medicalStaff.payload.request.MedicalStaffCreateRequest;
import healthmonitor.medicalStaff.payload.request.MedicalStaffRequest;
import healthmonitor.medicalStaff.payload.response.MedicalStaffEssentialResponse;
import healthmonitor.medicalStaff.payload.response.MedicalStaffResponse;
import healthmonitor.medicalStaff.service.MedicalStaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/staff")
public class MedicalStaffController {
    private final MedicalStaffService medicalStaffService;

    @GetMapping
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<List<MedicalStaffResponse>> getAll() {
        return ResponseEntity.ok(medicalStaffService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<MedicalStaffResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(medicalStaffService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<MedicalStaffResponse> save(@Valid @RequestBody MedicalStaffCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicalStaffService.save(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR') and @securityService.isOwner(#jwt, #id)")
    public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        medicalStaffService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR') and @securityService.isOwner(#jwt, #id)")
    public ResponseEntity<MedicalStaffResponse> update(@PathVariable String id, @Valid @RequestBody MedicalStaffRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(medicalStaffService.update(id, request));
    }

    @PostMapping("/{id}/assign/{patientId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> assignPatient(@PathVariable String id, @PathVariable String patientId) {
        medicalStaffService.assignPatient(id, patientId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/patients")
    public ResponseEntity<List<String>> getAllPatientIds(@PathVariable String id) {
        return ResponseEntity.ok(medicalStaffService.getPatientsIds(id));
    }

    @GetMapping("/patients/{patientId}/doctors-list")
    public ResponseEntity<List<String>> getDoctorsForPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(medicalStaffService.getDoctorsIdsByPatientId(patientId));
    }

    @GetMapping("/essential")
    public ResponseEntity<List<MedicalStaffEssentialResponse>> getAllDoctorsEssentialData() {
        return ResponseEntity.ok(medicalStaffService.getAllDoctorsEssentialData());
    }

    @GetMapping("/{id}/essential")
    public ResponseEntity<MedicalStaffEssentialResponse> getDoctorEssentialDataById(@PathVariable String id) {
        return ResponseEntity.ok(medicalStaffService.getDoctorEssentialDataById(id));
    }

    @GetMapping("/patients/{patientId}")
    public ResponseEntity<List<MedicalStaffEssentialResponse>> getDoctorsAssignedToPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(medicalStaffService.getDoctorsAssignedToPatient(patientId));
    }
    @DeleteMapping("/{id}/unassign/{patientId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> unassignPatient(@PathVariable String id, @PathVariable String patientId) {
        medicalStaffService.unassignPatient(id, patientId);
        return ResponseEntity.noContent().build();
    }
}
