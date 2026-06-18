package healthmonitor.visitsservice.visits.controller;

import healthmonitor.visitsservice.visits.service.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/visits")
@CrossOrigin(origins = "*")
public class VisitController {
    private final VisitService visitService;
}
