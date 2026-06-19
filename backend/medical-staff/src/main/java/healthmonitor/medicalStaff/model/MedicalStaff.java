package healthmonitor.medicalStaff.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "medical_staff")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MedicalStaff {
    @Id
    private String id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true)
    private String phoneNumber;

    @Column(unique = true)
    private String licenseNumber;

    @ElementCollection
    @CollectionTable(
            name = "staff_specializations",
            joinColumns = @JoinColumn(name="medical_staff_id")
    )
    @OrderColumn(name = "index_id")
    private List<Specialization> specializations = new ArrayList<>();

    @OneToMany(mappedBy = "medicalStaff", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatientAssignment> patientAssignments = new ArrayList<>();

    public void addPatientAssignment(PatientAssignment assignment) {
        patientAssignments.add(assignment);
        assignment.setMedicalStaff(this);
    }

    public void removePatientAssignment(PatientAssignment assignment) {
        patientAssignments.remove(assignment);
        assignment.setMedicalStaff(null);
    }
}
