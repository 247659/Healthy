package healthmonitor.visitsservice.visits.service;

import healthmonitor.visitsservice.visits.mapper.VisitMapper;
import healthmonitor.visitsservice.visits.model.Visit;
import healthmonitor.visitsservice.visits.payload.request.VisitRequest;
import healthmonitor.visitsservice.visits.payload.response.VisitResponse;
import healthmonitor.visitsservice.visits.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VisitServiceImpl implements VisitService {

    private final VisitRepository visitRepository;
    private final VisitMapper visitMapper;

    @Override
    public List<VisitResponse> getAll() {
        return visitRepository.findAll().stream()
                .map(visitMapper::toResponse)
                .toList();
    }

    @Override
    public VisitResponse getById(UUID id) {
        Visit visit = getEntity(id);
        return visitMapper.toResponse(visit);
    }

    @Override
    @Transactional
    public VisitResponse save(VisitRequest request) {
        Visit visit = visitMapper.toEntity(request);
        Visit savedVisit = visitRepository.save(visit);
        return visitMapper.toResponse(savedVisit);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Visit visit = getEntity(id);
        visitRepository.delete(visit);
    }

    @Override
    @Transactional
    public VisitResponse update(UUID id, VisitRequest request) {
        Visit visit = getEntity(id);
        visitMapper.updateEntity(visit, request);
        return visitMapper.toResponse(visit);
    }

    private Visit getEntity(UUID id) {
        return visitRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visit not found"));
    }
}
