package healthmonitor.patientAssignment.mapper;

import healthmonitor.medicalStaff.mapper.MedicalStaffMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = MedicalStaffMapper.class)
public interface PatientAssignmentMapper {
}
