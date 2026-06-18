package healthmonitor.visitsservice.visits.service;

import healthmonitor.visitsservice.visits.payload.request.VisitRequest;
import healthmonitor.visitsservice.visits.payload.response.VisitResponse;

import java.util.List;
import java.util.UUID;

public interface VisitService {
    List<VisitResponse> getAll();

    VisitResponse getById(UUID id);

    VisitResponse save(VisitRequest request);

    void delete(UUID id);

    VisitResponse update(UUID id, VisitRequest request);
}
