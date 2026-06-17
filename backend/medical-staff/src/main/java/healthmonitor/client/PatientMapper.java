package healthmonitor.client;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PatientMapper {
    PatientResponse toResponse(PatientClientResponse clientResponse);
}
