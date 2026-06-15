package healthmonitor.model.dto;

import lombok.Data;

@Data
public class LogoutRequestDto {
    private String refreshToken;
}
