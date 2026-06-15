package healthmonitor.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientDto {

    private String id;

    @NotBlank(message = "Email address cannot be empty")
    @Email(message = "Invalid email address format")
    private String email;

    @NotBlank(message = "First name cannot be empty")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name cannot be empty")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "PESEL number cannot be empty")
    @Pattern(regexp = "^\\d{11}$", message = "PESEL number must consist of exactly 11 digits")
    private String pesel;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be a date in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Phone number cannot be empty")
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "Address cannot be empty")
    private String address;
}
