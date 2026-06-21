package healthmonitor.visitsservice;

import healthmonitor.visitsservice.visits.payload.request.VisitRequest;
import healthmonitor.visitsservice.visits.payload.response.VisitResponse;
import healthmonitor.visitsservice.visits.repository.VisitRepository;
import healthmonitor.visitsservice.visits.service.VisitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class VisitServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private VisitService visitService;

    @Autowired
    private VisitRepository visitRepository;

    @BeforeEach
    void setUp() {
        visitRepository.deleteAll();
    }

    @Test
    void shouldSaveVisit() {
        VisitRequest request = new VisitRequest(UUID.randomUUID(), "pat-1", LocalDateTime.now(), 30, "Note");
        VisitResponse saved = visitService.save(request);

        assertNotNull(saved.id());
        assertEquals("pat-1", saved.patientId());
    }

    @Test
    void shouldGetAllVisits() {
        visitService.save(new VisitRequest(UUID.randomUUID(), "p1", LocalDateTime.now(), 30, "n"));
        visitService.save(new VisitRequest(UUID.randomUUID(), "p2", LocalDateTime.now(), 30, "n"));

        assertEquals(2, visitService.getAll().size());
    }

    @Test
    void shouldGetVisitById() {
        VisitResponse saved = visitService.save(new VisitRequest(UUID.randomUUID(), "p1", LocalDateTime.now(), 30, "n"));

        VisitResponse found = visitService.getById(saved.id());
        assertEquals(saved.id(), found.id());
    }

    @Test
    void shouldUpdateVisit() {
        VisitResponse saved = visitService.save(new VisitRequest(UUID.randomUUID(), "p1", LocalDateTime.now(), 30, "Old"));
        VisitRequest updateReq = new VisitRequest(saved.medicalStaffId(), "p1", LocalDateTime.now(), 60, "New");

        VisitResponse updated = visitService.update(saved.id(), updateReq);
        assertEquals("New", updated.note());
        assertEquals(60, updated.durationMinutes());
    }

    @Test
    void shouldDeleteVisit() {
        VisitResponse saved = visitService.save(new VisitRequest(UUID.randomUUID(), "p1", LocalDateTime.now(), 30, "n"));
        visitService.delete(saved.id());

        assertThrows(ResponseStatusException.class, () -> visitService.getById(saved.id()));
    }
}