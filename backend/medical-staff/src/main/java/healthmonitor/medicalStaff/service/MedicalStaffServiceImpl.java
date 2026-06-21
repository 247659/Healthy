package healthmonitor.medicalStaff.service;

import healthmonitor.client.AuthClient;
import healthmonitor.client.PatientClient;
import healthmonitor.medicalStaff.mapper.MedicalStaffMapper;
import healthmonitor.medicalStaff.model.MedicalStaff;
import healthmonitor.medicalStaff.model.PatientAssignment;
import healthmonitor.medicalStaff.payload.request.MedicalStaffCreateRequest;
import healthmonitor.medicalStaff.payload.request.MedicalStaffRequest;
import healthmonitor.medicalStaff.payload.response.MedicalStaffEssentialResponse;
import healthmonitor.medicalStaff.payload.response.MedicalStaffResponse;
import healthmonitor.medicalStaff.repository.MedicalStaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)

public class MedicalStaffServiceImpl implements MedicalStaffService {
    private final MedicalStaffRepository medicalStaffRepository;
    private final MedicalStaffMapper medicalStaffMapper;
    private final PatientClient patientClient;
    private final AuthClient authClient;

    @Override
    public List<MedicalStaffResponse> getAll() {
        return medicalStaffRepository.findAll().stream()
                .map(medicalStaffMapper::toResponse)
                .toList();
    }

    @Override
    public MedicalStaffResponse getById(String id) {
        MedicalStaff medicalStaff = getEntity(id);
        return medicalStaffMapper.toResponse(medicalStaff);
    }

    @Override
    @Transactional
    public MedicalStaffResponse save(MedicalStaffCreateRequest request) {
        authClient.updatePassword(request.id(), request.password());
        MedicalStaff medicalStaff = medicalStaffMapper.toEntity(request);
        MedicalStaff medicalStaffSaved = medicalStaffRepository.save(medicalStaff);

        return medicalStaffMapper.toResponse(medicalStaffSaved);
    }

    @Override
    @Transactional
    public void delete(String id) {
        MedicalStaff medicalStaff = getEntity(id);
        medicalStaffRepository.delete(medicalStaff);
    }

    @Override
    @Transactional
    public MedicalStaffResponse update(String id, MedicalStaffRequest request) {
        if (request.password() != null && !request.password().trim().isEmpty()) {
            authClient.updatePassword(id, request.password());
        }

        MedicalStaff medicalStaff = getEntity(id);
        medicalStaffMapper.updateEntity(medicalStaff, request);

        return medicalStaffMapper.toResponse(medicalStaff);
    }

    @Override
    @Transactional
    public void assignPatient(String id, String patientId) {
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
    public List<String> getPatientsIds(String id) {
        MedicalStaff medicalStaff = medicalStaffRepository.findWithPatientAssignmentsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical staff not found"));

        return medicalStaff.getPatientAssignments().stream()
                .map(PatientAssignment::getPatientId)
                .toList();
    }

    @Override
    public List<String> getDoctorsIdsByPatientId(String patientId) {
        return medicalStaffRepository.findIdsByPatientId(patientId);
    }

    @Override
    public List<MedicalStaffEssentialResponse> getAllDoctorsEssentialData() {
        return medicalStaffRepository.findAll().stream()
                .map(medicalStaffMapper::toEssentialResponse)
                .toList();
    }

    @Override
    public MedicalStaffEssentialResponse getDoctorEssentialDataById(String id) {
        MedicalStaff medicalStaff = getEntity(id);
        return medicalStaffMapper.toEssentialResponse(medicalStaff);
    }

    @Override
    @Transactional
    public void unassignPatient(String id, String patientId) {
        MedicalStaff medicalStaff = getEntity(id);
        medicalStaff.getPatientAssignments().stream()
                .filter(pa -> pa.getPatientId().equals(patientId))
                .findFirst()
                .ifPresent(medicalStaff::removePatientAssignment);
    }

    @Override
    public List<MedicalStaffEssentialResponse> getDoctorsAssignedToPatient(String patientId) {
        return medicalStaffRepository.findByPatientAssignments_PatientId(patientId).stream()
                .map(medicalStaffMapper::toEssentialResponse)
                .toList();
    }

    private MedicalStaff getEntity(String id) {
        return medicalStaffRepository.findWithSpecializationById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical staff not found"));
    }
}
