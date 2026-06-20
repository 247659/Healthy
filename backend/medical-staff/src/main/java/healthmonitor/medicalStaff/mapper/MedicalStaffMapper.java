package healthmonitor.medicalStaff.mapper;

import healthmonitor.medicalStaff.model.MedicalStaff;
import healthmonitor.medicalStaff.model.Specialization;
import healthmonitor.medicalStaff.payload.request.MedicalStaffCreateRequest;
import healthmonitor.medicalStaff.payload.request.MedicalStaffRequest;
import healthmonitor.medicalStaff.payload.response.MedicalStaffEssentialResponse;
import healthmonitor.medicalStaff.payload.response.MedicalStaffResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MedicalStaffMapper {
    MedicalStaffResponse toResponse(MedicalStaff medicalStaff);
    MedicalStaff toEntity(MedicalStaffRequest request);
    MedicalStaff toEntity(MedicalStaffCreateRequest request);

    @Mapping(source = "specializations", target = "specializationNames")
    MedicalStaffEssentialResponse toEssentialResponse(MedicalStaff medicalStaff);
    void updateEntity(@MappingTarget MedicalStaff medicalStaff, MedicalStaffRequest request);

    default String mapSpecializationToString(Specialization specialization) {
        if (specialization == null) {
            return null;
        }

        return specialization.getName();
    }
}
