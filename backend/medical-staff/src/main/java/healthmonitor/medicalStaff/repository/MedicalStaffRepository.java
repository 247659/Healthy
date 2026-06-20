package healthmonitor.medicalStaff.repository;

import healthmonitor.medicalStaff.model.MedicalStaff;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalStaffRepository extends JpaRepository<MedicalStaff, String> {
    @Override
    @EntityGraph(attributePaths = {"specializations"})
    List<MedicalStaff> findAll();

    @EntityGraph(attributePaths = {"specializations"})
    Optional<MedicalStaff> findWithSpecializationById(String id);

    @EntityGraph(attributePaths = {"patientAssignments"})
    Optional<MedicalStaff> findWithPatientAssignmentsById(String id);

    @Query("SELECT m.id FROM MedicalStaff m JOIN m.patientAssignments pa WHERE pa.patientId = :patientId")
    List<String> findIdsByPatientId(@Param("patientId") String patientId);

    List<MedicalStaff> findByPatientAssignments_PatientId(String patientId);
}
