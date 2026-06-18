package healthmonitor.medicalStaff.messaging.mapper;

import healthmonitor.medicalStaff.messaging.event.MedicalStaffRegisterEvent;
import healthmonitor.medicalStaff.payload.request.MedicalStaffRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MedicalStaffEventMapper {
    MedicalStaffRequest toRequest(MedicalStaffRegisterEvent event);
}
