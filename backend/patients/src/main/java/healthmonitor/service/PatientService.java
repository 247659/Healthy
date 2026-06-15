package healthmonitor.service;

import healthmonitor.model.dto.PatientDto;
import jakarta.transaction.Transactional;

import java.util.List;

public interface PatientService {
    PatientDto getPatientById(String id);

    List<PatientDto> getAllPatients();

    @Transactional
    PatientDto updatePatient(String id, PatientDto updateRequest);
}
