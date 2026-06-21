package healthmonitor.service;

import healthmonitor.dto.VitalSignsDto;
import org.springframework.web.multipart.MultipartFile;

public interface IntegrationService {

    void receiveVitals(VitalSignsDto dto);

    void processBatchMeasurements(MultipartFile file);
}
