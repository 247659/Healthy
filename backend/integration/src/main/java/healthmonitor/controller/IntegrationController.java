package healthmonitor.controller;

import healthmonitor.dto.VitalSignsDto;
import healthmonitor.service.IntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/integration")
@RequiredArgsConstructor
@Slf4j
public class IntegrationController {

    private final IntegrationService integrationService;

    @PostMapping
    public ResponseEntity<Void> receiveVitals(@Valid @RequestBody VitalSignsDto dto) {
        integrationService.receiveVitals(dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> receiveVitalsBatch(@RequestParam("file") MultipartFile file) {
        integrationService.processBatchMeasurements(file);
        return ResponseEntity.noContent().build();
    }
}
