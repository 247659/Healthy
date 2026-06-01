package healthmonitor.service;

import healthmonitor.exception.exceptions.DuplicateResourceException;
import healthmonitor.exception.exceptions.PatientNotFoundException;
import healthmonitor.mapper.PatientMapper;
import healthmonitor.model.Patient;
import healthmonitor.model.dto.PatientDto;
import healthmonitor.model.dto.PatientUpdateDto;
import healthmonitor.repository.PatientRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StandardPatientService implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Transactional
    @Override
    public PatientDto createPatient(PatientDto request) {
        if (patientRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Patient with this e-mail already exists");
        }

        if (request.getPesel() != null && patientRepository.existsByPesel(request.getPesel())) {
            throw new DuplicateResourceException("Patient with this PESEL number already exists");
        }

        Patient patient = new Patient();
        patient.setEmail(request.getEmail());
        patient.setName(request.getName());
        patient.setSurname(request.getSurname());
        patient.setPesel(request.getPesel());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setPhoneNumber(request.getPhoneNumber());
        patient.setAddress(request.getAddress());

        Patient savedPatient = patientRepository.save(patient);
        return patientMapper.toDto(savedPatient);
    }

    @Override
    public PatientDto getPatientById(String id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient with this ID doesn't exists: " + id));
        return patientMapper.toDto(patient);
    }

    @Override
    public List<PatientDto> getAllPatients() {
        return patientRepository.findAll().stream()
                .map(patientMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public PatientDto updatePatient(String id, PatientUpdateDto updateRequest) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient with this ID doesn't exists: " + id));

        if (updateRequest.getName() != null) patient.setName(updateRequest.getName());
        if (updateRequest.getSurname() != null) patient.setSurname(updateRequest.getSurname());
        if (updateRequest.getPhoneNumber() != null) patient.setPhoneNumber(updateRequest.getPhoneNumber());
        if (updateRequest.getAddress() != null) patient.setAddress(updateRequest.getAddress());
        if (updateRequest.getDateOfBirth() != null) patient.setDateOfBirth(updateRequest.getDateOfBirth());

        Patient updatedPatient = patientRepository.save(patient);
        return patientMapper.toDto(updatedPatient);
    }

}
