package healthmonitor.auth.model.dto;

import lombok.Data;

@Data
public class LogoutRequestDto {
    private String refreshToken;
}
