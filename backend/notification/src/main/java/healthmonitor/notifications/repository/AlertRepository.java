package healthmonitor.notifications.repository;

import healthmonitor.notifications.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {

    List<Alert> findByPatientIdAndIsReadFalseOrderByTimestampDesc(String patientId);
    List<Alert> findByPatientId(String patientId);
}
