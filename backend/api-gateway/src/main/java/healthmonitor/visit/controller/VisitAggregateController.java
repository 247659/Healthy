package healthmonitor.visit.controller;

import healthmonitor.payload.VisitResponse;
import healthmonitor.visit.service.VisitAggregateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gateway")
@RequiredArgsConstructor
public class VisitAggregateController {

    private final VisitAggregateService visitAggregateService;

    @GetMapping("/dashboard/visits/{visitId}")
    public Mono<VisitResponse> getVisit(@PathVariable UUID visitId) {
        return visitAggregateService.getVisitResponse(visitId);
    }
}
