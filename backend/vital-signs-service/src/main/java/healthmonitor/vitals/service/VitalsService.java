package healthmonitor.vitals.service;

import healthmonitor.vitals.dto.VitalSignsDto;

import java.util.List;
import java.time.Instant;

public interface VitalsService {

    List<VitalSignsDto> getPatientHistory(String patientId, Instant from, Instant to);

    void processAndSaveVitals(VitalSignsDto dto);
}
