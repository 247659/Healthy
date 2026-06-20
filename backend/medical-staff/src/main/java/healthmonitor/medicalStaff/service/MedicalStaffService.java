package healthmonitor.medicalStaff.service;

import healthmonitor.medicalStaff.payload.request.MedicalStaffCreateRequest;
import healthmonitor.medicalStaff.payload.request.MedicalStaffRequest;
import healthmonitor.medicalStaff.payload.response.MedicalStaffEssentialResponse;
import healthmonitor.medicalStaff.payload.response.MedicalStaffResponse;

import java.util.List;

public interface MedicalStaffService {
    List<MedicalStaffResponse> getAll();

    MedicalStaffResponse getById(String id);

    MedicalStaffResponse save(MedicalStaffCreateRequest request);

    void delete(String id);

    MedicalStaffResponse update(String id, MedicalStaffRequest request);

    void assignPatient(String id, String patientId);

    List<String> getPatientsIds(String id);

    List<String> getDoctorsIdsByPatientId(String patientId);

    List<MedicalStaffEssentialResponse> getAllDoctorsEssentialData();

    MedicalStaffEssentialResponse getDoctorEssentialDataById(String id);

    void unassignPatient(String id, String patientId);

    List<MedicalStaffEssentialResponse> getDoctorsAssignedToPatient(String patientId);
}
