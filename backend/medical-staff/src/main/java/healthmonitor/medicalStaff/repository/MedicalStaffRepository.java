package healthmonitor.medicalStaff.repository;

import healthmonitor.medicalStaff.model.MedicalStaff;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalStaffRepository extends JpaRepository<MedicalStaff, UUID> {
    @Override
    @EntityGraph(attributePaths = {"specializations"})
    List<MedicalStaff> findAll();

    @Override
    @EntityGraph(attributePaths = {"specializations"})
    Optional<MedicalStaff> findById(UUID id);
}
