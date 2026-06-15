package healthmonitor.service;

import healthmonitor.model.dto.LoginRequestDto;
import healthmonitor.model.dto.LogoutRequestDto;
import healthmonitor.model.dto.PatientRegistrationDto;
import healthmonitor.model.dto.RefreshTokenRequestDto;
import healthmonitor.model.dto.TokenResponseDto;
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
        return keycloakUserService.login(loginRequest.getEmail(), loginRequest.getPassword());
    }

    @Override
    public void logout(LogoutRequestDto logoutRequestDto) {
        keycloakUserService.logout(logoutRequestDto.getRefreshToken());
    }
    
    @Override
    public TokenResponseDto refresh(RefreshTokenRequestDto refreshTokenRequestDto) {
        return keycloakUserService.refreshAccessToken(refreshTokenRequestDto.getRefreshToken());
    }
}
