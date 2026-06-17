package healthmonitor.medicalStaff.service;

import healthmonitor.client.PatientClient;
import healthmonitor.client.PatientResponse;
import healthmonitor.medicalStaff.mapper.MedicalStaffMapper;
import healthmonitor.medicalStaff.model.MedicalStaff;
import healthmonitor.medicalStaff.payload.request.MedicalStaffRequest;
import healthmonitor.medicalStaff.payload.response.MedicalStaffResponse;
import healthmonitor.medicalStaff.repository.MedicalStaffRepository;
import healthmonitor.medicalStaff.model.PatientAssignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MedicalStaffServiceImpl implements MedicalStaffService {
    private final MedicalStaffRepository medicalStaffRepository;
    private final MedicalStaffMapper medicalStaffMapper;
    private final PatientClient patientClient;

    @Override
    public List<MedicalStaffResponse> getAll() {
        return medicalStaffRepository.findAll().stream()
                .map(medicalStaffMapper::toResponse)
                .toList();
    }

    @Override
    public MedicalStaffResponse getById(UUID id) {
        MedicalStaff medicalStaff = getEntity(id);
        return medicalStaffMapper.toResponse(medicalStaff);
    }

    @Override
    @Transactional
    public MedicalStaffResponse save(MedicalStaffRequest request) {
        MedicalStaff medicalStaff = medicalStaffMapper.toEntity(request);
        MedicalStaff medicalStaffSaved = medicalStaffRepository.save(medicalStaff);
        return medicalStaffMapper.toResponse(medicalStaffSaved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        MedicalStaff medicalStaff = getEntity(id);
        medicalStaffRepository.delete(medicalStaff);
    }

    @Override
    @Transactional
    public MedicalStaffResponse update(UUID id, MedicalStaffRequest request) {
        MedicalStaff medicalStaff = getEntity(id);
        medicalStaffMapper.updateEntity(medicalStaff, request);
        return medicalStaffMapper.toResponse(medicalStaff);
    }

    @Override
    @Transactional
    public void assignPatient(UUID id, String patientId) {
        patientClient.getPatient(patientId);
        MedicalStaff medicalStaff = getEntity(id);
        boolean alreadyAssigned = medicalStaff.getPatientAssignments().stream()
                .anyMatch(a -> a.getPatientId().equals(patientId));

        if (alreadyAssigned) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient is already assigned");
        }

        PatientAssignment patientAssignment = new PatientAssignment();
        patientAssignment.setMedicalStaff(medicalStaff);
        patientAssignment.setPatientId(patientId);
        medicalStaff.addPatientAssignment(patientAssignment);
        medicalStaffRepository.save(medicalStaff);
    }

    @Override
    public List<PatientResponse> getPatientsInfo(UUID id) {
        MedicalStaff medicalStaff = medicalStaffRepository.findWithPatientAssignmentsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical staff not found"));
        Set<String> patientIds = medicalStaff.getPatientAssignments().stream()
                .map(PatientAssignment::getPatientId)
                .collect(Collectors.toSet());

        return patientClient.getPatients(patientIds);
    }

    private MedicalStaff getEntity(UUID id) {
        return medicalStaffRepository.findWithSpecializationById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical staff not found"));
    }
}
