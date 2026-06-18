package healthmonitor.visitsservice.visits.repository;

import healthmonitor.visitsservice.visits.model.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VisitRepository extends JpaRepository<Visit, UUID> {
}
