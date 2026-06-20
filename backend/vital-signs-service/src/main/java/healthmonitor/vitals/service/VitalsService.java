package healthmonitor.vitals.service;

import healthmonitor.vitals.dto.VitalSignsDto;
import healthmonitor.vitals.dto.VitalThresholdDto;
import healthmonitor.vitals.event.VitalThresholdEvent;

import java.time.Instant;
import java.util.List;

public interface VitalsService {

    List<VitalSignsDto> getPatientHistory(String patientId, Instant from, Instant to);

    void processAndSaveVitals(VitalSignsDto dto);

    void saveThreshold(VitalThresholdEvent event);

    VitalThresholdDto update(String patientId, VitalThresholdDto request);

    VitalThresholdDto getByPatientId(String patientId);

    void processAndSaveVitalsBatch(List<VitalSignsDto> batch);
}
