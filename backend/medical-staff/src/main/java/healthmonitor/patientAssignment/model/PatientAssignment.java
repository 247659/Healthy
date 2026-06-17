package healthmonitor.patientAssignment.model;

import healthmonitor.medicalStaff.model.MedicalStaff;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "patient_assignments")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PatientAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_staff_id", nullable = false)
    private MedicalStaff medicalStaff;

    @Column(nullable = false)
    private String patientId;
}
