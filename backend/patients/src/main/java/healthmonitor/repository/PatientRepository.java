package healthmonitor.repository;

import healthmonitor.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {

    boolean existsByEmail(String email);
    boolean existsByPesel(String pesel);
    Optional<Patient> findByEmail(String email);
}
