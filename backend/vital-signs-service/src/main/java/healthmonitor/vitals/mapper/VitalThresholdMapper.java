package healthmonitor.vitals.mapper;

import healthmonitor.vitals.dto.VitalThresholdDto;
import healthmonitor.vitals.model.VitalThreshold;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VitalThresholdMapper {
    VitalThreshold toEntity(VitalThresholdDto request);
    VitalThresholdDto toDto(VitalThreshold vitalThreshold);
}
