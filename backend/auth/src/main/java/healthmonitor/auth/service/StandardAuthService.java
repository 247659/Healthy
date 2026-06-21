package healthmonitor.auth.service;

import healthmonitor.auth.model.dto.LoginRequestDto;
import healthmonitor.auth.model.dto.LogoutRequestDto;
import healthmonitor.auth.model.dto.PatientRegistrationDto;
import healthmonitor.auth.model.dto.RefreshTokenRequestDto;
import healthmonitor.auth.model.dto.TokenResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StandardAuthService implements AuthService {

    private final KeycloakUserService keycloakUserService;
    static private final String PATIENT_ROLE = "patient";

    @Override
    public void registerPatient(PatientRegistrationDto patientDto) {
        keycloakUserService.createUserInKeycloak(patientDto.getEmail(), patientDto.getPassword(), patientDto.getFirstName(), patientDto.getLastName(), PATIENT_ROLE);
    }

    @Override
    public TokenResponseDto login(LoginRequestDto loginRequest) {
        return keycloakUserService.loginPatient(loginRequest.getEmail(), loginRequest.getPassword());
    }

    @Override
    public TokenResponseDto loginDoctor(LoginRequestDto loginRequest) {
        return keycloakUserService.loginDoctor(loginRequest.getEmail(), loginRequest.getPassword());
    }

    @Override
    public void logout(LogoutRequestDto logoutRequestDto) {
        keycloakUserService.logout(logoutRequestDto.getRefreshToken());
    }
    
    @Override
    public TokenResponseDto refresh(RefreshTokenRequestDto refreshTokenRequestDto) {
        return keycloakUserService.refreshAccessToken(refreshTokenRequestDto.getRefreshToken());
    }

    @Override
    public void changePassword(String userId, String newPassword) {
        keycloakUserService.changeUserPassword(userId, newPassword);
    }
}
