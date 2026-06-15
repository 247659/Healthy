package healthmonitor.service;

import healthmonitor.model.dto.PatientDto;
import healthmonitor.model.dto.PatientRegistrationDto;
import jakarta.transaction.Transactional;

import java.util.List;

public interface PatientService {
    @Transactional
    PatientDto registerPatient(PatientRegistrationDto dto);

    PatientDto getPatientById(String id);

    List<PatientDto> getAllPatients();

    @Transactional
    PatientDto updatePatient(String id, PatientDto updateRequest);
}
