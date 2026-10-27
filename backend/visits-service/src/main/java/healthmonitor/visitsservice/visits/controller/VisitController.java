package healthmonitor.visitsservice.visits.controller;

import healthmonitor.visitsservice.visits.payload.request.VisitRequest;
import healthmonitor.visitsservice.visits.payload.response.VisitResponse;
import healthmonitor.visitsservice.visits.service.VisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/visits")
@CrossOrigin(origins = "*")
public class VisitController {
    private final VisitService visitService;

    @GetMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('PATIENT')")
    public ResponseEntity<List<VisitResponse>> getAll() {
        return ResponseEntity.ok(visitService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('PATIENT')")
    public ResponseEntity<VisitResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(visitService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('PATIENT')")
    public ResponseEntity<VisitResponse> save(@Valid @RequestBody VisitRequest request) {
        return ResponseEntity.ok(visitService.save(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('PATIENT')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        visitService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('PATIENT')")
    public ResponseEntity<VisitResponse> update(@PathVariable UUID id, @Valid @RequestBody VisitRequest request) {
        return ResponseEntity.ok(visitService.update(id, request));
    }
}
