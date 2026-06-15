package healthmonitor.service;

import healthmonitor.model.dto.LoginRequestDto;
import healthmonitor.model.dto.LogoutRequestDto;
import healthmonitor.model.dto.PatientRegistrationDto;
import healthmonitor.model.dto.RefreshTokenRequestDto;
import healthmonitor.model.dto.TokenResponseDto;

public interface AuthService {
    void registerPatient(PatientRegistrationDto patientDto);

    TokenResponseDto login(LoginRequestDto loginRequest);

    void logout(LogoutRequestDto logoutRequestDto);

    TokenResponseDto refresh(RefreshTokenRequestDto refreshTokenRequestDto);
}
