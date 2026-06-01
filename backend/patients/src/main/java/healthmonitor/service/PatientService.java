package healthmonitor.service;

import healthmonitor.model.dto.PatientDto;
import healthmonitor.model.dto.PatientUpdateDto;
import jakarta.transaction.Transactional;

import java.util.List;

public interface PatientService {
    @Transactional
    PatientDto createPatient(PatientDto request);

    PatientDto getPatientById(String id);

    List<PatientDto> getAllPatients();

    @Transactional
    PatientDto updatePatient(String id, PatientUpdateDto updateRequest);
}
