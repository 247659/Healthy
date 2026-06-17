package healthmonitor.medicalStaff.mapper;

import healthmonitor.medicalStaff.model.MedicalStaff;
import healthmonitor.medicalStaff.payload.request.MedicalStaffRequest;
import healthmonitor.medicalStaff.payload.response.MedicalStaffResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MedicalStaffMapper {
    MedicalStaffResponse toResponse(MedicalStaff medicalStaff);
    MedicalStaff toEntity(MedicalStaffRequest request);
    void updateEntity(@MappingTarget MedicalStaff medicalStaff, MedicalStaffRequest request);
}
