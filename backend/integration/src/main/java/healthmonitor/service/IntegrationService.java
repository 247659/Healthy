package healthmonitor.service;

import org.springframework.web.multipart.MultipartFile;

public interface IntegrationService {
    void processBatchMeasurements(MultipartFile file);
}
