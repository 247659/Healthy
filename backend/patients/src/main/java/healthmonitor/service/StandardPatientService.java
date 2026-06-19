package healthmonitor.service;

import healthmonitor.exception.exceptions.DuplicateResourceException;
import healthmonitor.exception.exceptions.PatientNotFoundException;
import healthmonitor.mapper.PatientMapper;
import healthmonitor.model.Patient;
import healthmonitor.model.dto.PatientDto;
import healthmonitor.model.event.PatientThresholdEvent;
import healthmonitor.repository.PatientRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StandardPatientService implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final ApplicationEventPublisher applicationEventPublisher;


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

    @Override
    @Transactional
    public PatientDto save(PatientDto request) {
        Patient patient = patientMapper.toEntity(request);
        Patient savedPatient = patientRepository.save(patient);
        applicationEventPublisher.publishEvent(new PatientThresholdEvent(savedPatient.getId()));
        return patientMapper.toDto(savedPatient);
    }

    @Transactional
    @Override
    public PatientDto updatePatient(String id, PatientDto updateRequest) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient with this ID doesn't exists: " + id));

        // 1. Walidacja PESEL: Sprawdzamy tylko wtedy, gdy podano nowy PESEL i jest on INNY niż obecny pesel pacjenta
        if (updateRequest.getPesel() != null && !Objects.equals(patient.getPesel(), updateRequest.getPesel())) {
            if (patientRepository.existsByPesel(updateRequest.getPesel())) {
                throw new DuplicateResourceException("Patient with this PESEL number already exists");
            }
            patient.setPesel(updateRequest.getPesel());
        }

        // 2. Aktualizacja reszty pól (Objects.equals jest bezpieczne, nawet jeśli z bazy wraca null)
        if (updateRequest.getFirstName() != null && !Objects.equals(patient.getFirstName(), updateRequest.getFirstName())) {
            patient.setFirstName(updateRequest.getFirstName());
        }

        if (updateRequest.getLastName() != null && !Objects.equals(patient.getLastName(), updateRequest.getLastName())) {
            patient.setLastName(updateRequest.getLastName());
        }

        if (updateRequest.getPhoneNumber() != null && !Objects.equals(patient.getPhoneNumber(), updateRequest.getPhoneNumber())) {
            patient.setPhoneNumber(updateRequest.getPhoneNumber());
        }

        if (updateRequest.getAddress() != null && !Objects.equals(patient.getAddress(), updateRequest.getAddress())) {
            patient.setAddress(updateRequest.getAddress());
        }

        if (updateRequest.getDateOfBirth() != null && !Objects.equals(patient.getDateOfBirth(), updateRequest.getDateOfBirth())) {
            patient.setDateOfBirth(updateRequest.getDateOfBirth());
        }

        Patient updatedPatient = patientRepository.save(patient);
        return patientMapper.toDto(updatedPatient);
    }

}
