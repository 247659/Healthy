package healthmonitor.mapper;

import healthmonitor.model.Patient;
import healthmonitor.model.dto.PatientDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    PatientDto toDto(Patient patient);
    Patient toEntity(PatientDto dto);
}
