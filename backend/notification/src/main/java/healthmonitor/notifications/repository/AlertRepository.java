package healthmonitor.notifications.repository;

import healthmonitor.notifications.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {

    List<Alert> findByPatientIdAndDestinationAndIsReadFalseOrderByTimestampDesc(String patientId, String destination);
    Page<Alert> findByPatientIdAndDestination(String patientId, String destination, Pageable pageable);
    @Query("SELECT a FROM Alert a WHERE a.alertId IN :alertIds AND a.destination = :destination")
    List<Alert> findAllByAlertIdAndDestination(@Param("alertIds") List<String> alertIds, @Param("destination") String destination);
}
