package healthmonitor.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisteredEvent {
    private String keycloakUserId;
    private String email;
    private String firstName;
    private String lastName;
}
