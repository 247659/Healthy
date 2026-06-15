package healthmonitor.model;

import lombok.Data;

@Data
public class LogoutRequestDto {
    private String refreshToken;
}
