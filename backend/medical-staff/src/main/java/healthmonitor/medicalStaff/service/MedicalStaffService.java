package healthmonitor.medicalStaff.service;

import healthmonitor.medicalStaff.payload.request.MedicalStaffRequest;
import healthmonitor.medicalStaff.payload.response.MedicalStaffResponse;

import java.util.List;
import java.util.UUID;

public interface MedicalStaffService {
    List<MedicalStaffResponse> getAll();

    MedicalStaffResponse getById(UUID id);

    MedicalStaffResponse save(MedicalStaffRequest request);

    void delete(UUID id);

    MedicalStaffResponse update(UUID id, MedicalStaffRequest request);
}
