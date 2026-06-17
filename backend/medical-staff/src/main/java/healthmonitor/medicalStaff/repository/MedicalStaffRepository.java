package healthmonitor.medicalStaff.repository;

import healthmonitor.medicalStaff.model.MedicalStaff;
import jakarta.validation.constraints.NotNull;
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

    @EntityGraph(attributePaths = {"specializations"})
    Optional<MedicalStaff> findWithSpecializationById(UUID id);

    @EntityGraph(attributePaths = {"patientAssignments"})
    Optional<MedicalStaff> findWithPatientAssignmentsById(UUID id);
}
