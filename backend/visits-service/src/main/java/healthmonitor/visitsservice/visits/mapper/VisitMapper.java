package healthmonitor.visitsservice.visits.mapper;

import healthmonitor.visitsservice.visits.model.Visit;
import healthmonitor.visitsservice.visits.payload.request.VisitRequest;
import healthmonitor.visitsservice.visits.payload.response.VisitResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VisitMapper {
    VisitResponse toResponse(Visit visit);

    Visit toEntity(VisitRequest request);

    void updateEntity(@MappingTarget Visit visit, VisitRequest request);
}
