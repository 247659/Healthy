package healthmonitor.service;

import healthmonitor.model.dto.PatientDto;
import jakarta.transaction.Transactional;

import java.util.List;

public interface PatientService {
    PatientDto getPatientById(String id);

    List<PatientDto> getAllPatients();

    PatientDto save(PatientDto request);

    @Transactional
    PatientDto updatePatient(String id, PatientDto updateRequest);
}
