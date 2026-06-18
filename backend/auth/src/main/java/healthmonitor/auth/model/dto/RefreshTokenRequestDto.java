package healthmonitor.auth.model.dto;

import lombok.Data;

@Data
public class RefreshTokenRequestDto {
    private String refreshToken;
}