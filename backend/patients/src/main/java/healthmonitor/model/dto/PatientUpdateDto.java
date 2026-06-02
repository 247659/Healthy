package healthmonitor.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientUpdateDto {

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String name;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String surname;

    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    private String address;

    @Past(message = "Date of birth must be a date in the past")
    private LocalDate dateOfBirth;
}
