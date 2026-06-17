package healthmonitor.medicalStaff.controller;

import healthmonitor.medicalStaff.payload.request.MedicalStaffRequest;
import healthmonitor.medicalStaff.payload.response.MedicalStaffResponse;
import healthmonitor.medicalStaff.service.MedicalStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/staff")
public class MedicalStaffController {
    private final MedicalStaffService medicalStaffService;

    @GetMapping
    public ResponseEntity<List<MedicalStaffResponse>> getAll() {
        return ResponseEntity.ok(medicalStaffService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalStaffResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(medicalStaffService.getById(id));
    }

    @PostMapping
    public ResponseEntity<MedicalStaffResponse> save(@RequestBody MedicalStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicalStaffService.save(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        medicalStaffService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalStaffResponse> update(@PathVariable UUID id, @RequestBody MedicalStaffRequest request) {
        return ResponseEntity.ok(medicalStaffService.update(id, request));
    }
}
