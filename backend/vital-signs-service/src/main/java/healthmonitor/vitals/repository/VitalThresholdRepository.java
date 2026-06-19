package healthmonitor.vitals.repository;

import healthmonitor.vitals.model.VitalThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VitalThresholdRepository extends JpaRepository<VitalThreshold, String> {
}
