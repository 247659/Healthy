package healthmonitor.medicalStaff.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalDate;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Specialization {
    @Column(nullable = false)
    private String name;

    @Column(name = "obtained_date")
    private LocalDate obtainedDate;

    @Column(name = "certificate_number")
    private String certificateNumber;
}
