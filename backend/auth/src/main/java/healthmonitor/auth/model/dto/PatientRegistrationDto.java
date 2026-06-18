package healthmonitor.auth.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PatientRegistrationDto {

    @NotBlank(message = "Email address cannot be empty")
    @Email(message = "Invalid email address format")
    private String email;

    @NotBlank(message = "First name cannot be empty")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(
            regexp = "^[A-Za-zÀ-ÿĄąĆćĘęŁłŃńÓóŚśŹźŻż\\- ]+$",
            message = "First name can contain only letters, spaces and hyphens"
    )
    private String firstName;

    @NotBlank(message = "Last name cannot be empty")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(
            regexp = "^[A-Za-zÀ-ÿĄąĆćĘęŁłŃńÓóŚśŹźŻż\\- ]+$",
            message = "Last name can contain only letters, spaces and hyphens"
    )
    private String lastName;
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._#\\-])[A-Za-z\\d@$!%*?&._#\\-]{8,64}$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit and one special character"
    )
    private String password;
}
