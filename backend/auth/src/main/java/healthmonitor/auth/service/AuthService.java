package healthmonitor.auth.service;

import healthmonitor.auth.model.dto.LoginRequestDto;
import healthmonitor.auth.model.dto.LogoutRequestDto;
import healthmonitor.auth.model.dto.PatientRegistrationDto;
import healthmonitor.auth.model.dto.RefreshTokenRequestDto;
import healthmonitor.auth.model.dto.TokenResponseDto;

public interface AuthService {
    void registerPatient(PatientRegistrationDto patientDto);

    TokenResponseDto login(LoginRequestDto loginRequest);

    TokenResponseDto loginDoctor(LoginRequestDto loginRequest);

    void logout(LogoutRequestDto logoutRequestDto);

    TokenResponseDto refresh(RefreshTokenRequestDto refreshTokenRequestDto);

    void changePassword(String userId, String newPassword);
}
