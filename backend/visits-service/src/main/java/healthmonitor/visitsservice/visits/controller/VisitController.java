package healthmonitor.visitsservice.visits.controller;

import healthmonitor.visitsservice.visits.payload.request.VisitRequest;
import healthmonitor.visitsservice.visits.payload.response.VisitResponse;
import healthmonitor.visitsservice.visits.service.VisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/visits")
@CrossOrigin(origins = "*")
public class VisitController {
    private final VisitService visitService;

    @GetMapping
    public ResponseEntity<List<VisitResponse>> getAll() {
        return ResponseEntity.ok(visitService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VisitResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(visitService.getById(id));
    }

    @PostMapping
    public ResponseEntity<VisitResponse> save(@Valid @RequestBody VisitRequest request) {
        return ResponseEntity.ok(visitService.save(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        visitService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<VisitResponse> update(@PathVariable UUID id, @Valid @RequestBody VisitRequest request) {
        return ResponseEntity.ok(visitService.update(id, request));
    }
}
