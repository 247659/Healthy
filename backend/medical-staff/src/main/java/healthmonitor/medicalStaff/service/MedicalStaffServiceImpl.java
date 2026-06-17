package healthmonitor.medicalStaff.service;

import healthmonitor.medicalStaff.mapper.MedicalStaffMapper;
import healthmonitor.medicalStaff.model.MedicalStaff;
import healthmonitor.medicalStaff.payload.request.MedicalStaffRequest;
import healthmonitor.medicalStaff.payload.response.MedicalStaffResponse;
import healthmonitor.medicalStaff.repository.MedicalStaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MedicalStaffServiceImpl implements MedicalStaffService {
    private final MedicalStaffRepository medicalStaffRepository;
    private final MedicalStaffMapper medicalStaffMapper;

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

    private MedicalStaff getEntity(UUID id) {
        return medicalStaffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical staff not found"));

    }
}
